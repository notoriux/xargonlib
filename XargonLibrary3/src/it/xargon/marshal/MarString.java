package it.xargon.marshal;

import it.xargon.util.Bitwise;

public class MarString extends AbstractMarshaller {
   public MarString() {super("STRING", Source.MEMORY, String.class);}
      
   public byte[] marshalToMemory(Object obj) {
      return Bitwise.stringToByteArray((String)obj);
   }
   
   public Object unmarshalFromMemory(byte[] contents) {
      return Bitwise.byteArrayToString(contents);
   }
}
