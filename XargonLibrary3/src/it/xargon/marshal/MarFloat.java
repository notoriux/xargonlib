package it.xargon.marshal;

import it.xargon.util.Bitwise;

public class MarFloat extends AbstractMarshaller {
   public MarFloat() {super("FLOAT", Source.MEMORY, Float.class);}

   public byte[] marshalToMemory(Object obj) {
      return Bitwise.floatToByteArray(((Float)obj).floatValue());
   }
   
   public Object unmarshalFromMemory(byte[] contents) {
      return new Float(Bitwise.byteArrayToFloat(contents));
   }
}
