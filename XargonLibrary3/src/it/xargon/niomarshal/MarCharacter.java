package it.xargon.niomarshal;

import java.nio.ByteBuffer;

public class MarCharacter extends AbstractMarshaller<Character> {
   public MarCharacter() {super("OCHAR");}

   @Override
   public ByteBuffer marshal(Character obj) {
      ByteBuffer result=alloc(Character.BYTES);
      result.putChar(obj.charValue()).flip();
      return result;
   }

   @Override
   public Character unmarshal(ByteBuffer buffer) {
      return new Character(buffer.getChar());
   }
}
