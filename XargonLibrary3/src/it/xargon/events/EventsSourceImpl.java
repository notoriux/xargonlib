package it.xargon.events;

import it.xargon.util.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ExecutorService;

public abstract class EventsSourceImpl implements EventsSource {
   private HashMap<Class<?>, EventProcessor> eventProcessors=null;
   private HashMap<Identifier, SinkRegistration> registrationIds=null;
   private IdGenerator idgen=null;
   
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

         InvocationDispatcher idisp=new InvocationDispatcher(EventsSourceImpl.this, allSinks, method, args);
         
         switch (eventKind) {
            case SIMPLE: idisp.simpleInvocation(); break;
            case SERIAL: idisp.serialInvocation(getThreadPool()); break;
            case PARALLEL: idisp.parallelInvocation(getThreadPool()); break;
            case SWING: idisp.swingInvocation(); break;
            case CHAIN_FIRST: result=idisp.chainedInvocation(true); break;
            case CHAIN_LAST: result=idisp.chainedInvocation(false); break;
         }
         
         return result;
      }
   }
   
   public EventsSourceImpl() {
      idgen=new IdGenerator();
      eventProcessors=new HashMap<>();
      registrationIds=new HashMap<>();
      Class<?>[] innerClasses=this.getClass().getClasses();
      for(Class<?> inner:innerClasses) {
         Event evAnnot=inner.getAnnotation(Event.class);
         FunctionalInterface funcAnnot=inner.getAnnotation(FunctionalInterface.class);
         if (evAnnot!=null && funcAnnot!=null) eventProcessors.put(inner, new EventProcessor(inner));
      }
   }
   
   protected ExecutorService getThreadPool() {return null;}
   
   protected ExceptionReaction handleUncaughtException(Object sink, Class<?> event, Throwable tr) {
      tr.printStackTrace(Debug.stderr);return ExceptionReaction.UNREGISTER;
   }
   
   private void checkEventProcessor(Class<?> evclass) {
      if (!eventProcessors.containsKey(evclass)) throw new IllegalArgumentException(evclass.getCanonicalName()+" is not a valid event");
   }
   
   protected <T> T raise(Class<T> evclass) {
      checkEventProcessor(evclass);
      EventProcessor evproc=eventProcessors.get(evclass);
      //Ereditare un evento da Runnable indica che non vi sono parametri da passare: invocazione diretta
      if (Runnable.class.isAssignableFrom(evclass)) {
         ((Runnable) evproc.getProxy(evclass)).run();
         return null;
      }
      return evproc.getProxy(evclass);
   }
   
   public Identifier[] bindEvents(final Object sink) {
      //- scandire tutti i metodi immediati (non derivati) della classe di "sink", pubblici e privati
      Class<?> sinkClass=sink.getClass();
      Method[] methodsToScan=sinkClass.getDeclaredMethods();
      HashMap<Class<?>, Object> allFoundSinks=new HashMap<>();

      for(final Method sinkMethod:methodsToScan) {
         OnEvent eventAnnot=sinkMethod.getAnnotation(OnEvent.class);
         if (eventAnnot!=null) {
            Class<?> eventClass=eventAnnot.value();
            //- per ogni metodo annotato con @OnEvent:
            //- controllare se la classe passata a @OnEvent è presente tra
            //  gli eventi rilevati in fase di costruzione
            if (!eventProcessors.containsKey(eventClass)) 
               throw new IllegalArgumentException("Class \"" + eventClass.getName() + "\" on sink method \"" + sinkMethod.getName() + "\" isn't an event");
            
            //- controllare se la firma del metodo corrisponde alla firma
            //  del metodo dell'interfaccia funzionale associata all'evento
            Method eventMethod=eventClass.getMethods()[0];
            Class<?>[] sinkSignature=sinkMethod.getParameterTypes();
            Class<?>[] eventSignature=eventMethod.getParameterTypes();
            
            //- se la firma non corrisponde, emettere un'eccezione e fallire l'intera operazione.
            if (!Arrays.equals(sinkSignature, eventSignature))
               throw new IllegalArgumentException("Sink method \"" + sinkMethod.getName() + "\" signature isn't the same of the event \"" + eventClass.getCanonicalName() + "\"");
            
            //Altrimenti...
            //- creare un proxy tramite interfaccia funzionale dell'evento, il cui InvocationHandler richiami esattamente
            //  il metodo dell'oggetto sink che abbiamo trovato prima. Settare il flag "accessible" se necessario.

            if (Modifier.isPrivate(sinkMethod.getModifiers())) sinkMethod.setAccessible(true);
            
            Object sinkInvoker=Proxy.newProxyInstance(
                  Thread.currentThread().getContextClassLoader(),
                  new Class<?>[] {eventClass},
                  (object, method, args) -> {
                     //Paracadute per i metodi della classe Object
                     /*if (method.getClass().equals(Object.class)) return method.invoke(sink, args);
                     else */return sinkMethod.invoke(sink, args);
                  });
            //- Conservarlo in una lista temporanea.
            allFoundSinks.put(eventClass, sinkInvoker);
         }
      }

      //- Se i proxy sono stati tutti generati con successo, associare ogni proxy al proprio evento
      //- annotare tutti gli Identifier creati
      Identifier[] registrations=new Identifier[allFoundSinks.size()];
      int index=0;
      for(Map.Entry<Class<?>, Object> entry:allFoundSinks.entrySet()) {
         registrations[index]=_onEvent(entry.getKey(), entry.getValue());
         index++;
      }

      return registrations;
   }
   
   public <T> Identifier onEvent(Class<T> evClass, T sink) {
      return _onEvent(evClass, sink);
   }
   
   private Identifier _onEvent(Class<?> evclass, Object sink) {
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
