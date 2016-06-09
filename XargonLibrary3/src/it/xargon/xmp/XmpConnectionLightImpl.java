package it.xargon.xmp;

import java.io.*;
import java.net.*;
import java.util.Arrays;

import it.xargon.streams.*;
import it.xargon.streams.EncryptionModel.Default;
import it.xargon.util.*;
import it.xargon.events.*;

class XmpConnectionLightImpl implements EventsSourceImpl<XmpConnection.Events>, XmpConnection {   
   private Socket sock=null;
   private DuplexChannel chan=null;
   private XmpParser parser=null;
   private int scan=0;
   private XmpMessage answer=null;
   private XmpFactory fact=null;
   private IdGenerator idGenerator=new IdGenerator();
   private boolean askEncryption=false;
   private SecurityModel secModel=SecurityModel.BOTH;
   private EncryptionModel encModel=EncryptionModel.getDefaultModel(Default.MID_SEC);
   private Identity iident=null;
   
   public boolean isRequestingEncryption() {return askEncryption;}
   public void setRequestingEncryption(boolean req) {checkState();askEncryption=req;}
   
   public SecurityModel getSecurityModel() {return secModel;}
   public void setSecurityModel(SecurityModel sec) {checkState();secModel=sec;}
   
   public EncryptionModel getEncryptionModel() {return encModel;}
   public void setEncryptionModel(EncryptionModel enc) {checkState();encModel=enc;}
   
   public Identity getIdentity() {return iident;}
   public void setIdentity(Identity ident) {iident=ident;}

   private void checkState() {if (connected) throw new IllegalStateException();}
   
   private boolean connected=false;
   
   public XmpConnectionLightImpl(Socket sck, XmpFactory factory) throws IOException {
      //Se è costruito tramite socket, gestiremo anche le richieste di chiusura
      this(sck.getInputStream(), sck.getOutputStream(), factory);
      sock=sck;
   }
   
   public XmpConnectionLightImpl(InputStream instream, OutputStream outstream, XmpFactory factory) {
      fact=factory;
      chan=new DuplexChannel(instream, outstream, null);
      parser=new XmpParser();
   }
   
   public XmpFactory getFactory() {return fact;}
      
   public synchronized void start() throws IOException {
      connected=true;
      //Emettere la firma e la versione: il send è sempre possibile
      chan.getOutputStream().write(SIGNATURE);
      chan.getOutputStream().write(askEncryption?1:0);
      
      //Leggere i 5 byte di firma e versione
      byte[] sig=new byte[SIGNATURE.length];
      chan.getInputStream().read(sig);      
      //Confronto, e troncamento nel caso in cui la firma non corrisponda
      if (!Arrays.equals(SIGNATURE, sig)) {
         chan.close();
         throw new IOException("Firma e versione del protocollo non validi");
      }
      
      //Leggere se è richiesta crittazione
      boolean encryptRequested=(chan.getInputStream().read()==1);
      boolean encryptConfirmed=false;
      
      //Confrontare con il modello di sicurezza attuale
      switch (secModel) {
         case PLAIN_ONLY:
            if (encryptRequested) {
               chan.close();
               throw new IOException("Non sono permesse connessioni crittate");
            } else {
               encryptConfirmed=false;
            }
            break;
         case CRYPTED_ONLY:
            if (encryptRequested) {
               encryptConfirmed=true;
            } else {
               chan.close();
               throw new IOException("Non sono permesse connessioni in chiaro");
            }
            break;
         case BOTH:
            encryptConfirmed=(encryptRequested | askEncryption);
            break;
      }
      
      if (encryptConfirmed) {
         chan.setIdentity(iident);
         chan=Encryptor.secureChannel(chan, encModel);
         iident=chan.getIdentity();
      }
   }
   
   public synchronized void close() throws IOException {
      chan.close();
      if (sock!=null) sock.close();
      connected=false;
   }

   public String toString() {
      if (sock!=null) return "LXmpLightConnectionImpl: " + sock.toString();
      return "LXmpLightConnectionImpl: raw streams"; 
   }

   public boolean isConnected() {return connected;}
   public boolean isRunning() {return connected;}

   private synchronized void sendMessage(XmpMessageImpl message) throws IOException {
      Identifier msgid=idGenerator.next();
      message.setMessageId(msgid);
      message.marshal(sock.getOutputStream());
   }

   public void sendEvent(byte[] outgoing) throws IOException {
      sendMessage(new XmpMessageImpl(XmpMessageType.EVENT, outgoing));
   }

   public byte[] sendRequest(byte[] outgoing) throws IOException {return sendRequest(outgoing, 0);}

   public byte[] sendRequest(byte[] outgoing, long timeout) throws IOException {
      answer=null;
      
      //Il timeout deve essere ignorato, non abbiamo il controllo pieno sull'implementazione
      //sottostante all'inputstream
      if (!connected) return null;
      
      //Inviare il messaggio (serializza e assegna ID)
      XmpMessageImpl xout=new XmpMessageImpl(XmpMessageType.REQUEST, outgoing);
      sendMessage(xout);

      //Inizia il loop di lettura
      do {
         scan=chan.getInputStream().read();
         if (scan!=-1) {
            //Inserire ogni byte nel parser, ignoriamo gli errori di parsificazione
            XmpMessage incomingMsg=null;
            try {incomingMsg=parser.feed(Bitwise.asByte(scan));}
            catch (XmpParserException ignore) {}
            
            //Se finalmente è presente almeno un messaggio...
            if (incomingMsg!=null) {
               //controlliamo che sia il messaggio di risposta che aspettavamo
               if ((incomingMsg.getType()==XmpMessageType.ANSWER) && (incomingMsg.isChildOf(xout))) answer=incomingMsg;
               else {
                  //non lo è, ma facciamo un minimo di elaborazione
                  if (incomingMsg.getType()==XmpMessageType.REQUEST) {
                     //Se è arrivata una richiesta, inviamo una risposta finta
                     //almeno per sbloccare il richiedente
                     XmpMessageImpl fakeanswer=((XmpMessageImpl)incomingMsg).createAnswer();
                     sendMessage(fakeanswer);
                  }
                  //in tutti gli altri casi ignoriamo il messaggio
               }
            }
         }
      } while ((answer==null) && (scan!=-1));
      //Continua finchè non si chiude lo stream per qualche motivo,
      //oppure finchè non arriva la risposta cercata.
      
      return answer.getContents();
   }

   public void sendRequest(byte[] outgoing, AsyncAnswer receiver) throws IOException {
      receiver.receive(sendRequest(outgoing));
   }
   
   public void sendRequest(byte[] outgoing, AsyncAnswer receiver, long timeout) throws IOException {
      receiver.receive(sendRequest(outgoing, timeout));      
   }

   public Events[] getAllEventSinks() {throw new UnsupportedOperationException();}

   public boolean isRegistered(Events sink) {throw new UnsupportedOperationException();}

   public boolean register(Events sink) {throw new UnsupportedOperationException();}

   public boolean unregister(Events sink) {throw new UnsupportedOperationException();}

   public void unregisterAll() {throw new UnsupportedOperationException();}

   public void registerProbe(XmpMessageProbe.Events sink) {
      throw new UnsupportedOperationException();
   }

   public void unregisterProbe(XmpMessageProbe.Events sink) {
      throw new UnsupportedOperationException();
   }

   public EventsTrap createTrap() {throw new UnsupportedOperationException();}

   public boolean unregister(EventsTrap trap) {throw new UnsupportedOperationException();}

   public boolean isRegistered(EventsTrap trap) {throw new UnsupportedOperationException();}
}
