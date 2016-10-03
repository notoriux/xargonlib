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
   private AtomicBoolean mustStop=new AtomicBoolean(false);

   public boolean isRunning() {
      return globalSelector!=null;
   }
   
   public synchronized void start() {
      if (loopTask!=null) return;
      mustStop.set(false);
      threadPool=Executors.newCachedThreadPool();
      loopTask=threadPool.submit(this::runLoop);
   }
   
   public synchronized void stop() {
      if (loopTask==null) return;
      try {
         mustStop.set(true);
         loopTask.cancel(true);
         loopTask.get();
         loopTask=null;
         threadPool.shutdown();
         threadPool.awaitTermination(5, TimeUnit.MINUTES);
      } catch (InterruptedException | ExecutionException e) {
         e.printStackTrace(System.err);
      }
   }
   
   public SelectionKey register(SelectableChannel channel, SelectionProcessor proc) {
      if (loopTask==null) throw new IllegalStateException("Selector not started");
      try {
         return channel.register(globalSelector, channel.validOps(), proc);
      } catch (ClosedChannelException ex) {
         throw new IllegalStateException("Invalid channel: closed", ex);
      }
   }

   private void runLoop() {
      try {
         globalSelector=Selector.open();
      } catch (IOException ex) {
         ex.printStackTrace();
         return;
      }
      
      while (!mustStop.get()) {
         try {
            int ops=globalSelector.select();
            if (ops==0) continue;

            Set<SelectionKey> selectedKeys=globalSelector.selectedKeys();
            
            selectedKeys.forEach(key -> {
               selectedKeys.remove(key);
               SelectionProcessor proc=(SelectionProcessor)key.attachment();
               threadPool.submit(() -> proc.processKey(this, key));
            });
            
         } catch (Exception ex) {
            if (!(ex instanceof ClosedChannelException)) ex.printStackTrace();
            mustStop.set(true);
         }
      }
      
      try {
         globalSelector.close();
      } catch (IOException ex) {
         ex.printStackTrace();
      }
      
      globalSelector=null;
   }
}
