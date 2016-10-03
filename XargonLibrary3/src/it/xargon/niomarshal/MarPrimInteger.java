package it.xargon.niomarshal;

import java.nio.ByteBuffer;

public class MarPrimInteger extends AbstractPrimitiveMarshaller<Integer> {
   public MarPrimInteger() {super("PINT");}

   @Override
   public ByteBuffer marshal(Integer obj) {
      ByteBuffer result=alloc(Integer.BYTES);
      result.putInt(obj.intValue()).flip();
      return result;
   }

   @Override
   public Integer unmarshal(ByteBuffer buffer) {
      return buffer.getInt();
   }
}
