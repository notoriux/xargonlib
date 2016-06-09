package it.xargon.marshal;

import it.xargon.util.Bitwise;

public class MarDouble extends AbstractMarshaller {
   public MarDouble() {super("DOUBLE", Source.MEMORY, Double.class);}

   public byte[] marshalToMemory(Object obj) {
      return Bitwise.doubleToByteArray(((Double)obj).doubleValue());
   }
   
   public Object unmarshalFromMemory(byte[] contents) {
      return new Double(Bitwise.byteArrayToDouble(contents));
   }
}
