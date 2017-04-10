package it.xargon.nioxmp.msg;

import java.nio.ByteBuffer;

import it.xargon.niomarshal.AbstractMarshaller;
import it.xargon.util.Bitwise;
import it.xargon.util.ByteBufferAccumulator;
import it.xargon.util.Identifier;

public class MarXmpSessionClose extends AbstractMarshaller<XmpSessionClose> {
   public MarXmpSessionClose()  {super("XMP40S-CLS");}

   public float getAffinity(Class<?> cl) {
      if (cl.equals(XmpSessionInit.class)) return 1f;
      return 0f;
   }
   
   @Override
   public ByteBuffer marshal(XmpSessionClose xmpmessage) {
      ByteBufferAccumulator accumulator=new ByteBufferAccumulator(getAllocator());
      accumulator.addWithByteSize(xmpmessage.getSessionId().getData());
      return accumulator.gather();
   }

   @Override
   public XmpSessionClose unmarshal(ByteBuffer buffer) {
      int sidSize=Bitwise.asInt(buffer.get());
      byte[] sidBuf=new byte[sidSize];
      buffer.get(sidBuf);
      Identifier sessionId=new Identifier(sidBuf);
      
      return new XmpSessionClose(sessionId);
   }
   
}
