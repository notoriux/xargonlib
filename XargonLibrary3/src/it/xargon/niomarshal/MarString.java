package it.xargon.niomarshal;

import java.nio.ByteBuffer;

public class MarString extends AbstractMarshaller<String> {
   public MarString() {super("STRING");}

   @Override
   public ByteBuffer marshal(String obj) {
      byte[] tmp=obj.getBytes();
      ByteBuffer result=alloc(tmp.length);
      result.put(tmp).flip();
      return result;
   }

   @Override
   public String unmarshal(ByteBuffer buffer) {
      byte[] tmp=new byte[buffer.remaining()];
      buffer.get(tmp);
      return new String(tmp);
   }
}
