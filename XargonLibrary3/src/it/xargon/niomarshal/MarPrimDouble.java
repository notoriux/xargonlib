package it.xargon.niomarshal;

import java.nio.ByteBuffer;

public class MarPrimDouble extends AbstractPrimitiveMarshaller<Double> {
   public MarPrimDouble() {super("PDOUBLE");}

   @Override
   public ByteBuffer marshal(Double obj) {
      ByteBuffer result=alloc(Double.BYTES);
      result.putDouble(obj.doubleValue()).flip();
      return result;
   }

   @Override
   public Double unmarshal(ByteBuffer buffer) {
      return buffer.getDouble();
   }
}
