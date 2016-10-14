package it.xargon.streams;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.*;

import it.xargon.events.*;
import it.xargon.util.*;

public class SmartInputStream extends EventsSourceImpl {
   @FunctionalInterface @Event
   public interface DataArrived {public void with(byte[] data, int length);}
   public final static Class<DataArrived> DATAARRIVED=DataArrived.class;
   
   @FunctionalInterface @Event
   public interface StreamException {public void with(StreamErrorReaction reaction);}
   public final static Class<StreamException> STREAMEXCEPTION=StreamException.class;

   @FunctionalInterface @Event
   public interface Started extends Runnable {}
   public final static Class<Started> STARTED=Started.class;
   
   @FunctionalInterface @Event
   public interface Stopped extends Runnable {}
   public final static Class<Stopped> STOPPED=Stopped.class;
   
   @FunctionalInterface @Event
   public interface Suspended extends Runnable {}
   public final static Class<Suspended> SUSPENDED=Suspended.class;
   
   @FunctionalInterface @Event
   public interface Restored extends Runnable {}
   public final static Class<Restored> RESTORED=Restored.class;
   
   private byte[] buffer=null;
   
   private ExecutorService ithreadPool=null;
   private Future<?> inputStreamFuture=null;
   
   private String prevThreadName=null;
   private Thread runner=null;
   
   private LessBlockingInputStream istream=null;

   private volatile boolean timeToClose=false;
   
   private boolean running=false;
   
   private volatile BooleanLatch startlock=null;
   private volatile BooleanLatch waitForFreeze=null;
   private volatile BooleanLatch waitForRelease=null;

   public SmartInputStream(InputStream in) {this(in,null);}
   public SmartInputStream(InputStream in, ExecutorService threadPool) {
      super();
      if (in==null) throw new IllegalArgumentException("InputStream expected");
      istream=new LessBlockingInputStream(in);
      ithreadPool=threadPool;
      running=false;
      buffer=new byte[1];
   }
   
   @Override
   protected ExecutorService getThreadPool() {return ithreadPool;}

   public synchronized void start() { //Impediamo che più di un thread possa chiamare start
      if (running) return;
      timeToClose=false;
      startlock=new BooleanLatch();
      if (ithreadPool==null) {
         FutureTask<Object> task = new FutureTask<Object>(new Runnable() {public void run() {inputStreamRun();}}, null);
         inputStreamFuture=task;
         new Thread(task).start();
      } else {
         inputStreamFuture = ithreadPool.submit(new Runnable() {public void run() {inputStreamRun();}});
      }
      //Blocca finchè il tread di gestione dello stream non sia completamente avviato
      try {startlock.await();} catch (InterruptedException ex) {}
      startlock=null;

      //Solo ora possiamo notificare dell'evento tutti i listener, prima di ritornare al chiamante
      raise(STARTED);
   }
   
   public synchronized InputStream startSuspended() {
      if ((waitForRelease!=null) || (running)) return null;
      waitForFreeze=new BooleanLatch();
      start();
      //Lancia "started" ma il thread di lettura si blocca prima
      //della prima lettura (notificando il blocco sul nostro "waitForFreeze")
      try {waitForFreeze.await();} catch (InterruptedException ex) {}
      //Possiamo catturare e restituire l'inputstream bloccato
      return istream.getInputStream();
   }
      
   public InputStream stop() {
      if (!running) return null;
      synchronized (this) {
         timeToClose=true;
         inputStreamFuture.cancel(false);      
         
         if (Thread.currentThread()!=runner) {
            //Blocca finchè il thread di gestione degli eventi dello stream non sia terminato,
            //tranne quando il thread che ha chiamato lo stop è lo stesso del gestore degli
            //eventi (rischio di race condition)
            try {inputStreamFuture.get();} catch (Exception ex) {}         
         }
         
         return istream.getInputStream();
      }
   }
   
   public boolean isRunning() {return running;}
   
   public InputStream suspendInputStream() {
      if (waitForRelease!=null) return null;
      waitForFreeze=new BooleanLatch();

      //Se il thread attuale è il lettore dello stream, evidentemente
      //è necessario reagire ad una richiesta remota con un'iterazione
      //particolare con lo stream stesso: nessuna attesa (altrimenti blocco garantito)
      if (Thread.currentThread()!=runner) try {waitForFreeze.await();} catch (InterruptedException ex) {}
      //Il thread di lettura dell'InputStream è sospeso, possiamo prendere
      //l'InputStream e fornirlo al chiamante (che può benissimo decidere
      //di chiuderlo). Viene restituito lo stream originale per
      //permettere al chiamante eventualmente di sostituire lo stream
      //vedi: implementazione di stream criptati

      return istream.getInputStream();
   }
   
   public void restoreInputStream(InputStream is) {
      if (waitForRelease==null) return;
      if (is!=null) istream.setinputStream(is);
      startlock=new BooleanLatch();
      waitForRelease.open();
      try {startlock.await();} catch (InterruptedException ignored) {}
      startlock=null;
   }
      
   private void inputStreamRun() {
      runner=Thread.currentThread();
      prevThreadName=runner.getName();
      runner.setName("SmartInputStream - " + istream.getClass().getName());
      int takenbytes=0;
      if (startlock!=null) startlock.open();
      running=true;
      do {
         if ((waitForFreeze!=null) && (waitForRelease==null)) {
            running=false;
            waitForRelease=new BooleanLatch();
            raise(SUSPENDED); //Avviamo chi è interessato che la consegna degli eventi è sospesa
            waitForFreeze.open();
            //permette al chiamante di suspendInputStream di ottenere l'inputstream gestito
            try {waitForRelease.await();} catch (InterruptedException ignored) {}
            waitForFreeze=null;
            waitForRelease=null; //pronti per il prossimo giro
            running=true;
            if (startlock!=null) startlock.open();
            raise(RESTORED); //Avviamo chi è interessato che la consegna degli eventi è riattivata
         }
         try {
            takenbytes=istream.read(buffer);
            if (takenbytes!=-1) {
               raise(DATAARRIVED).with(buffer, takenbytes);
               if (istream.available()>buffer.length) buffer=new byte[istream.available()];
            }
            else timeToClose=true;
         } catch (IOException ex) {
            if (!timeToClose) {
               StreamErrorReaction ex2=new StreamErrorReaction(ex);               
               raise(STREAMEXCEPTION).with(ex2);
               if (ex2.stopRequested()) timeToClose=true;
            }
         }
      } while (!timeToClose);

      running=false;

      raise(STOPPED);
      runner.setName(prevThreadName);
   }
}
