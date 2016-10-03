package it.xargon.niomarshal;

import java.nio.ByteBuffer;

public class MarByteArray extends AbstractMarshaller<byte[]> {
   public MarByteArray() {super("BYTEARRAY");}
   
   public float getAffinity(Class<?> javaClass) {
      if (javaClass.isArray() && Byte.TYPE.isAssignableFrom(javaClass.getComponentType()))
         return 1f;
      return 0f;
   }
   
   public byte[] marshalToMemory(Object obj) {return (byte[])obj;}
   
   public Object unmarshalFromMemory(byte[] contents) {return contents;}

   @Override
   public ByteBuffer marshal(byte[] obj) {
      ByteBuffer result=alloc(((byte[])obj).length);
      result.put(obj).flip();
      return result;
   }

   @Override
   public byte[] unmarshal(ByteBuffer buffer) {
      byte[] result=new byte[buffer.remaining()];
      buffer.get(result);
      return result;
   }
}
