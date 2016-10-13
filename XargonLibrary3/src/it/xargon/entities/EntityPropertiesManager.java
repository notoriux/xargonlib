package it.xargon.entities;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class EntityPropertiesManager<T> {
   private static HashMap<String, EntityPropertiesManager<?>> cache=null;
   
   static {
      cache=new HashMap<String, EntityPropertiesManager<?>>();
   }
   
   @SuppressWarnings("unchecked")
   public static <T> EntityPropertiesManager<T> get(Class<T> entityClass) {
      Objects.requireNonNull(entityClass);
      EntityPropertiesManager<T> result=null;
      String className=entityClass.getName();
      if (cache.containsKey(className)) result=(EntityPropertiesManager<T>) cache.get(className);
      else {
         result=new EntityPropertiesManager<T>(entityClass);
         cache.put(className, result);
      }
      return result;
   }
   
   private class PropertyAccessor {
      private Method getMethod=null;
      private Method setMethod=null;
      private TrapSetterAccessor accessorSetMethod=null;
      private Field field=null;
      private Class<?> propClass=null;
      private boolean ignoredInConversion=false;
      
      public PropertyAccessor(Method getMethod, boolean ignoredInConversion) {
         this.getMethod=getMethod;
         this.propClass=getMethod.getReturnType();
         this.ignoredInConversion=ignoredInConversion;
      }
      
      public PropertyAccessor(Method getMethod, Method setMethod, boolean ignoredInConversion) {
         this.getMethod=getMethod;
         this.setMethod=setMethod;
         this.propClass=getMethod.getReturnType();
         this.ignoredInConversion=ignoredInConversion;
      }
      
      public PropertyAccessor(Method getMethod, TrapSetterAccessor accessorSetMethod, boolean ignoredInConversion) {
         this.getMethod=getMethod;
         this.accessorSetMethod=accessorSetMethod;
         this.propClass=getMethod.getReturnType();
         this.ignoredInConversion=ignoredInConversion;
      }
      
      public PropertyAccessor(Field field, boolean ignoredInConversion) {
         this.field=field;
         this.propClass=field.getType();
         this.ignoredInConversion=ignoredInConversion;
      }
      
      public Class<?> getPropertyType() {return propClass;}
      
      public boolean isReadOnly() {return field==null && setMethod==null && accessorSetMethod==null;}
      
      public boolean isIgnoredInConversion() {return ignoredInConversion;}
      
      public Object getValueFrom(Object entity) {
         Object result=null;
         
         try {
            if (getMethod!=null) result=getMethod.invoke(entity);
            else result=field.get(entity);
         } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalStateException(e);
         }
         
         return result;
      }
      
      public void setValueOn(Object entity, Object value) {
         if (propClass.isPrimitive() && value==null)
            throw new IllegalArgumentException("Cannot set null reference on a primitive " + propClass.getName() + "!");
         if (value!=null && !propClass.isPrimitive() && !propClass.isAssignableFrom(value.getClass()))
            throw new IllegalArgumentException("Assigned value type " + value.getClass().getName() + " is incompatible with property type " + propClass.getName());
         if (isReadOnly()) throw new IllegalStateException("Read-only property");
         try {
            if (setMethod!=null) setMethod.invoke(entity, value);
            else if (accessorSetMethod!=null) {
               Object property=getValueFrom(entity);
               accessorSetMethod.set(value, property);
            }
            else field.set(entity,value);
         } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalStateException(e);
         }
      }
   }
   
   private Map<String, PropertyAccessor> accessibleProps=null;
   
   private Class<T> entityClass=null;

   public static interface TrapSetterAccessor {
      public void set(Object source, Object entity) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;
   }
   
   @SuppressWarnings("unchecked")
   private static void setMap(Object source, Object property) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      Map<Object,Object> propertyRef=(Map<Object,Object>) property;
      propertyRef.clear();
      propertyRef.putAll((Map<Object, Object>) source);
   }      
   
   @SuppressWarnings("unchecked")
   private static void setCollection(Object source, Object property) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      Collection<Object> propertyRef=(Collection<Object>) property;
      propertyRef.clear();
      propertyRef.addAll((Collection<Object>) source);
   }
   
   private EntityPropertiesManager(Class<T> entityClass) {
      if (entityClass.isAnnotation()
       || entityClass.isArray()
       || entityClass.isEnum()
       || entityClass.isSynthetic()
       || entityClass.isPrimitive())
         throw new IllegalStateException("Could not process low-level classes (annotations, arrays, enums, primitives or synthetic)");
      
      this.entityClass=entityClass;
      accessibleProps=new HashMap<String, EntityPropertiesManager<T>.PropertyAccessor>();
      
      for(Method getMethod:entityClass.getMethods()) {
         //per qualificarsi come proprietà un method "get" deve:
         //1) essere pubblico
         //2) non avere argomenti
         //3) restituire qualcosa diverso da void
         //4) avere il nome che inizi per get/is/has
         //5) non essere dichiarato in java.lang.Object
         //rispettate queste condizioni, il metodo verrà associato alla
         //proprietà per nome, togliendo prima il prefisso get/is/has
         //e convertendo il primo carattere del nome rimanente in minuscolo
         
         String getMethodName=getMethod.getName();
         String setMethodName=null;
         Method setMethod=null;
         TrapSetterAccessor accessorSetMethod=null;
         String propName=null;
         String aliasedPropName=null;
         PropertyAccessor propAccess=null;
         boolean ignoreInConversion=false;
         
         if (Modifier.isPublic(getMethod.getModifiers())
               && getMethod.getParameterCount()==0
               && !getMethod.getReturnType().equals(Void.class)
               && !getMethod.getDeclaringClass().equals(Object.class)
               && (getMethodName.startsWith("get") || getMethodName.startsWith("has") || getMethodName.startsWith("is"))
               ) {
            if (getMethodName.startsWith("get") || getMethodName.startsWith("has")) propName=getMethodName.substring(3);
            else if (getMethodName.startsWith("is")) propName=getMethodName.substring(2);
                        
            Class<?> propType=getMethod.getReturnType();
            if (getMethod.isAnnotationPresent(IgnoreProperty.class)) ignoreInConversion=true;
            
            //Se è presente un'annotazione @AliasProperty, il nome specificato utilizzato
            //come nome della proprietà
            AliasProperty aprop=getMethod.getAnnotation(AliasProperty.class);
            if (aprop!=null && aprop.value()!=null && !(aprop.value().isEmpty())) aliasedPropName=aprop.value();
            
            //Il corrispondente metodo "set" deve:
            //1) essere pubblico
            //2) iniziare con "set" + nome della proprietà (con iniziale maiuscola)
            //3) possedere un solo parametro dello stesso tipo del ritorno di "get"
            //4) non possedere l'annotazione JsonIgnore
            setMethodName="set" + propName;
            try {
               setMethod=entityClass.getMethod(setMethodName, propType);
               if (setMethod!=null &&
                     (setMethod.getReturnType().equals(Void.class)
                       || !Modifier.isPublic(setMethod.getModifiers()))
                     ) setMethod=null;

               //if (setMethod!=null && setMethod.isAnnotationPresent(JsonIgnore.class))
               //   ignoreInConversion=true;
               if (setMethod!=null && setMethod.isAnnotationPresent(IgnoreProperty.class))
                  ignoreInConversion=true;
            } catch (NoSuchMethodException ignored) {
               //Manca il metodo "set", ma prima di gettare la spugna vediamo se
               //il tipo concreto implementa "Collection" o "Map", nel qual caso
               //inseriamo un metodo "set" fittizio che provvederà a pulire la Map
               //o la Collection in questione, per aggiungere tutti i valori
               //passati per argomento.
               
               if (Map.class.isAssignableFrom(propType)) {
                  accessorSetMethod=EntityPropertiesManager::setMap;
               } else if (Collection.class.isAssignableFrom(propType)) {
                  accessorSetMethod=EntityPropertiesManager::setCollection;                  
               }
               
               //Se non è nemmeno una Map o una Collection, è una proprietà di sola lettura
            } 
            
            if (aliasedPropName!=null) propName=aliasedPropName;
            else propName=propName.substring(0,1).toLowerCase() + propName.substring(1);
            
            if (setMethod!=null) propAccess=new PropertyAccessor(getMethod, setMethod, ignoreInConversion);
            else if (accessorSetMethod!=null) propAccess=new PropertyAccessor(getMethod, accessorSetMethod, ignoreInConversion);
            else propAccess=new PropertyAccessor(getMethod, ignoreInConversion);
            
            accessibleProps.put(propName, propAccess);
         }
      }
            
      //Cerchiamo tutti i field anche privati, anche nelle classi
      //antenate (tranne Object)
      //Per qualificarsi un field non deve avere lo stesso nome
      //di una proprietà già ricavata via metodi get/set e non
      //possedere l'annotazione IgnoreProperty
      Class<?> scannedClass=entityClass;
      
      while (!(scannedClass.equals(Object.class))) {
         for(Field field:scannedClass.getDeclaredFields()) {
            boolean ignoreInConversion=field.isAnnotationPresent(IgnoreProperty.class);
            
            String fldName=field.getName();
            
            if (!accessibleProps.containsKey(fldName)) {
               field.setAccessible(true); //garantisce l'accesso
               
               PropertyAccessor propAccess=new PropertyAccessor(field, ignoreInConversion);
               accessibleProps.put(fldName, propAccess);
            }
         }         
         scannedClass=scannedClass.getSuperclass();
      }
   }
   
   public Class<T> getEntityClass() {return entityClass;}
   
   public String[] getAllProperties() {
      return accessibleProps.keySet().toArray(new String[accessibleProps.size()]);
   }
   
   public boolean hasProperty(String propName) {
      return accessibleProps.containsKey(propName);
   }
   
   private PropertyAccessor getProp(String propName) {
      if (!hasProperty(propName)) throw new IllegalArgumentException("Property \"" + propName + "\" not defined on " + entityClass.getName());
      return accessibleProps.get(propName);
   }
   
   public Class<?> getPropertyType(String propName) {
      return getProp(propName).getPropertyType();
   }
   
   public boolean isReadOnly(String propName) {
      return getProp(propName).isReadOnly();
   }
   
   public boolean isIgnoredInConversion(String propName) {
      return getProp(propName).isIgnoredInConversion();
   }
   
   @SuppressWarnings("unchecked")
   public <P> P get(String propName, Class<P> returnType, Object sourceObj) {
      return (P) getProp(propName).getValueFrom(sourceObj);
   }
   
   public Object get(String propName, Object sourceObj) {
      return getProp(propName).getValueFrom(sourceObj);
   }
   
   public void set(String propName, Object destObj, Object value) {
      getProp(propName).setValueOn(destObj, value);
   }
}
