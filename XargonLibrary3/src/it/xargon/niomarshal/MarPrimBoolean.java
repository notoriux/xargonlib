package it.xargon.niomarshal;

import java.nio.ByteBuffer;

import it.xargon.util.Bitwise;

public class MarPrimBoolean extends AbstractPrimitiveMarshaller<Boolean> {
   public MarPrimBoolean() {super("PBOOL");}
   
   private final static byte BTRUE=Bitwise.asByte(0xFF);
   private final static byte BFALSE=Bitwise.asByte(0x00);

   @Override
   public ByteBuffer marshal(Boolean obj) {
      ByteBuffer result=alloc(1);
      result.put(obj.booleanValue()?BTRUE:BFALSE).flip();
      return result;
   }

   @Override
   public Boolean unmarshal(ByteBuffer buffer) {
      return buffer.get()==BTRUE;
   }
}
