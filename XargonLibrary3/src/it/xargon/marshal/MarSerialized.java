package it.xargon.marshal;

import java.io.Serializable;

import it.xargon.util.Bitwise;

public class MarSerialized extends AbstractMarshaller {
   public MarSerialized() {super("SER", Source.MEMORY, Serializable.class);}
   
   public float getAffinity(Class<?> javaclass) {
      if (Serializable.class.isAssignableFrom(javaclass)) return 0.7f;
      return 0f;
   }
   
   public byte[] marshalToMemory(Object obj) {
      return Bitwise.serializeObject(obj);
   }
   
   public Object unmarshalFromMemory(byte[] contents) {
      return Bitwise.deserializeObject(contents);
   }
}
