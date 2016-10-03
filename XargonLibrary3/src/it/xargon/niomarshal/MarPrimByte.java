package it.xargon.niomarshal;

import java.nio.ByteBuffer;

public class MarPrimByte extends AbstractPrimitiveMarshaller<Byte> {
   public MarPrimByte() {super("PBYTE");}

   @Override
   public ByteBuffer marshal(Byte obj) {
      ByteBuffer result=alloc(Byte.BYTES);
      result.put(obj.byteValue()).flip();
      return result;
   }

   @Override
   public Byte unmarshal(ByteBuffer buffer) {
      return buffer.get();
   }
}
