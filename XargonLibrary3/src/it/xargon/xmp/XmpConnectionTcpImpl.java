package it.xargon.xmp;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import it.xargon.events.EventsSourceImpl;
import it.xargon.smartnet.*;
import it.xargon.streams.*;
import it.xargon.streams.EncryptionModel.Default;
import it.xargon.util.*;

class XmpConnectionTcpImpl extends EventsSourceImpl<XmpConnection.Events> implements XmpConnection {
   private XmpParser parser=null;
   private XmpFactory fact=null;
   private ExecutorService factoryThreadPool=null;
   private ExecutorService jobsThreadPool=null;
   
   private TcpConnection tcpconn=null;
      
   private IdGenerator idGenerator=new IdGenerator();
   private HashMap<Identifier, XmpMessageImpl> pendingRequests=null;
   private HashMap<Identifier, XmpMessageImpl> receivedAnswers=null;
   private HashSet<Holder<Future<?>>> currentJobs=null;

   private boolean askEncryption=false;
   private SecurityModel secModel=SecurityModel.BOTH;
   private EncryptionModel encModel=EncryptionModel.getDefaultModel(Default.MID_SEC);
   private Identity iident=null;
   
   private XmpMessageProbeImpl probe=null;
   
   public boolean isRequestingEncryption() {return askEncryption;}
   public void setRequestingEncryption(boolean req) {checkState();askEncryption=req;}
   
   public SecurityModel getSecurityModel() {return secModel;}
   public void setSecurityModel(SecurityModel sec) {checkState();secModel=sec;}
   
   public EncryptionModel getEncryptionModel() {return encModel;}
   public void setEncryptionModel(EncryptionModel enc) {checkState();encModel=enc;}
   
   public Identity getIdentity() {return iident;}
   public void setIdentity(Identity ident) {iident=ident;}
   
   private void checkState() {if (tcpconn.isRunning()) throw new IllegalStateException();}

   private TcpConnection.Events tcpconneventsink=new TcpConnection.Events.Adapter() {
      public void connected(TcpConnection conn) {te_connected();}
      public void dataArrived(TcpConnection conn, byte[] data) {te_dataArrived(data);}
      public void disconnected(TcpConnection conn) {te_disconnected();}      
   };
   
   private void te_connected() {raiseEvent.connected(this);}

   private void dispatchRequest(final XmpMessageImpl ixmpincoming) {
      final Holder<Future<?>> jobHolder=new Holder<Future<?>>();
      
      Runnable rtask=new Runnable() {public void run() {
         if (probe!=null) probe.p_preProcessMessage(XmpConnectionTcpImpl.this, ixmpincoming);
         
         XmpMessageImpl ixmpanswer=ixmpincoming.createAnswer();

         byte[] answerContents=null;
         //Tutta l'elaborazione avviene in questo metodo
         answerContents=raiseEvent.processRequest(XmpConnectionTcpImpl.this, ixmpincoming.getContents());
         ixmpanswer.setContents(answerContents);
         
         if (Thread.currentThread().isInterrupted()) return;
         if (tcpconn.isConnected()) {
            try {sendMessage(ixmpanswer);}
            catch (IOException ex) {raiseEvent.exception(XmpConnectionTcpImpl.this, new XmpException(ex));}
            if (probe!=null) probe.p_postProcessMessage(XmpConnectionTcpImpl.this, ixmpincoming, ixmpanswer);
         }
         
         synchronized (currentJobs) {currentJobs.remove(jobHolder);}
      }};
      
      synchronized (currentJobs) {            
         if (jobsThreadPool==null) {
            FutureTask<Object> ftask=new FutureTask<Object>(rtask, null);
            jobHolder.set(ftask);
            new Thread(ftask).start();
         } else {
            jobHolder.set(jobsThreadPool.submit(rtask));
         }
         
         currentJobs.add(jobHolder);
      }
   }
      
   private void dispatchEvent(final XmpMessageImpl ixmpincoming) {
      final Holder<Future<?>> jobHolder=new Holder<Future<?>>();
      
      Runnable rtask=new Runnable() {public void run() {
         if (probe!=null) probe.p_preProcessMessage(XmpConnectionTcpImpl.this, ixmpincoming);
         raiseEvent.processEvent(XmpConnectionTcpImpl.this, ixmpincoming.getContents());
         if (probe!=null) probe.p_postProcessMessage(XmpConnectionTcpImpl.this, ixmpincoming, null);            
         synchronized (currentJobs) {currentJobs.remove(jobHolder);}
      }};
      
      synchronized (currentJobs) {
         if (jobsThreadPool==null) {
            FutureTask<Object> ftask=new FutureTask<Object>(rtask, null);
            jobHolder.set(ftask);
            new Thread(ftask).start();
         } else {
            jobHolder.set(jobsThreadPool.submit(rtask));
         }
         
         currentJobs.add(jobHolder);
      }
   }
   
   private void dispatchDiscarded(final XmpMessageImpl ixmpincoming) {
      final Holder<Future<?>> jobHolder=new Holder<Future<?>>();
      
      Runnable rtask=new Runnable() {public void run() {
         if (probe!=null) probe.p_discardedAnswer(XmpConnectionTcpImpl.this, ixmpincoming);         
         synchronized (currentJobs) {currentJobs.remove(jobHolder);}
      }};
      
      synchronized (currentJobs) {
         if (jobsThreadPool==null) {
            FutureTask<Object> ftask=new FutureTask<Object>(rtask, null);
            jobHolder.set(ftask);
            new Thread(ftask).start();
         } else {
            jobHolder.set(jobsThreadPool.submit(rtask));
         }
         
         currentJobs.add(jobHolder);
      }
   }
   
   private void te_dataArrived(byte[] data) {
      //Vi sono dati in arrivo sul socket
      //Carichiamo un byte alla volta sul parser (catturando eventuali eccezioni).
      //Nel momento in cui i dati sono sufficienti a generare un messaggio, lo elaboriamo
      
      for(byte b:data) {
         XmpMessageImpl msg=null;
         try {msg=parser.feed(b);}
         catch (XmpException ex) {raiseEvent.exception(this, ex);}
         if (msg!=null) dispatchXmpMessage(msg);
      }
   }
      
   private void dispatchXmpMessage(XmpMessageImpl xmpincoming) {
      switch (xmpincoming.getType()) {
         case ANSWER:
            XmpMessageImpl awaiter=pendingRequests.get(xmpincoming.getParentId());
            if (awaiter==null) dispatchDiscarded(xmpincoming);
            else {
               receivedAnswers.put(xmpincoming.getParentId(), xmpincoming);
               synchronized (awaiter) {awaiter.notify();} //sveglia il thread in attesa
            }
            break;
         case REQUEST:
            dispatchRequest(xmpincoming);
            break;
         case EVENT:
            dispatchEvent(xmpincoming);
            break;
      }      
   }
   
   private void te_disconnected() {
      //Il socket TCP è chiuso, non ci interessa più nessun evento
      tcpconn.forceClose();
      tcpconn.unregister(tcpconneventsink);      
      
      //Ne approfittiamo per fare un po' di pulizie      
      
      //Aspettiamo che tutti i messaggi XMP di richiesta/evento finiscano
      //di essere elaborate. Non essendoci più nessuna connessione, i messaggi answer
      //generati dalle corrispondenti request non possono più essere inviati.
      
      HashSet<Holder<Future<?>>> currentJobsCopy=null;      
      synchronized (currentJobs) {currentJobsCopy=new HashSet<Holder<Future<?>>>(currentJobs);}      
      for(Holder<Future<?>> h:currentJobsCopy) {
         try {
            h.get().get();
         } catch (InterruptedException ex) {
         } catch (ExecutionException ex) {
            //Se un processore di messaggi XMP lancia un'eccezione, questa deve
            //essere riportata all'esterno (almeno per essere loggata)
            raiseEvent.exception(this, new XmpException(ex));
         }
      }

      //Tutti i thread nel pool dedicato all'elaborazione dei job asincroni
      //(elaborazione di eventi e richieste in arrivo) vengono fermati
      if (jobsThreadPool!=factoryThreadPool) jobsThreadPool.shutdown();

      //Tutti i thread ancora in attesa di risposta vengono svegliati
      //per restituire null al chiamante. Autonomamente i thread in attesa
      //elimineranno le risposte "finte"
      Set<Identifier> pendingRequestsCopy=new HashSet<Identifier>(pendingRequests.keySet());
      for(Identifier reqid:pendingRequestsCopy) receivedAnswers.put(reqid, null);
      pendingRequests.clear();

      //Tutti i listener vengono avvisati della disconnessione completa
      raiseEvent.disconnected(this);
   }      
   
   public XmpConnectionTcpImpl(XmpFactory factory, TcpConnection conn, int maxParallelJobs) {
      super(factory.getThreadPool());
      fact=factory;
      factoryThreadPool=fact.getThreadPool();
      if (maxParallelJobs==0) jobsThreadPool=factoryThreadPool;
      else jobsThreadPool=Executors.newFixedThreadPool(maxParallelJobs);
      tcpconn=conn;
      parser=new XmpParser();
      pendingRequests=new HashMap<Identifier, XmpMessageImpl>();
      receivedAnswers=new HashMap<Identifier, XmpMessageImpl>();
      currentJobs=new HashSet<Holder<Future<?>>>();
   }
   
   public XmpFactory getFactory() {return fact;}
   
   public void start() throws IOException {
      if (tcpconn.isRunning()) return;
      
      tcpconn.register(tcpconneventsink);
      DuplexChannel tcpChannel=tcpconn.startSuspended();
      //dialogo sulla "firma" della connessione e sul modello di sicurezza da adottare.
      
      //Altri listener registrati su questa XMPConnection possono prendere l'evento
      //connected come autorizzazione a "sparare" messaggi. Questi messaggi però
      //rimarrannno accodati finchè l'inputstream non verrà rilasciato.
      
      //Emettere la firma e la versione: il send è sempre possibile
      tcpChannel.getOutputStream().write(SIGNATURE);
      tcpChannel.getOutputStream().write(askEncryption?1:0);
      
      //Leggere i 5 byte di firma e versione
      byte[] sig=new byte[SIGNATURE.length];
      tcpChannel.getInputStream().read(sig);      
      //Confronto, e troncamento nel caso in cui la firma non corrisponda
      if (!Arrays.equals(SIGNATURE, sig)) {
         tcpconn.setDuplexChannel(tcpChannel);
         tcpChannel.close();
         throw new IOException("Firma e versione del protocollo non validi");
      }
      
      //Leggere se è richiesta crittazione
      boolean encryptRequested=(tcpChannel.getInputStream().read()==1);
      boolean encryptConfirmed=false;
      
      //Confrontare con il modello di sicurezza attuale
      switch (secModel) {
         case PLAIN_ONLY:
            if (encryptRequested) {
               tcpconn.setDuplexChannel(tcpChannel);
               tcpChannel.close();
               throw new IOException("Non sono permesse connessioni crittate");
            } else {
               encryptConfirmed=false;
            }
            break;
         case CRYPTED_ONLY:
            if (encryptRequested) {
               encryptConfirmed=true;
            } else {
               tcpconn.setDuplexChannel(tcpChannel);
               tcpChannel.close();
               throw new IOException("Non sono permesse connessioni in chiaro");
            }
            break;
         case BOTH:
            encryptConfirmed=(encryptRequested | askEncryption);
            break;
      }
      
      if (encryptConfirmed) {
         tcpChannel.setIdentity(iident);
         tcpChannel=Encryptor.secureChannel(tcpChannel, encModel);
         iident=tcpChannel.getIdentity();
      }
      
      tcpconn.setDuplexChannel(tcpChannel);
   }
   
   public void close() throws IOException {
      if (!tcpconn.isRunning()) return;
      tcpconn.close();
   }
   
   public String toString() {return "BXmpConnectionImpl: " + tcpconn.toString();}
      
   public boolean isConnected() {return tcpconn.isConnected();}
   public boolean isRunning() {return tcpconn.isRunning();}
   
   public void sendEvent(byte[] outgoing) throws IOException {
      XmpMessageImpl outmsg=new XmpMessageImpl(XmpMessageType.EVENT, outgoing);
      sendMessage(outmsg);
      if (probe!=null) probe.p_eventSent(this, outmsg);
   }
      
   public byte[] sendRequest(byte[] outgoing) throws IOException {
      return sendRequest(outgoing, 0);
   }
      
   public byte[] sendRequest(byte[] outgoing, long timeout) throws IOException {
      if (!tcpconn.isRunning()) return null;
      XmpMessageImpl outgoingMessage=new XmpMessageImpl(XmpMessageType.REQUEST, outgoing);
      sendMessage(outgoingMessage);
      if (probe!=null) probe.p_requestSent(this, outgoingMessage);
      return awaitAnswer(outgoingMessage, timeout);
   }

   private synchronized void sendMessage(XmpMessageImpl message) throws IOException {
      if ((!isConnected()) || (!isRunning())) throw new IOException("Connessione non attiva");
      Identifier msgid=idGenerator.next();
      message.setMessageId(msgid);
      if (message.getType()==XmpMessageType.REQUEST) pendingRequests.put(msgid, message);
      message.marshal(tcpconn.getOutputStream());
   }

   private boolean timedOut(XmpMessageImpl outgone, long timeout) {
      if (timeout==0) return false;
      return (System.currentTimeMillis()-outgone.getTimestamp()) > timeout;
   }
   
   private byte[] awaitAnswer(XmpMessageImpl outgone, long timeout) {
      while (!(receivedAnswers.containsKey(outgone.getMessageId())) && (!timedOut(outgone, timeout))) {
         synchronized (outgone) {
            try {outgone.wait(10);} catch (InterruptedException ex) {}
         }
      }
      
      XmpMessageImpl answer=receivedAnswers.remove(outgone.getMessageId());
      pendingRequests.remove(outgone.getMessageId());
      if (probe!=null) probe.p_answerReceived(this, answer);
      if (answer==null) return null;
      return answer.getContents();
   }
   
   public void sendRequest(byte[] outgoing, AsyncAnswer receiver) throws IOException {
      sendRequest(outgoing, receiver, 0);
   }
   
   public void sendRequest(byte[] outgoing, final AsyncAnswer receiver, final long timeout) throws IOException {
      if (!tcpconn.isRunning()) {
         receiver.receive(null);
         return;
      }
      final XmpMessageImpl outgoingMessage=new XmpMessageImpl(XmpMessageType.REQUEST, outgoing);
      sendMessage(outgoingMessage);
      if (probe!=null) probe.p_requestSent(this, outgoingMessage);

      Runnable rtask=new Runnable() {
         public void run() {
            byte[] answer=awaitAnswer(outgoingMessage, timeout);
            receiver.receive(answer);
         }
      };
      
      if (factoryThreadPool==null) {
         new Thread(rtask).start();
      } else {
         factoryThreadPool.submit(rtask);     
      }
   }
   
   public synchronized void registerProbe(XmpMessageProbe.Events sink) {
      if (probe==null) probe=new XmpMessageProbeImpl(factoryThreadPool);
      probe.register(sink);
   }

   public synchronized void unregisterProbe(XmpMessageProbe.Events sink) {
      if (probe==null) return;
      probe.unregister(sink);
      if (probe.getAllEventSinks().length==0) probe=null;
   }
}
