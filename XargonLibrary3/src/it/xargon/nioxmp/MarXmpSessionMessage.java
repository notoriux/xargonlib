package it.xargon.nioxmp;

import java.nio.ByteBuffer;

import it.xargon.niomarshal.AbstractMarshaller;
import it.xargon.util.Bitwise;
import it.xargon.util.ByteBufferAccumulator;
import it.xargon.util.Identifier;

public class MarXmpSessionMessage extends AbstractMarshaller<XmpSessionMessage> {
   public MarXmpSessionMessage()  {super("XMPSMSG40");}

   public float getAffinity(Class<?> cl) {
      if (cl.equals(XmpSessionMessage.class)) return 1f;
      return 0f;
   }
   
   @Override
   public ByteBuffer marshal(XmpSessionMessage xmpmessage) {
      ByteBufferAccumulator accumulator=new ByteBufferAccumulator(allocator);
      //Message type
      accumulator.add(xmpmessage.getType().id());
      //Session ID (only if message type different from "INIT")
      if (!xmpmessage.getType().equals(XmpSessionMessageType.INIT))
         accumulator.addWithByteSize(xmpmessage.getSessionId().getData());
      return accumulator.gather();
   }

   @Override
   public XmpSessionMessage unmarshal(ByteBuffer buffer) {
      //Message type
      XmpSessionMessageType mtype=XmpSessionMessageType.getById(buffer.get());
      
      //Session ID (if present)
      Identifier sessionId=new Identifier();
      if (!mtype.equals(XmpSessionMessageType.INIT)) {
         int sidSize=Bitwise.asInt(buffer.get());
         byte[] sidBuf=new byte[sidSize];
         buffer.get(sidBuf);
         sessionId=new Identifier(sidBuf);
      }
      
      return new XmpSessionMessage(mtype)
            .setSessionId(sessionId);
   }
   
}
