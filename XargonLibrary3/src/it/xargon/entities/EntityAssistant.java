package it.xargon.entities;

import java.util.*;
import java.util.function.Function;
import java.util.function.BiFunction;

public class EntityAssistant<T> {
   public enum TargetCondition {
      NOT_CHECKED, IS_NULL, NOT_NULL, IS_EMPTY, NOT_EMPTY
   }
   
   public enum SourceCondition {
      NOT_CHECKED, NOT_NULL, NOT_EMPTY
   }
   
   private class Policy {
      public TargetCondition targetCond=null;
      public SourceCondition sourceCond=null;
      
      public Policy() {
         this.targetCond=TargetCondition.NOT_CHECKED;
         this.sourceCond=SourceCondition.NOT_CHECKED;
      }

      public Policy(TargetCondition targetCond) {
         this.targetCond=targetCond;
         this.sourceCond=SourceCondition.NOT_CHECKED;
      }

      public Policy(SourceCondition sourceCond) {
         this.targetCond=TargetCondition.NOT_CHECKED;
         this.sourceCond=sourceCond;
      }

      public Policy(TargetCondition targetCond, SourceCondition sourceCond) {
         this.targetCond=targetCond;
         this.sourceCond=sourceCond;
      }
   }
   
   private Class<T> instanceClass;
   private T assistedEntity;
   private EntityPropertiesManager<T> propManager=null;
   private LinkedHashMap<String, Function<?,?>> updaters=null;
   private LinkedHashMap<String, Policy> policies=null;
      
   public class UpdateTarget {
      private String[] targetProperties=null;
      
      private UpdateTarget(boolean tolerant, String... properties) {
         Objects.requireNonNull(properties);
         ArrayList<String> filteredProps=new ArrayList<>();
         for(int i=0;i<properties.length;i++) {
            if (testProperty(tolerant, properties[i])) filteredProps.add(properties[i]);
         }
         targetProperties=filteredProps.toArray(new String[filteredProps.size()]);
      }
            
      public UpdatePolicy withValuesFrom(final T sourceEntity) {
         Objects.requireNonNull(sourceEntity);
         
         for(final String propName:targetProperties) {
            updaters.put(propName, t -> propManager.get(propName, sourceEntity));
         }
         
         return new UpdatePolicy(targetProperties);
      }
      
      public UpdatePolicy withValuesFromOther(final Object sourceEntity) {
         Objects.requireNonNull(sourceEntity);
         final EntityPropertiesManager<?> otherPropMgr=EntityPropertiesManager.get(sourceEntity.getClass());

         for(final String propName:targetProperties) {
            if (!otherPropMgr.hasProperty(propName)) continue;
            updaters.put(propName, t -> otherPropMgr.get(propName, sourceEntity));
         }
         
         return new UpdatePolicy(targetProperties);
      }
      
      public UpdatePolicy withValuesFromMap(final Map<String,?> sourceMap) {
         Objects.requireNonNull(sourceMap);

         for(final String propName:targetProperties) {
            if (!sourceMap.containsKey(propName)) continue;
            updaters.put(propName, t -> sourceMap.get(propName));
         }
         
         return new UpdatePolicy(targetProperties);
      }

      public UpdatePolicy withValue(final Object value) { //Value può anche essere null
         for(final String propName:targetProperties) {
            updaters.put(propName, t -> value);
         }
         
         return new UpdatePolicy(targetProperties);
      }
      
      public UpdatePolicy withValues(final Object... values) {
         if (values.length!=targetProperties.length)
            throw new IllegalArgumentException("Provided values quantity must match the properties quantity");
         
         int i=0;
         
         for(final String propName:targetProperties) {
            final Object value=values[i]; //necessario per assegnare un riferimento invariabile all'oggetto
            updaters.put(propName, t -> value);
            i++;
         }
         
         return new UpdatePolicy(targetProperties);
      }
      
      @SuppressWarnings("unchecked")
      public <P> UpdatePolicy withFunction(Class<P> propClass, final Function<P,P> function) {
         Objects.requireNonNull(propClass);
         Objects.requireNonNull(function);
                  
         for(final String propName:targetProperties) {
            updaters.put(propName, t -> function.apply((P) t));
         }
         
         return new UpdatePolicy(targetProperties);
      }
      
      @SuppressWarnings("unchecked")
      public <P> UpdatePolicy withValuesMappedFrom(final T sourceEntity, Class<P> propClass, final Function<P,P> function) {
         Objects.requireNonNull(sourceEntity);
         Objects.requireNonNull(propClass);
         Objects.requireNonNull(function);
         
         for(final String propName:targetProperties) {
            updaters.put(propName, t -> function.apply((P)propManager.get(propName, sourceEntity)));
         }
         
         return new UpdatePolicy(targetProperties);
      }

      @SuppressWarnings("unchecked")
      public <P> UpdatePolicy withValuesMappedFromOther(final Object sourceEntity, Class<P> propClass, final Function<P,P> function) {
         Objects.requireNonNull(sourceEntity);
         Objects.requireNonNull(propClass);
         Objects.requireNonNull(function);
         
         final EntityPropertiesManager<?> otherPropMgr=EntityPropertiesManager.get(sourceEntity.getClass());
         
         for(final String propName:targetProperties) {
            if (!otherPropMgr.hasProperty(propName)) continue;
            updaters.put(propName, t -> function.apply((P)otherPropMgr.get(propName, sourceEntity)));
         }
         
         return new UpdatePolicy(targetProperties);
      }
      
      @SuppressWarnings("unchecked")
      public <P,R> UpdatePolicy withValuesMappedFromOther(final Object sourceEntity, Class<P> sourcePropClass, Class<R> destPropClass, final Function<P,R> function) {
         Objects.requireNonNull(sourceEntity);
         Objects.requireNonNull(sourcePropClass);
         Objects.requireNonNull(destPropClass);
         Objects.requireNonNull(function);
         
         final EntityPropertiesManager<?> otherPropMgr=EntityPropertiesManager.get(sourceEntity.getClass());
         
         for(final String propName:targetProperties) {
            if (!otherPropMgr.hasProperty(propName)) continue;
            updaters.put(propName, t -> function.apply((P)otherPropMgr.get(propName, sourceEntity)));
         }
         
         return new UpdatePolicy(targetProperties);
      }
      
      @SuppressWarnings("unchecked")
      public <P> UpdatePolicy withValuesMappedFromMap(final Map<String,?> sourceMap, Class<P> propClass, final Function<P,P> function) {
         Objects.requireNonNull(sourceMap);
         Objects.requireNonNull(propClass);
         Objects.requireNonNull(function);
         
         for(final String propName:targetProperties) {
            if (!sourceMap.containsKey(propName)) continue;
            updaters.put(propName, t -> function.apply((P)sourceMap.get(propName)));
         }
         
         return new UpdatePolicy(targetProperties);
      }

      @SuppressWarnings("unchecked")
      public <P,R> UpdatePolicy withValuesMappedFromMap(final Map<String,?> sourceMap, Class<P> sourcePropClass, Class<R> destPropClass, final Function<P,R> function) {
         Objects.requireNonNull(sourceMap);
         Objects.requireNonNull(sourcePropClass);
         Objects.requireNonNull(destPropClass);
         Objects.requireNonNull(function);
         
         for(final String propName:targetProperties) {
            if (!sourceMap.containsKey(propName)) continue;
            updaters.put(propName, t -> function.apply((P)sourceMap.get(propName)));
         }
         
         return new UpdatePolicy(targetProperties);
      }
      
      @SuppressWarnings("unchecked")
      public <P> UpdatePolicy withValuesMappedFrom(final T sourceEntity, Class<P> propClass, final BiFunction<P,P,P> function) {
         Objects.requireNonNull(sourceEntity);
         Objects.requireNonNull(propClass);
         Objects.requireNonNull(function);

         for(final String propName:targetProperties) {
            updaters.put(propName, t -> function.apply((P)t, (P)propManager.get(propName, sourceEntity)));
         }
         
         return new UpdatePolicy(targetProperties);
      }
            
      @SuppressWarnings("unchecked")
      public <P> UpdatePolicy withValuesMappedFromOther(final Object sourceEntity, Class<P> propClass, final BiFunction<P,P,P> function) {
         Objects.requireNonNull(sourceEntity);
         Objects.requireNonNull(propClass);
         Objects.requireNonNull(function);

         final EntityPropertiesManager<?> otherPropMgr=EntityPropertiesManager.get(sourceEntity.getClass());

         for(final String propName:targetProperties) {
            if (!otherPropMgr.hasProperty(propName)) continue;
            updaters.put(propName, t -> function.apply((P)t, (P)otherPropMgr.get(propName, sourceEntity)));
         }
         
         return new UpdatePolicy(targetProperties);
      }
      
      @SuppressWarnings("unchecked")
      public <P,R> UpdatePolicy withValuesMappedFromOther(final Object sourceEntity, Class<P> sourcePropClass, Class<R> destPropClass, final BiFunction<R,P,R> function) {
         Objects.requireNonNull(sourceEntity);
         Objects.requireNonNull(sourcePropClass);
         Objects.requireNonNull(destPropClass);
         Objects.requireNonNull(function);

         final EntityPropertiesManager<?> otherPropMgr=EntityPropertiesManager.get(sourceEntity.getClass());

         for(final String propName:targetProperties) {
            if (!otherPropMgr.hasProperty(propName)) continue;
            updaters.put(propName, t -> function.apply((R)t, (P)otherPropMgr.get(propName, sourceEntity)));
         }
         
         return new UpdatePolicy(targetProperties);
      }

      @SuppressWarnings("unchecked")
      public <P> UpdatePolicy withValuesMappedFromMap(final Map<String,?> sourceMap, Class<P> propClass, final BiFunction<P,P,P> function) {
         Objects.requireNonNull(sourceMap);
         Objects.requireNonNull(propClass);
         Objects.requireNonNull(function);

         for(final String propName:targetProperties) {
            if (!sourceMap.containsKey(propName)) continue;
            updaters.put(propName, t -> function.apply((P)t, (P)sourceMap.get(propName)));
         }
         
         return new UpdatePolicy(targetProperties);
      }
      
      @SuppressWarnings("unchecked")
      public <P,R> UpdatePolicy withValuesMappedFromMap(final Map<String,?> sourceMap, Class<P> sourcePropClass, Class<R> destPropClass, final BiFunction<R,P,R> function) {
         Objects.requireNonNull(sourceMap);
         Objects.requireNonNull(sourcePropClass);
         Objects.requireNonNull(destPropClass);
         Objects.requireNonNull(function);

         for(final String propName:targetProperties) {
            if (!sourceMap.containsKey(propName)) continue;
            updaters.put(propName, t -> function.apply((R)t, (P)sourceMap.get(propName)));
         }
         
         return new UpdatePolicy(targetProperties);
      }
   }
   
   public class UpdatePolicy {
      private String[] targetProperties=null;
      
      private UpdatePolicy(String... properties) {
         targetProperties=Objects.requireNonNull(properties);
      }
      
      public EntityAssistant<T> inconditionally() {
         Policy pol=new Policy();
         for(String prop:targetProperties) policies.put(prop, pol);
         return EntityAssistant.this;
      }
      
      public EntityAssistant<T> onlyIf(TargetCondition targetCond) {
         Policy pol=new Policy(targetCond);
         for(String prop:targetProperties) policies.put(prop, pol);
         return EntityAssistant.this;
      }
      
      public EntityAssistant<T> onlyIf(SourceCondition sourceCond) {
         Policy pol=new Policy(sourceCond);
         for(String prop:targetProperties) policies.put(prop, pol);
         return EntityAssistant.this;
      }
      
      public EntityAssistant<T> onlyIf(TargetCondition targetCond, SourceCondition sourceCond) {
         Policy pol=new Policy(targetCond, sourceCond);
         for(String prop:targetProperties) policies.put(prop, pol);
         return EntityAssistant.this;
      }
   }
   
   @SuppressWarnings("unchecked")
   private EntityAssistant(T assistedEntity) {
      this.assistedEntity=assistedEntity;      
      this.instanceClass=(Class<T>) this.assistedEntity.getClass();
      propManager=EntityPropertiesManager.get(instanceClass);      
      updaters=new LinkedHashMap<>();
      policies=new LinkedHashMap<>();
   }
   
   public static <T> EntityAssistant<T> of(T assistedEntity) {
      return new EntityAssistant<T>(Objects.requireNonNull(assistedEntity));
   }
   
   private boolean testProperty(boolean tolerant, String property) {
      if (updaters.containsKey(property)) {
         if (tolerant) return false;
         else throw new IllegalArgumentException("Property \"" + property + "\" already used");
      }
      if (propManager.isReadOnly(property)) {
         if (tolerant) return false;
         else throw new IllegalArgumentException("Property \"" + property + "\" must be readable AND writable");
      }
      return true;
   }
   
   public UpdateTarget updates(String... properties) {return new UpdateTarget(false, properties);}

   public UpdateTarget updatesEverything() {
      return new UpdateTarget(true, propManager.getAllProperties());
   }
   
   public static class AssistantException extends RuntimeException {
      public AssistantException(String propertyName, Throwable cause) {
         super(makeMessage(propertyName, cause), cause);
      }
      
      private static String makeMessage(String propertyName, Throwable cause) {
         return "Error while processing property \"" + propertyName + "\": " + cause.getMessage();
      }
   }
   
   @SuppressWarnings("unchecked")
   public String[] apply() {
      LinkedList<String> result=new LinkedList<String>();
      for(Map.Entry<String, Function<?,?>> updaterEntry:updaters.entrySet()) {
         Object originalValue=propManager.get(updaterEntry.getKey(), assistedEntity);
         Object processedValue=null;
         
         @SuppressWarnings("rawtypes")
         Function mapper=updaterEntry.getValue();
         try {processedValue=mapper.apply(originalValue);} catch (Exception ex) {
            throw new AssistantException(updaterEntry.getKey(), ex);
         }
         
         //La proprietà sull'entità assistita viene settata seguendo le policy determinate
         //in fase di creazione dell'assistente
         
         Policy pol=policies.get(updaterEntry.getKey());
         
         boolean targetChecked=
               (pol.targetCond.equals(TargetCondition.NOT_CHECKED)) ||
               (pol.targetCond.equals(TargetCondition.IS_NULL) && originalValue==null) ||
               (pol.targetCond.equals(TargetCondition.IS_EMPTY) && originalValue==null) ||
               (pol.targetCond.equals(TargetCondition.NOT_NULL) && originalValue!=null) ||
               (pol.targetCond.equals(TargetCondition.IS_EMPTY) && (originalValue instanceof String) && (((String)originalValue).isEmpty())) ||
               (pol.targetCond.equals(TargetCondition.NOT_EMPTY) && (originalValue instanceof String) && (!((String)originalValue).isEmpty())) ||
               (pol.targetCond.equals(TargetCondition.IS_EMPTY) && (originalValue instanceof Map) && (((Map<?, ?>)originalValue).isEmpty())) ||
               (pol.targetCond.equals(TargetCondition.NOT_EMPTY) && (originalValue instanceof Map) && (!((Map<?, ?>)originalValue).isEmpty())) ||
               (pol.targetCond.equals(TargetCondition.IS_EMPTY) && (originalValue instanceof Collection) && (((Collection<?>)originalValue).isEmpty())) ||
               (pol.targetCond.equals(TargetCondition.NOT_EMPTY) && (originalValue instanceof Collection) && (!((Collection<?>)originalValue).isEmpty()));
         
         boolean sourceChecked=
               (pol.sourceCond.equals(SourceCondition.NOT_CHECKED)) ||
               (pol.sourceCond.equals(SourceCondition.NOT_NULL) && processedValue!=null) ||
               (pol.sourceCond.equals(SourceCondition.NOT_EMPTY) && ( 
                     (processedValue instanceof CharSequence && ((CharSequence)processedValue).length()>0) ||
                     (!(processedValue instanceof CharSequence) && processedValue!=null) ||
                     (processedValue instanceof Map && ((Map<?,?>)processedValue).size()>0) ||
                     (processedValue instanceof Collection && ((Collection<?>)processedValue).size()>0))
               );
         
         if (targetChecked && sourceChecked) {
            result.add(updaterEntry.getKey());
            propManager.set(updaterEntry.getKey(), assistedEntity, processedValue);
         }
      }
      return result.toArray(new String[result.size()]);
   }
}
