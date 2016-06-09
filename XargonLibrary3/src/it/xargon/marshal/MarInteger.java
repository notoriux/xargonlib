package it.xargon.marshal;

import it.xargon.util.Bitwise;

public class MarInteger extends AbstractMarshaller {
   public MarInteger() {super("INT", Source.MEMORY, Integer.class);}

   public byte[] marshalToMemory(Object obj) {
      return Bitwise.intToByteArray(((Integer)obj).intValue());
   }
   
   public Object unmarshalFromMemory(byte[] contents) {
      return new Integer(Bitwise.byteArrayToInt(contents));
   }
}
