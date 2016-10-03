package it.xargon.niomarshal;

import java.nio.ByteBuffer;

import it.xargon.util.Bitwise;

public class MarDouble extends AbstractMarshaller<Double> {
   public MarDouble() {super("ODOUBLE");}

   public byte[] marshalToMemory(Object obj) {
      return Bitwise.doubleToByteArray(((Double)obj).doubleValue());
   }
   
   public Object unmarshalFromMemory(byte[] contents) {
      return new Double(Bitwise.byteArrayToDouble(contents));
   }

   @Override
   public ByteBuffer marshal(Double obj) {
      ByteBuffer result=alloc(Double.BYTES);
      result.putDouble(obj.doubleValue()).flip();
      return result;
   }

   @Override
   public Double unmarshal(ByteBuffer buffer) {
      return new Double(buffer.getDouble());
   }
}
