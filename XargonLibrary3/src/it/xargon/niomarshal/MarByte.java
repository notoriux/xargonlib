package it.xargon.niomarshal;

import java.nio.ByteBuffer;

public class MarByte extends AbstractMarshaller<Byte> {
   public MarByte() {super("OBYTE");}

   @Override
   public ByteBuffer marshal(Byte obj) {
      ByteBuffer result=alloc(Byte.BYTES);
      result.put(obj.byteValue()).flip();
      return result;
   }

   @Override
   public Byte unmarshal(ByteBuffer buffer) {
      return new Byte(buffer.get());
   }
}
