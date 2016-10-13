package it.xargon.niomarshal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public abstract class AbstractPrimitiveMarshaller<T> extends AbstractMarshaller<T> {
   private Class<?> primClass=null;
   private Class<?> affineClass=null;
   
   public AbstractPrimitiveMarshaller(String name) {
      super(name);
      try {
         affineClass=super.getAffineClass();
         Field primTypeAccessor = affineClass.getField("TYPE");
         if (!Modifier.isStatic(primTypeAccessor.getModifiers()))
            throw new IllegalStateException(affineClass.getName() + " \"TYPE\" field isn't static");
         if (!primTypeAccessor.getType().equals(Class.class))
            throw new IllegalStateException(affineClass.getName() + " \"TYPE\" field isn't a Class reference");
         primClass=(Class<?>) primTypeAccessor.get(null);
         if (!primClass.isPrimitive())
            throw new IllegalStateException(affineClass.getName() + " \"TYPE\" field isn't a primitive Class reference");
      } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
         throw new IllegalStateException("Unable to detect primitive type for " + super.getAffineClass().getName(), e);
      }
   }
   
   public Class<?> getAffineClass() {return primClass;}
   
   public float getAffinity(Class<?> cl) {
      if (cl.equals(primClass)) return 1.0f;
      return super.getAffinity(cl);
   }
}
