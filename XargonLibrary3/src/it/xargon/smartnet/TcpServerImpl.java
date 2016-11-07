package it.xargon.smartnet;

import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;

import it.xargon.events.*;
import it.xargon.util.*;

class TcpServerImpl extends EventsSourceImpl implements TcpServer {
   private SmartnetFactory ifactory=null;
   private ExecutorService ithreadPool=null;
   private Future<?> serverFutureTask=null;
   private BooleanLatch startlock=new BooleanLatch();
   private BooleanLatch stoplock=new BooleanLatch();
   private Thread serverThread=null;
   
   private InetSocketAddress iEndPoint=null;
   private ServerSocket iServ=null;
   private boolean running;
   private boolean killconnections;
   private boolean timeToClose;

   private HashSet<TcpConnectionImpl> connections=null;
   private Object lock=null;

   public TcpServerImpl(SmartnetFactory factory, InetSocketAddress endPoint) {
      super();
      ifactory=factory;
      ithreadPool=factory.getThreadPool();
      iEndPoint=endPoint;
      lock=new Object();
      connections=new HashSet<TcpConnectionImpl>();
      killconnections=false;
      timeToClose=false;
   }
   
   @Override
   protected ExecutorService getThreadPool() {return ithreadPool;}

   public synchronized void start() throws IOException {
      if (running) {throw new IOException("Already started");}
      iServ=new ServerSocket();
      iServ.bind(iEndPoint);
      iServ.setReuseAddress(true);
      iServ.setSoTimeout(50);
      if (ithreadPool==null) {
         FutureTask<Object> task=new FutureTask<Object>(new Runnable() {public void run() {internalRun();}}, null);
         serverFutureTask=task;
         new Thread(task).start();
      } else {
         serverFutureTask=ithreadPool.submit(new Runnable() {public void run() {internalRun();}});
      }
      try {startlock.await();} catch (InterruptedException e) {}
   }

   public synchronized void shutdown() throws IOException {
      if (!running) {throw new IOException("Already stopped");}
      if (timeToClose) return;
      killconnections=true;
      timeToClose=true;
      serverFutureTask.cancel(false);
      if (Thread.currentThread()==serverThread) stoplock.open();
      try {stoplock.await();} catch (InterruptedException e) {}
   }
   
   public synchronized Collection<TcpConnection> stop() throws IOException {
      if (!running) {throw new IOException("Already stopped");}
      if (timeToClose) return null;
      Collection<TcpConnection> result=getConnections();
      killconnections=false;
      timeToClose=true;
      serverFutureTask.cancel(false);
      if (Thread.currentThread()==serverThread) stoplock.open();
      try {stoplock.await();} catch (InterruptedException e) {}
      return result;
   }

   public boolean isRunning() {return running;}
 
   public void connectionCleanup(TcpConnectionImpl conn) {
      //Nel momento in cui la connessione TCP si è conclusa, e solo
      //DOPO aver avvisato tutti i listener sulla connessione, la connection chiamerà questa
      //procedura sul server per consentire la pulizia dei riferimenti, e per
      //avvisare tutti i listener sul server
      synchronized (lock) {connections.remove(conn);}
      raise(REMOVED).raise(this, conn);
   }

   private void internalRun() {
      serverThread=Thread.currentThread();
      running=true;
      Thread.currentThread().setName("TcpServer - " + iEndPoint.toString());
      raise(STARTED).raise(this);
      startlock.open();
      while (!timeToClose) {
         try {
            Socket csock=iServ.accept();

            TcpConnectionImpl iconn=new TcpConnectionImpl(ifactory, this, csock);
            
            //Lasciamo che il listener/i listener di questo server
            //inseriscano i propri listener anche sulla connessione.
            //A questo punto la responsabilità dell'avvio del thread
            //di gestione è nelle mani dei listener su questo server
            raise(BUILD).raise(this, iconn);
            
            //Controlliamo che la connection sia stata avviata correttamente
            //se così non fosse, chiudiamo il socket e solleviamo un evento
            //di errore
            
            if (!iconn.isRunning()) {
               csock.close();
               raise(SERVEREXCEPTION).raise(this, new SmartnetException("TcpConnection was not started before returning from the \"build\" event"));
            } else {
               synchronized (lock) {connections.add(iconn);}
            }
         } catch(SocketTimeoutException ex) {
            //Nulla da fare. Verifica timeToClose e riprova "accept"*/
         } catch(IOException ex) {
            //Errore durante l'accettazione: notifichiamo il listener
            raise(SERVEREXCEPTION).raise(this, ex);
            //Forziamo la chiusura del socket server
            try {iServ.close();} catch (IOException ex2) {}
         }
      }

      //Thread del server in fase di terminazione: chiudere tutte le connessioni
      
      //Il vero compito del server non sta più girando
      running=false;

      //Impediamo l'accettazione di ulteriori connessioni chiudendo il server socket
      try {
         iServ.close();
      } catch(IOException ex) {
         //Errore durante la chiusura: notifichiamo il listener
         raise(SERVEREXCEPTION).raise(this, ex);
      }

      //Se è stato richiesto un arresto completo...
      if (killconnections) {
         //...cicliamo su tutte le connessioni aperte e chiudiamole una per una
         //Il metodo "stop" è bloccante finchè la connessione non è chiusa
         //Per sicurezza creiamo una copia dell'elenco delle connessioni su cui
         //possiamo iterare
         HashSet<TcpConnection> connscopy=new HashSet<TcpConnection>(connections);
         for(TcpConnection iconn:connscopy) {
            try {
               if (iconn.isConnected()) iconn.close();
            } catch (IOException ex) {
               raise(SERVEREXCEPTION).raise(this, ex);
            }
         }
      }

      raise(STOPPED).raise(this);
      stoplock.open();
      serverThread=null;
   }
   
   public String toString() {
      if (iServ==null) return "TcpServerImpl: no socket bind";
      return "TcpServerImpl: " + iEndPoint.toString();      
   }
   
   public InetSocketAddress getListeningAddress() {
      if (iServ==null) return null;
      return iEndPoint;            
   }
   
   public Collection<TcpConnection> getConnections() {
      return Collections.unmodifiableCollection(new HashSet<TcpConnection>(connections));
   }

   public SmartnetFactory getFactory() {return ifactory;}
}
