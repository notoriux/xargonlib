package it.xargon.niomarshal;

import java.io.Serializable;
import java.nio.ByteBuffer;

import it.xargon.util.Bitwise;

public class MarSerialized extends AbstractMarshaller<Serializable> {
   public MarSerialized() {super("SER");}
   
   public float getAffinity(Class<?> javaclass) {
      if (Serializable.class.isAssignableFrom(javaclass)) return 0.7f;
      return 0f;
   }

   @Override
   public ByteBuffer marshal(Serializable obj) {
      byte[] tmp=Bitwise.serializeObject(obj);
      ByteBuffer result=alloc(tmp.length);
      result.put(tmp).flip();
      return result;
   }

   @Override
   public Serializable unmarshal(ByteBuffer buffer) {
      byte[] tmp=new byte[buffer.remaining()];
      buffer.get(tmp);
      return Bitwise.deserializeObject(tmp);
   }
}
