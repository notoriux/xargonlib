package it.xargon.entities;

import java.util.*;
import java.util.function.*;

public class EntityValidator<T> {
   private Class<T> instanceClass;
   private T provided;
   private T original;
   private EntityPropertiesManager<T> propManager=null;
   
   private LinkedList<NamedCondition> allTests=null;
   
   private static class ValidationResultInternal implements EntityValidationResult {
      public static ValidationResultInternal TRUE=new ValidationResultInternal(true);
      
      public static ValidationResultInternal FALSE(String reason) {
         return new ValidationResultInternal(false, reason);
      }
      
      private boolean ok=false;
      private String reason=null;
      
      private ValidationResultInternal(boolean ok) {
         this.ok=ok;
         if (ok) reason="ok";
      }
      
      private ValidationResultInternal(boolean ok, String reason) {
         this.ok=ok;
         this.reason=reason;
      }
      
      public boolean isOk() {return ok;}
      public String getReason() {return reason;}
   }
   
   private static abstract class NamedCondition {
      protected String propName=null;
      public NamedCondition(String propName) {this.propName=propName;}
      public String getPropertyName() {return propName;}
   }
   
   private static abstract class UnaryCondition<T> extends NamedCondition {
      public UnaryCondition(String propName) {super(propName);}
      public abstract ValidationResultInternal test(T t);
   }
   
   private static abstract class BinaryCondition<T> extends NamedCondition {
      public BinaryCondition(String propName) {super(propName);}
      public abstract ValidationResultInternal test(T t, T u);
   }

   private class ConditionNotNull extends UnaryCondition<T> {
      public ConditionNotNull(String propName) {super(propName);}
      @Override
      public ValidationResultInternal test(T t) {
         Object val=propManager.get(propName, t);
         if (val==null) return ValidationResultInternal.FALSE("Property \"" + propName + "\" MUST NOT be null");
         return ValidationResultInternal.TRUE;
      }
   }
   
   private class ConditionRequires extends UnaryCondition<T> {
      public ConditionRequires(String propName) {super(propName);}
      @Override
      public ValidationResultInternal test(T t) {
         Object val=propManager.get(propName, t);
         //Stringa vuota in XML equivale a proprietà non valorizzata
         if ((val==null) || (val instanceof String && ((String)val).isEmpty()))
            return ValidationResultInternal.FALSE("Property \"" + propName + "\" MUST be set");
         return ValidationResultInternal.TRUE;
      }
   }
   
   private class ConditionForbids extends UnaryCondition<T> {
      public ConditionForbids(String propName) {super(propName);}
      @Override
      public ValidationResultInternal test(T t) {
         Object val=propManager.get(propName, t);
         
         if (val!=null) {
            if (val instanceof String && ((String)val).isEmpty()) return ValidationResultInternal.TRUE;
            else return ValidationResultInternal.FALSE("Property \"" + propName + "\" MUST NOT be set");
         }
         
         return ValidationResultInternal.TRUE;
      }
   }
   
   private class ConditionDoesNotAlter extends BinaryCondition<T> {
      public ConditionDoesNotAlter(String propName) {super(propName);}
      @Override
      public ValidationResultInternal test(T t, T u) {
         Object tValue=propManager.get(propName, t);
         
         if (tValue!=null) {
            if (tValue instanceof String && ((String)tValue).isEmpty()) return ValidationResultInternal.TRUE;
            Object uValue=propManager.get(propName, u);
            if (tValue.equals(uValue)) return ValidationResultInternal.TRUE;
            return ValidationResultInternal.FALSE("Property \"" + propName + "\" MUST NOT ALTER the original value");
         }
         
         return ValidationResultInternal.TRUE;
      }
   }
   
   private class GenericAssertion extends UnaryCondition<T> {
      private Predicate<T> predicate=null;
      
      public GenericAssertion(String hint, Predicate<T> predicate) {
         super(hint);
         this.predicate=predicate;
      }
      
      @Override
      public ValidationResultInternal test(T t) {
         if (predicate.test(t)) return ValidationResultInternal.TRUE;
         else return ValidationResultInternal.FALSE("Generic assertion \"" + propName + "\" FAILED");
      }
   }
   
   private class ComparisonAssertion extends BinaryCondition<T> {
      private BiPredicate<T,T> predicate=null;
      
      public ComparisonAssertion(String hint, BiPredicate<T,T> predicate) {
         super(hint);
         this.predicate=predicate;
      }
      
      @Override
      public ValidationResultInternal test(T t,T u) {
         if (predicate.test(t,u)) return ValidationResultInternal.TRUE;
         return ValidationResultInternal.FALSE("Comparison assertion \"" + propName + "\" FAILED");
      }
   }
   
   @SuppressWarnings("unchecked")
   private EntityValidator(T provided) {
      this.provided=provided;      
      this.instanceClass=(Class<T>) this.provided.getClass();
      propManager=EntityPropertiesManager.get(instanceClass);
      allTests=new LinkedList<NamedCondition>();
   }
   
   public static <T> EntityValidator<T> of(T provided) {
      return new EntityValidator<T>(Objects.requireNonNull(provided));
   }

   public EntityValidator<T> with(T original) {
      Objects.requireNonNull(this.provided, "An \"original\" entity is permitted only for comparison with a \"provided\" entity");
      this.original=Objects.requireNonNull(original);
      return this;
   }
   
   private void testProperty(String property) {
      if (!propManager.hasProperty(property))
         throw new IllegalArgumentException("Property \"" + property + "\" not defined on " + instanceClass.getName());
   }
   
   private void testConditions(String property) {
      for(NamedCondition c:allTests) {
         if (c.getPropertyName().equals(property))
            throw new IllegalArgumentException("Property or hint \"" + property + "\" is already used in a " + c.getClass().getName());
      }      
   }
   
   public EntityValidator<T> notNull(String... properties) {
      for(String property:Objects.requireNonNull(properties)) {
         testProperty(property);
         testConditions(property);
         allTests.add(new ConditionNotNull(property));
      }
      return this;
   }
   
   public EntityValidator<T> requires(String... properties) {
      for(String property:Objects.requireNonNull(properties)) {
         testProperty(property);
         testConditions(property);
         allTests.add(new ConditionRequires(property));
      }
      return this;
   }
   
   public EntityValidator<T> requiresEverything() {
      for(String property:propManager.getAllProperties()) {
         testProperty(property);
         testConditions(property);
         allTests.add(new ConditionRequires(property));
      }
      return this;
   }
   
   public EntityValidator<T> forbids(String... properties) {
      for(String property:Objects.requireNonNull(properties)) {
         testProperty(property);
         testConditions(property);
         allTests.add(new ConditionForbids(property));
      }
      return this;
   }

   public EntityValidator<T> doesNotAlter(String... properties) {
      if (original==null)
         throw new IllegalStateException("Cannot perform comparison checks if \"original\" instance hasn't been set");
      for(String property:Objects.requireNonNull(properties)) {
         testProperty(property);
         testConditions(property);
         allTests.add(new ConditionDoesNotAlter(property));
      }
      return this;
   }
   
   public EntityValidator<T> doesNotAlterAnything() {
      if (original==null)
         throw new IllegalStateException("Cannot perform comparison checks if \"original\" instance hasn't been set");
      for(String property:propManager.getAllProperties()) {
         testConditions(property);
         allTests.add(new ConditionDoesNotAlter(property));
      }
      return this;
   }
   
   public EntityValidator<T> asserts(String hint, Predicate<T> predicate) {
      testConditions(hint);
      allTests.add(new GenericAssertion(hint, predicate));
      return this;
   }
   
   public <P> EntityValidator<T> assertsProperty(final String property, final Class<P> propClass, final Predicate<P> predicate) {
      testConditions(property);
      allTests.add(new GenericAssertion(property, entity -> {
         @SuppressWarnings("unchecked")
         P entityProp=(P) propManager.get(property, entity);
         return predicate.test(entityProp);
      }));
      return this;
   }
   
   public EntityValidator<T> asserts(String hint, BiPredicate<T,T> predicate) {
      if (original==null)
         throw new IllegalStateException("Cannot perform comparison checks if \"original\" instance hasn't been set");
      testConditions(hint);
      allTests.add(new ComparisonAssertion(hint, predicate));
      return this;
   }
   
   public <P> EntityValidator<T> assertsProperty(final String property, final Class<P> propClass, final BiPredicate<P,P> predicate) {
      testConditions(property);
      allTests.add(new ComparisonAssertion(property, (provided, original) -> {
         @SuppressWarnings("unchecked")
         P provEntProp=(P) propManager.get(property, provided);
         @SuppressWarnings("unchecked")
         P origEntProp=(P) propManager.get(property, original);
         return predicate.test(provEntProp, origEntProp);
      }));
      return this;
   }
   
   public EntityValidationResult check() {
      if (allTests.size()>0 && original==null) {
         for(NamedCondition cond:allTests) {
            if (cond instanceof BinaryCondition<?>)
               throw new IllegalStateException("Cannot perform comparison checks if original instance hasn't been set");
         }
      }

      return runCheck(provided, original);
   }

   public EntityValidationResult check(T providedInstance) {
      if (provided!=null)
         throw new IllegalArgumentException("This validator isn't generic: has already a provided instance");
      if (allTests.size()>0)
         throw new IllegalStateException("Cannot perform comparison checks if original instance hasn't been provided");
      return runCheck(providedInstance, null);
   }

   public EntityValidationResult check(T providedInstance, T originalInstance) {
      if (provided!=null && original!=null)
         throw new IllegalArgumentException("This validator isn't generic: has already a provided & original instance");
      if (allTests.size()>0 && originalInstance==null)
         throw new IllegalStateException("Cannot perform comparison checks if original instance hasn't been provided");
      return runCheck(providedInstance, originalInstance);
   }
   
   @SuppressWarnings("unchecked")
   private EntityValidationResult runCheck(T prov, T orig) {
      for(NamedCondition c:allTests) {
         ValidationResultInternal result=null;
         if (c instanceof UnaryCondition) {
            result=((UnaryCondition<T>)c).test(prov);
         } else if (c instanceof BinaryCondition) {
            result=((BinaryCondition<T>)c).test(prov,orig);            
         }
         
         if (!result.isOk()) return result;
      }
      
      return ValidationResultInternal.TRUE;
   }
}
