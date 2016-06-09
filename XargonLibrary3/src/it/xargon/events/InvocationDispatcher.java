package it.xargon.events;

import it.xargon.events.EventsSource.ExceptionReaction;

import java.lang.reflect.*;
import java.util.concurrent.ExecutorService;

class InvocationDispatcher {
   private Object[] itargets=null;
   private Method imeth=null;
   private Object[] iargs=null;
   private EventsSourceImpl iowner=null;
   private volatile boolean invoked=false;
   private Object chainedResult=null;
   
   public InvocationDispatcher(EventsSourceImpl owner, Object[] targets, Method evmeth, Object[] args) {
      iowner=owner;
      itargets=targets;
      imeth=evmeth;
      iargs=args;
   }
   
   public synchronized boolean isInvoked() {return invoked;}
   
   public synchronized Object getChainedResult() {
      if (!invoked) throw new IllegalStateException("Dispatcher not invoked");
      return chainedResult;
   }
   
   public synchronized void checkInvoked() {
      if (invoked) throw new IllegalStateException("Dispatcher already invoked");
      invoked=true;      
   }
   
   public void simpleInvocation() {
      checkInvoked();
 
      for(Object target:itargets) {
         try {InvocationDispatcher.dispatch(imeth, target, iargs);}
         catch (Throwable tr) {
            ExceptionReaction reaction=iowner.handleUncaughtException(target, imeth.getDeclaringClass(), tr);
            switch (reaction) {
               case CONTINUE: continue;
               case STOP: return;
               case UNREGISTER: //implica CONTINUE
                  iowner.internal_unregister(imeth.getDeclaringClass(), target);
                  continue;
            }
         }
      } 
   }
   
   public void parallelInvocation(ExecutorService threadPool) {
      checkInvoked();

      for(Object target:itargets) {
         Runnable task=()->{
            try {InvocationDispatcher.dispatch(imeth, target, iargs);}
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

      Object[] _args=new Object[iargs.length];
      System.arraycopy(iargs, 0, _args, 0, iargs.length);

      for(Object target:itargets) {
         try {
            chainedResult=InvocationDispatcher.dispatch(imeth, target, _args);
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
   
   private static Object dispatch(Method evmeth, Object target, Object[] args) throws Throwable {
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
   }
}
