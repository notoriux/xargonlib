package it.xargon.marshal;

public class MarPrimByteArray extends AbstractMarshaller {
   public MarPrimByteArray() {super("P-BYTEARRAY", Source.MEMORY, Object.class);}
   
   public float getAffinity(Class<?> javaClass) {
      if (javaClass.isArray() && Byte.TYPE.isAssignableFrom(javaClass.getComponentType()))
         return 1f;
      return 0f;
   }
   
   public byte[] marshalToMemory(Object obj) {return (byte[])obj;}
   
   public Object unmarshalFromMemory(byte[] contents) {return contents;}
}
