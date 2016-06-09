package it.xargon.marshal;

import it.xargon.util.Bitwise;

public class MarByte extends AbstractMarshaller {
   public MarByte() {super("BYTE", Source.MEMORY, Byte.class);}

   public byte[] marshalToMemory(Object obj) {
      byte[] result=new byte[1];
      result[0]=Bitwise.asByte(((Byte)obj).byteValue());
      return result;
   }
   
   public Object unmarshalFromMemory(byte[] contents) {
      return new Byte(contents[0]);
   }
}
