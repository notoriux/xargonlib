package it.xargon.niomarshal;

import java.nio.ByteBuffer;

public class MarInteger extends AbstractMarshaller<Integer> {
   public MarInteger() {super("OINT");}

   @Override
   public ByteBuffer marshal(Integer obj) {
      ByteBuffer result=alloc(Integer.BYTES);
      result.putInt(obj.intValue()).flip();
      return result;
   }

   @Override
   public Integer unmarshal(ByteBuffer buffer) {
      return new Integer(buffer.getInt());
   }
}
