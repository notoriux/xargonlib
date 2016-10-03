package it.xargon.niomarshal;

import java.nio.ByteBuffer;

public class MarFloat extends AbstractMarshaller<Float> {
   public MarFloat() {super("OFLOAT");}

   @Override
   public ByteBuffer marshal(Float obj) {
      ByteBuffer result=alloc(Float.BYTES);
      result.putFloat(obj.floatValue()).flip();
      return result;
   }

   @Override
   public Float unmarshal(ByteBuffer buffer) {
      return new Float(buffer.getFloat());
   }
}
