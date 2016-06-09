package it.xargon.marshal;

public class MarVoid extends AbstractMarshaller {
   public MarVoid() {super("VOID", Source.MEMORY, Void.class);}
   
   private static byte[] bzero=new byte[0];

   public byte[] marshalToMemory(Object obj) {return bzero;}
   
   public Object unmarshalFromMemory(byte[] contents) {return null;}
}
