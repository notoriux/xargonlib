package it.xargon.nioxmp.msg;

import java.nio.ByteBuffer;

import it.xargon.niomarshal.AbstractMarshaller;
import it.xargon.util.Bitwise;
import it.xargon.util.ByteBufferAccumulator;
import it.xargon.util.Identifier;

public class MarXmpContentRequest extends AbstractMarshaller<XmpContentRequest> {
   public MarXmpContentRequest()  {super("XMP40C-REQ");}

   public float getAffinity(Class<?> cl) {
      if (cl.equals(XmpContentRequest.class)) return 1f;
      return 0f;
   }
   
   @Override
   public ByteBuffer marshal(XmpContentRequest xmpmessage) {
      ByteBufferAccumulator accumulator=new ByteBufferAccumulator(allocator);
      //Session ID
      accumulator.addWithByteSize(xmpmessage.getSessionId().getData());
      //Message ID
      accumulator.addWithByteSize(xmpmessage.getMessageId().getData());
      //Finally, the contents.
      accumulator.addWithSize(xmpmessage.getContents());
      return accumulator.gather();
   }

   @Override
   public XmpContentRequest unmarshal(ByteBuffer buffer) {
      //Session ID
      int sidSize=Bitwise.asInt(buffer.get());
      byte[] sidBuf=new byte[sidSize];
      buffer.get(sidBuf);
      Identifier sessionId=new Identifier(sidBuf);
      
      //Message ID
      int midSize=Bitwise.asInt(buffer.get());
      byte[] midBuf=new byte[midSize];
      buffer.get(midBuf);
      Identifier messageId=new Identifier(midBuf);
      
      //Contents
      int contentSize=buffer.getInt();
      ByteBuffer contents=alloc(contentSize);
      contents.put(buffer);
      
      return new XmpContentRequest(sessionId, messageId, contents);
   }
}
