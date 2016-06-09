package it.xargon.marshal;

import it.xargon.util.Bitwise;

public class MarPrimFloat extends AbstractMarshaller {
   public MarPrimFloat() {super("P-FLOAT", Source.MEMORY, Float.TYPE);}

   public byte[] marshalToMemory(Object obj) {
      return Bitwise.floatToByteArray(((Float)obj).floatValue());
   }
   
   public Object unmarshalFromMemory(byte[] contents) {
      return Bitwise.byteArrayToFloat(contents);
   }
}
