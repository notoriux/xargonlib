package it.xargon.marshal;

import it.xargon.util.Bitwise;

public class MarPrimInteger extends AbstractMarshaller {
   public MarPrimInteger() {super("P-INT", Source.MEMORY, Integer.TYPE);}

   public byte[] marshalToMemory(Object obj) {
      return Bitwise.intToByteArray(((Integer)obj).intValue());
   }
   
   public Object unmarshalFromMemory(byte[] contents) {
      return Bitwise.byteArrayToInt(contents);
   }
}
