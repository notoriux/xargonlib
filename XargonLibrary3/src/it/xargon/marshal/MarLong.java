package it.xargon.marshal;

import it.xargon.util.Bitwise;

public class MarLong extends AbstractMarshaller {
   public MarLong() {super("LONG", Source.MEMORY, Long.class);}

   public byte[] marshalToMemory(Object obj) {
      return Bitwise.longToByteArray(((Long)obj).longValue());
   }
   
   public Object unmarshalFromMemory(byte[] contents) {
      return new Long(Bitwise.byteArrayToLong(contents));
   }
}
