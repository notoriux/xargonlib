package it.xargon.niomarshal;

import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.Objects;

abstract class AbstractMarshaller<T> {
   private String name=null;
   private byte[] encName=null;
   private Class<T> affineClass=null;
   //iniettato dal databridge stesso durante l'installazione del marshaller
   private DataBridge dataBridge=null;

   @SuppressWarnings({"unchecked"})
   protected AbstractMarshaller(String name) {
      this.name=Objects.requireNonNull(name);
      encName=name.getBytes();
      affineClass=((Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
   }
   
   protected ByteBuffer alloc(int size) {return dataBridge.allocate(size);}
   
   protected DataBridge getDataBridge() {return dataBridge;}
   
   public final String getName() {return name;}
   
   final byte[] getEncName() {return encName;}
      
   public Class<?> getAffineClass() {return affineClass;}
         
   public float getAffinity(Class<?> cl) {
      if (cl.equals(affineClass)) return 0.9f;
      if (affineClass.isAssignableFrom(cl)) return 0.8f;
      return 0f;
   }
   
   @SuppressWarnings("unchecked")
   public final ByteBuffer marshalObject(Object obj) {return marshal((T)obj);}
   
   public abstract ByteBuffer marshal(T obj);
   
   public abstract T unmarshal(ByteBuffer buffer);
}
