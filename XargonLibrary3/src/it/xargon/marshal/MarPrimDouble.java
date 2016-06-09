package it.xargon.marshal;

import it.xargon.util.Bitwise;

public class MarPrimDouble extends AbstractMarshaller {
   public MarPrimDouble() {super("P-DOUBLE", Source.MEMORY, Double.TYPE);}

   public byte[] marshalToMemory(Object obj) {
      return Bitwise.doubleToByteArray(((Double)obj).doubleValue());
   }
   
   public Object unmarshalFromMemory(byte[] contents) {
      return Bitwise.byteArrayToDouble(contents);
   }
}
