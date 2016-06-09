package it.xargon.marshal;

import it.xargon.util.Bitwise;

public class MarPrimShort extends AbstractMarshaller {
   public MarPrimShort() {super("P-SHORT", Source.MEMORY, Short.TYPE);}

   public byte[] marshalToMemory(Object obj) {
      return Bitwise.shortToByteArray(((Short)obj).shortValue());
   }
   
   public Object unmarshalFromMemory(byte[] contents) {
      return Bitwise.byteArrayToShort(contents);
   }
}
