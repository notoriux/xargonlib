package it.xargon.marshal;

import it.xargon.util.Bitwise;


public class MarPrimByte extends AbstractMarshaller {
   public MarPrimByte() {super("P-BYTE", Source.MEMORY, Byte.TYPE);}

   public byte[] marshalToMemory(Object obj) {
      byte[] result=new byte[1];
      result[0]=Bitwise.asByte(((Byte)obj).byteValue());
      return result;
   }
   
   public Object unmarshalFromMemory(byte[] contents) {
      return contents[0];
   }
}
