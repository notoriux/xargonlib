package it.xargon.niomarshal;

import java.nio.ByteBuffer;

public class MarVoid extends AbstractMarshaller<Void> {
   private ByteBuffer BZERO=null;

   public MarVoid() {super("VOID");}
   
   @Override
   public ByteBuffer marshal(Void obj) {
      if (BZERO==null) BZERO=alloc(0);
      return BZERO;
   }

   @Override
   public Void unmarshal(ByteBuffer buffer) {return null;}
}
