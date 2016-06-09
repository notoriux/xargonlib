package it.xargon.marshal;

import it.xargon.util.Bitwise;

public class MarPrimLong extends AbstractMarshaller {
   public MarPrimLong() {super("P-LONG", Source.MEMORY, Long.TYPE);}

   public byte[] marshalToMemory(Object obj) {
      return Bitwise.longToByteArray(((Long)obj).longValue());
   }
   
   public Object unmarshalFromMemory(byte[] contents) {
      return Bitwise.byteArrayToLong(contents);
   }
}
