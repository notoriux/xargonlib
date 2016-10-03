package it.xargon.niomarshal;

import java.nio.ByteBuffer;

public class MarPrimLong extends AbstractPrimitiveMarshaller<Long> {
   public MarPrimLong() {super("PLONG");}

   @Override
   public ByteBuffer marshal(Long obj) {
      ByteBuffer result=alloc(Long.BYTES);
      result.putLong(obj.longValue()).flip();
      return result;
   }

   @Override
   public Long unmarshal(ByteBuffer buffer) {
      return buffer.getLong();
   }
}
