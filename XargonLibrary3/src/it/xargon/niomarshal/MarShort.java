package it.xargon.niomarshal;

import java.nio.ByteBuffer;

public class MarShort extends AbstractMarshaller<Short> {
   public MarShort() {super("OSHORT");}

   @Override
   public ByteBuffer marshal(Short obj) {
      ByteBuffer result=alloc(Short.BYTES);
      result.putShort(obj.shortValue()).flip();
      return result;
   }

   @Override
   public Short unmarshal(ByteBuffer buffer) {
      return new Short(buffer.getShort());
   }
}
