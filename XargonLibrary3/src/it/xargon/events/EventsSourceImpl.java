package it.xargon.events;

import it.xargon.util.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ExecutorService;

public abstract class EventsSourceImpl implements EventsSource {
   private HashMap<Class<?>, EventProcessor> eventProcessors=null;
   private HashMap<Identifier, SinkRegistration> registrationIds=null;
   private IdGenerator idgen=null;
   private ExecutorService ithreadPool=null;
   
   private class SinkRegistration {
      public EventProcessor eventProcessor;
      public Object sink;
      
      public SinkRegistration(EventProcessor eventProcessor, Object sink) {
         this.eventProcessor=eventProcessor;
         this.sink=sink;
      }
   }
   
   private class EventProcessor implements InvocationHandler {
      private class SinkAssoc {
         public Identifier id;
         public Object sink;
         
         public SinkAssoc(Identifier id, Object sink) {
            this.id=id;
            this.sink=sink;
         }
      }
      
      private Object raiserProxy=null;
      private Class<?> eventClass=null;
      private Event.Kind eventKind=null;
      private Object sinksLock=new Object();
      private SinkAssoc[] sinks=new SinkAssoc[0];
      
      public EventProcessor(Class<?> eventClass) {
         this.eventClass=eventClass;
         Event evAnnot=this.eventClass.getAnnotation(Event.class);
         FunctionalInterface funcAnnot=this.eventClass.getAnnotation(FunctionalInterface.class);
         if (evAnnot==null || funcAnnot==null) throw new IllegalArgumentException(eventClass.getCanonicalName() + " missing proper event annotation");
         eventKind=evAnnot.value();
         raiserProxy=Proxy.newProxyInstance(eventClass.getClassLoader(), new Class<?>[] {this.eventClass}, this);
      }
            
      public boolean addSink(Identifier id, Object sink) {
         synchronized (sinksLock) {
            if (getSinkId(sink)!=null) return false;
            SinkAssoc[] newsinks=new SinkAssoc[sinks.length+1];
            System.arraycopy(sinks, 0, newsinks, 0, sinks.length);
            newsinks[newsinks.length-1]=new SinkAssoc(id, sink);
            sinks=newsinks;
            return true;
         }
      }
      
      public Identifier removeSink(Object sink) {
         synchronized (sinksLock) {
            Identifier removedId=getSinkId(sink);
            if (removedId==null) return null;
            SinkAssoc[] newsinks=new SinkAssoc[sinks.length-1];
            int i=0;
            for(SinkAssoc check:sinks) {
               if (check.sink!=sink) {newsinks[i]=check;i++;}
            }
            sinks=newsinks;
            return removedId;
         }
      }
      
      public Identifier[] removeAllSinks() {
         synchronized (sinksLock) {
            Identifier[] result=new Identifier[sinks.length];
            for(int i=0;i<sinks.length;i++) result[i]=sinks[i].id;
            sinks=new SinkAssoc[0];
            return result;
         }
      }
      
      public Identifier getSinkId(Object sink) {
         synchronized (sinksLock) {
            for(SinkAssoc test:sinks) if (test.sink==sink) return test.id;
            return null;
         }
      }
      
      public Object[] getAllSinks() {
         synchronized (sinksLock) {
            Object[] result=new Object[sinks.length];
            for(int i=0;i<sinks.length;i++) result[i]=sinks[i].sink;
            return result;
         }
      }
      
      @SuppressWarnings("unchecked")
      public <T> T getProxy(Class<T> proxyClass) {
         if (proxyClass.equals(eventClass)) return (T)raiserProxy;
         return null;
      }

      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
         Object result=null;
         Object[] allSinks=getAllSinks();
         
         if (allSinks.length==0) return null;

         switch (eventKind) {
            case SIMPLE: 
               new InvocationDispatcher(EventsSourceImpl.this, allSinks, method, args).simpleInvocation();
               break;
            case SERIAL:
               new InvocationDispatcher(EventsSourceImpl.this, allSinks, method, args).serialInvocation(ithreadPool);
               break;
            case PARALLEL:
               new InvocationDispatcher(EventsSourceImpl.this, allSinks, method, args).parallelInvocation(ithreadPool);
               break;
            case SWING:
               new InvocationDispatcher(EventsSourceImpl.this, allSinks, method, args).swingInvocation();
               break;
            case CHAIN_FIRST:
               result=new InvocationDispatcher(EventsSourceImpl.this, allSinks, method, args).chainedInvocation(true);
               break;
            case CHAIN_LAST:
               result=new InvocationDispatcher(EventsSourceImpl.this, allSinks, method, args).chainedInvocation(false);
               break;
         }
         
         return result;
      }
   }
   
   public EventsSourceImpl() {this(null);}
   
   public EventsSourceImpl(ExecutorService threadPool) {
      ithreadPool=threadPool;
      idgen=new IdGenerator();
      eventProcessors=new HashMap<>();
      Class<?>[] innerClasses=this.getClass().getClasses();
      for(Class<?> inner:innerClasses) {
         Event evAnnot=inner.getAnnotation(Event.class);
         FunctionalInterface funcAnnot=inner.getAnnotation(FunctionalInterface.class);
         if (evAnnot!=null && funcAnnot!=null) eventProcessors.put(inner, new EventProcessor(inner));
      }
   }
   
   protected ExceptionReaction handleUncaughtException(Object sink, Class<?> event, Throwable tr) {
      tr.printStackTrace(Debug.stderr);return ExceptionReaction.UNREGISTER;
   }
   
   private void checkEventProcessor(Class<?> evclass) {
      if (!eventProcessors.containsKey(evclass)) throw new IllegalArgumentException(evclass.getCanonicalName()+" is not a valid event");
   }
   
   protected <T> T raise(Class<T> evclass) {
      checkEventProcessor(evclass);
      EventProcessor evproc=eventProcessors.get(evclass);
      return evproc.getProxy(evclass);
   }
   
   public <T> Identifier onEvent(Class<T> evclass, T sink) {
      checkEventProcessor(evclass);
      EventProcessor evproc=eventProcessors.get(evclass);
      synchronized (registrationIds) {
         Identifier id=idgen.next();
         registrationIds.put(id, new SinkRegistration(evproc, sink));
         evproc.addSink(id, sink);
         return id;
      }
   }
   
   public <T> Identifier unregister(Class<T> evclass, T sink) {
      return internal_unregister(evclass, sink);
   }
   
   Identifier internal_unregister(Class<?> evclass, Object sink) {
      checkEventProcessor(evclass);
      EventProcessor evproc=eventProcessors.get(evclass);
      synchronized (registrationIds) {
         Identifier id=evproc.removeSink(sink);
         if (id!=null) registrationIds.remove(id);
         return id;
      }
   }
   
   public boolean unregister(Identifier[] ids) {
      synchronized (registrationIds) {
         for(Identifier id:ids) {if (!registrationIds.containsKey(id)) return false;}
         for(Identifier id:ids) {
            SinkRegistration reg=registrationIds.get(id);
            Identifier checkId=reg.eventProcessor.removeSink(reg.sink);
            if (!checkId.equals(id)) throw new IllegalStateException("FATAL: provided registration ID differs from stored ID");
            registrationIds.remove(id);
         }
         return true;
      }
   }
   
   public boolean unregister(Identifier id) {
      synchronized (registrationIds) {
         SinkRegistration reg=registrationIds.get(id);
         if (reg==null) return false;
         Identifier checkId=reg.eventProcessor.removeSink(reg.sink);
         if (!checkId.equals(id)) throw new IllegalStateException("FATAL: provided registration ID differs from stored ID");
         registrationIds.remove(id);
         return true;
      }
   }
   
   public void unregisterAll(Class<?> evclass) {
      if (evclass==null) throw new IllegalArgumentException("Please specify an event interface");
      checkEventProcessor(evclass);
      EventProcessor evproc=eventProcessors.get(evclass);
      evproc.removeAllSinks();
   }
   
   public void unregisterAll() {
      eventProcessors.values().stream().forEach(evproc -> evproc.removeAllSinks());
   }
}
