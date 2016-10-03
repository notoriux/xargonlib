package it.xargon.niomarshal;

import java.nio.ByteBuffer;

public class MarPrimCharacter extends AbstractPrimitiveMarshaller<Character> {
   public MarPrimCharacter() {super("PCHAR");}

   @Override
   public ByteBuffer marshal(Character obj) {
      ByteBuffer result=alloc(Character.BYTES);
      result.putChar(obj.charValue()).flip();
      return result;
   }

   @Override
   public Character unmarshal(ByteBuffer buffer) {
      return buffer.getChar();
   }
}
