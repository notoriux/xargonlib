package it.xargon.marshal;

import it.xargon.util.Bitwise;

public class MarPrimCharacter extends AbstractMarshaller {
   public MarPrimCharacter() {super("P-CHAR", Source.MEMORY, Character.TYPE);}

   public byte[] marshalToMemory(Object obj) {
      return Bitwise.charToByteArray(((Character)obj).charValue());
   }
   
   public Object unmarshalFromMemory(byte[] contents) {
      return Bitwise.byteArrayToChar(contents);
   }
}
