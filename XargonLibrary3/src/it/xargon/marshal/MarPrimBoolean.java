package it.xargon.marshal;

import it.xargon.util.Bitwise;

public class MarPrimBoolean extends AbstractMarshaller {
   public MarPrimBoolean() {super("P-BOOLEAN", Source.MEMORY, Boolean.TYPE);}

   public byte[] marshalToMemory(Object obj) {
      byte[] result=new byte[1];
      result[0]=Bitwise.asByte(((Boolean)obj).booleanValue()?0xFF:0x00);
      return result;
   }
   
   public Object unmarshalFromMemory(byte[] contents) {
      if (Bitwise.asInt(contents[0])==0x00) return false;
      return true;
   }
}
