package it.xargon.events;

import it.xargon.events.EventsSource.ExceptionReaction;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ExecutorService;

class InvocationDispatcher {
   private Object[] itargets=null;
   private Method imeth=null;
   private Object[] iargs=null;
   private EventsSourceImpl iowner=null;
   private volatile boolean invoked=false;
   
   public static ThreadLocal<LinkedList<EventsSource>> currentSources=new ThreadLocal<LinkedList<EventsSource>>() {
      @Override
      protected LinkedList<EventsSource> initialValue() {
         return new LinkedList<>();
      }
   };
   
   public static ThreadLocal<LinkedList<LinkedList<Object>>> eventResults=new ThreadLocal<LinkedList<LinkedList<Object>>>() {
      @Override
      protected LinkedList<LinkedList<Object>> initialValue() {
         return new LinkedList<>();
      }
   };
   
   public InvocationDispatcher(EventsSourceImpl owner, Object[] targets, Method evmeth, Object[] args) {
      iowner=owner;
      itargets=targets;
      imeth=evmeth;
      iargs=args;
   }
   
   public synchronized boolean isInvoked() {return invoked;}
   
   public synchronized void checkInvoked() {
      if (invoked) throw new IllegalStateException("Dispatcher already invoked");
      invoked=true;      
   }
   
   public Object simpleInvocation() {
      checkInvoked();
      Object result=null;
      
      //Prepararsi a raccogliere tutti gli eventuali risultati da parte dei listener
      LinkedList<Object> allResults=new LinkedList<>();
      eventResults.get().addLast(allResults);
      
      try {
         for(Object target:itargets) {
            try {
               result=null;
               result=InvocationDispatcher.dispatch(iowner, imeth, target, iargs);
               //In questo modo tutti i listener possono accedere all'elenco dei
               //risultati degli altri listener per lo stesso evento
               if (result!=null) {
                  //Se il risultato è già presente, ne tiene una sola copia (ma la mette per ultima)
                  if (allResults.contains(result)) allResults.remove(result);
                  allResults.addLast(result);
               }
            } catch (Throwable tr) {
               ExceptionReaction reaction=iowner.handleUncaughtException(target, imeth.getDeclaringClass(), tr);
               switch (reaction) {
                  case CONTINUE: continue;
                  case STOP: return null;
                  case UNREGISTER: //implica CONTINUE
                     iowner.internal_unregister(imeth.getDeclaringClass(), target);
                     continue;
               }
            }
         }
         
         //Il risultato della chiamata è l'ultimo inserito nell'elenco dei risultati
         //(o null se non vi sono stati risultati)
         Object[] results=allResults.toArray();
         return (results==null || results.length==0)?null:results[results.length-1];
      } finally {
         eventResults.get().remove(allResults); //Finita la festa, si mette tutto in ordine
      }
   }
   
   public void parallelInvocation(ExecutorService threadPool) {
      checkInvoked();

      for(Object target:itargets) {
         Runnable task=()->{
            try {InvocationDispatcher.dispatch(iowner, imeth, target, iargs);}
            catch (Throwable tr) {
               ExceptionReaction reaction=iowner.handleUncaughtException(target, imeth.getDeclaringClass(), tr);
               switch (reaction) {
                  //Si tratta di un'invocazione seriale, gli altri thread procedono per conto proprio
                  //quindi un "STOP" non avrebbe effetto
                  case CONTINUE: return;
                  case STOP: return;
                  case UNREGISTER:
                     iowner.internal_unregister(imeth.getDeclaringClass(), target);
                     return;
               }
            }
         };
         if (threadPool!=null) threadPool.submit(task);
         else new Thread(task).start();
      }      
   }
   
   public void serialInvocation(ExecutorService threadPool) {
      checkInvoked();

      Runnable task=()->simpleInvocation();
      if (threadPool!=null) threadPool.submit(task);
      else new Thread(task).start();      
   }
   
   public void swingInvocation() {
      checkInvoked();

      if (javax.swing.SwingUtilities.isEventDispatchThread()) simpleInvocation();
      else {Runnable task=()->simpleInvocation();javax.swing.SwingUtilities.invokeLater(task);}
   }
   
   public Object chainedInvocation(boolean onFirst) {
      checkInvoked();
      Object chainedResult=null;

      Object[] _args=new Object[iargs.length];
      System.arraycopy(iargs, 0, _args, 0, iargs.length);

      for(Object target:itargets) {
         try {
            chainedResult=InvocationDispatcher.dispatch(iowner, imeth, target, _args);
            if (onFirst) _args[0]=chainedResult; else _args[_args.length-1]=chainedResult;
         } catch (Throwable tr) {
            ExceptionReaction reaction=iowner.handleUncaughtException(target, imeth.getDeclaringClass(), tr);
            switch (reaction) {
               case CONTINUE:
                  //Lascia invariati gli argomenti, tenendo come valido il risultato della
                  //precedente invocazione
                  continue;
               case STOP:
                  //Considera valido il risultato della precedente invocazione
                  return chainedResult;
               case UNREGISTER:
                  //implica CONTINUE, lascia invariati gli argomenti
                  iowner.internal_unregister(imeth.getDeclaringClass(), target);
                  continue;
            }
         }
      }

      return chainedResult;
   }
   
   private static Object dispatch(EventsSourceImpl evSource, Method evmeth, Object target, Object[] args) throws Throwable {
      try {
         currentSources.get().addFirst(evSource);
         
         Object result=null;
         if (target instanceof InvocationHandler) {
            InvocationHandler ih=InvocationHandler.class.cast(target);
            result=ih.invoke(target, evmeth, args);
         } else if (Proxy.isProxyClass(target.getClass())) {
            InvocationHandler ih=Proxy.getInvocationHandler(target);
            result=ih.invoke(target, evmeth, args);
         } else {
            result=evmeth.invoke(target, args);
         }
         return result;
      } finally {
         currentSources.get().removeFirst();
      }
   }
}
