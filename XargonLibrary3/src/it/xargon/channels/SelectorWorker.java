package it.xargon.channels;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.IOException;
import java.nio.channels.*;

public class SelectorWorker {
   private ExecutorService threadPool=null;
   private Selector globalSelector=null;
   private Future<?> loopTask=null;
   private Thread selectorThread=null;
   private AtomicBoolean mustStop=new AtomicBoolean(false);
   private AtomicBoolean mustSuspend=new AtomicBoolean(false);
   private AtomicBoolean suspended=new AtomicBoolean(false);
   private Object suspendMonitor=new Object();
   private Object startMonitor=new Object();

   public boolean isRunning() {
      return loopTask!=null;
   }
   
   public boolean isSuspended() {
      return suspended.get();
   }
   
   public synchronized void start() {
      if (loopTask!=null) return;
      mustStop.set(false);
      threadPool=Executors.newCachedThreadPool();
      loopTask=threadPool.submit(this::runLoop);
      synchronized (startMonitor) {
         try {
            startMonitor.wait();
         } catch (InterruptedException ignored) {}
      }
   }
   
   public synchronized void suspend() {
      if (Thread.currentThread().equals(selectorThread)) return;
      if (suspended.get()) return;
      mustSuspend.set(true);
      synchronized (suspendMonitor) {
         globalSelector.wakeup();
         //attendere che il selector sia effettivamente entrato in sospensione
         try {suspendMonitor.wait();} catch (InterruptedException ignored) {}
      }
   }
   
   public synchronized void resume() {
      if (Thread.currentThread().equals(selectorThread)) return;
      if (!suspended.get()) return;
      mustSuspend.set(false);
      synchronized (suspendMonitor) {
         suspendMonitor.notify();
         //attendere che il selector sia effettivamente uscito dalla sospensione
         try {suspendMonitor.wait();} catch (InterruptedException ignored) {}
      }
   }
   
   public synchronized void stop() {
      if (loopTask==null) return;
      try {
         mustStop.set(true);
         resume();
         globalSelector.wakeup();
         loopTask.get();
         loopTask=null;
         threadPool.shutdown();
         threadPool.awaitTermination(5, TimeUnit.MINUTES);
      } catch (InterruptedException | ExecutionException e) {
         e.printStackTrace(System.err);
      }
   }
   
   public SelectionKey register(SelectableChannel channel, int interestedOps, SelectionProcessor proc) {
      if (loopTask==null) throw new IllegalStateException("Selector not started");
      try {
         boolean mustResume=!isSuspended();
         suspend();
         SelectionKey selKey=channel.register(globalSelector, interestedOps, proc);
         if (mustResume) resume();
         return selKey;
      } catch (ClosedChannelException ex) {
         throw new IllegalStateException("Invalid channel: closed", ex);
      }
   }

   private void runLoop() {
      selectorThread=Thread.currentThread();
      try {
         globalSelector=Selector.open();
      } catch (IOException ex) {
         ex.printStackTrace();
         return;
      } finally {
         synchronized (startMonitor) {startMonitor.notify();}
      }
      
      while (!mustStop.get()) {
         try {
            int ops=globalSelector.select();
            
            while (mustSuspend.get()) {
               synchronized (suspendMonitor) {
                  suspended.set(true);
                  suspendMonitor.notify(); //risveglia il thread che ha imposto l'attesa
                  suspendMonitor.wait(); //rimane qui finché non viene risvegliato da resume
                  suspended.set(false);
                  suspendMonitor.notify(); //risveglia il thread che ha imposto il resume
               }
            }
            
            if (ops==0) continue;

            Set<SelectionKey> selectedKeys=globalSelector.selectedKeys();
            
            selectedKeys.forEach(key -> {
               selectedKeys.remove(key);
               SelectionProcessor proc=(SelectionProcessor)key.attachment();
               Runnable parallelProcess=proc.processKey(this, key);
               if (parallelProcess!=null) threadPool.submit(parallelProcess::run);
            });
         } catch (Exception ex) {
            if (!(ex instanceof ClosedChannelException)) ex.printStackTrace();
            mustStop.set(true);
         }
      }
      
      try {
         //annullare tutte le registrazioni e chiudere i canali registrati
         //(in realtà dovrebbero farlo i rispettivi client di SelectorWorker)
         globalSelector.keys().forEach(key -> {
            if (key.channel().isOpen()) try {key.channel().close();} catch (IOException ignored) {}
            if (key.channel().isRegistered()) key.cancel();
         });
         globalSelector.close();
      } catch (IOException ex) {
         ex.printStackTrace();
      }
      
      globalSelector=null;
   }
}
