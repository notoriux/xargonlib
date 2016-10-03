package it.xargon.niomarshal;

import java.nio.ByteBuffer;

public class MarLong extends AbstractMarshaller<Long> {
   public MarLong() {super("OLONG");}

   @Override
   public ByteBuffer marshal(Long obj) {
      ByteBuffer result=alloc(Long.BYTES);
      result.putLong(obj.longValue()).flip();
      return result;
   }

   @Override
   public Long unmarshal(ByteBuffer buffer) {
      return new Long(buffer.getLong());
   }
}
