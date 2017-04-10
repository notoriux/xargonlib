package it.xargon.nioxmp.msg;

import java.nio.ByteBuffer;

import it.xargon.niomarshal.AbstractMarshaller;
import it.xargon.util.Bitwise;
import it.xargon.util.ByteBufferAccumulator;
import it.xargon.util.Identifier;

public class MarXmpSessionKO extends AbstractMarshaller<XmpSessionKO> {
   public MarXmpSessionKO()  {super("XMP40S-KO");}

   public float getAffinity(Class<?> cl) {
      if (cl.equals(XmpSessionInit.class)) return 1f;
      return 0f;
   }
   
   @Override
   public ByteBuffer marshal(XmpSessionKO xmpmessage) {
      ByteBufferAccumulator accumulator=new ByteBufferAccumulator(getAllocator());
      accumulator.addWithByteSize(xmpmessage.getSessionId().getData());
      accumulator.addWithSize(xmpmessage.getReason().getBytes());
      return accumulator.gather();
   }

   @Override
   public XmpSessionKO unmarshal(ByteBuffer buffer) {
      int sidSize=Bitwise.asInt(buffer.get());
      byte[] sidBuf=new byte[sidSize];
      buffer.get(sidBuf);
      Identifier sessionId=new Identifier(sidBuf);
      
      int reasonSize=Bitwise.asInt(buffer.get());
      byte[] reasonBuf=new byte[reasonSize];
      buffer.get(reasonBuf);
      String reason=new String(reasonBuf);
      
      return new XmpSessionKO(sessionId, reason);
   }
   
}
