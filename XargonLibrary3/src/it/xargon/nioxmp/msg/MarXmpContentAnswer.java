package it.xargon.nioxmp.msg;

import java.nio.ByteBuffer;

import it.xargon.niomarshal.AbstractMarshaller;
import it.xargon.util.Bitwise;
import it.xargon.util.ByteBufferAccumulator;
import it.xargon.util.Identifier;

public class MarXmpContentAnswer extends AbstractMarshaller<XmpContentAnswer> {
   public MarXmpContentAnswer()  {super("XMP40C-ANS");}

   public float getAffinity(Class<?> cl) {
      if (cl.equals(XmpContentAnswer.class)) return 1f;
      return 0f;
   }
   
   @Override
   public ByteBuffer marshal(XmpContentAnswer xmpmessage) {
      ByteBufferAccumulator accumulator=new ByteBufferAccumulator(getAllocator());;
      //Session ID
      accumulator.addWithByteSize(xmpmessage.getSessionId().getData());
      //Message ID
      accumulator.addWithByteSize(xmpmessage.getMessageId().getData());
      //Parent ID
      accumulator.addWithSize(xmpmessage.getParentId().getData());
      //Finally, the contents.
      accumulator.addWithSize(xmpmessage.getContents());
      return accumulator.gather();
   }

   @Override
   public XmpContentAnswer unmarshal(ByteBuffer buffer) {
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
      
      //Parent ID
      int pidSize=Bitwise.asInt(buffer.get());
      byte[] pidBuf=new byte[pidSize];
      buffer.get(pidBuf);
      Identifier parentId=new Identifier(pidBuf);
      
      //Contents
      int contentSize=buffer.getInt();
      ByteBuffer contents=alloc(contentSize);
      contents.put(buffer);
      
      return new XmpContentAnswer(sessionId, messageId, parentId, contents);
   }
   
}
