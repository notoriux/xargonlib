package it.xargon.niomarshal;

import java.nio.ByteBuffer;

public class MarPrimFloat extends AbstractPrimitiveMarshaller<Float> {
   public MarPrimFloat() {super("PFLOAT");}

   @Override
   public ByteBuffer marshal(Float obj) {
      ByteBuffer result=alloc(Float.BYTES);
      result.putFloat(obj.floatValue()).flip();
      return result;
   }

   @Override
   public Float unmarshal(ByteBuffer buffer) {
      return buffer.getFloat();
   }
}
