package it.xargon.niomarshal;

import java.nio.ByteBuffer;

public class MarPrimShort extends AbstractPrimitiveMarshaller<Short> {
   public MarPrimShort() {super("PSHORT");}

   @Override
   public ByteBuffer marshal(Short obj) {
      ByteBuffer result=alloc(Short.BYTES);
      result.putShort(obj.shortValue()).flip();
      return result;
   }

   @Override
   public Short unmarshal(ByteBuffer buffer) {
      return buffer.getShort();
   }
}
