package it.xargon.nioxmp;

import java.nio.ByteBuffer;

import it.xargon.niomarshal.AbstractMarshaller;
import it.xargon.util.Bitwise;
import it.xargon.util.ByteBufferAccumulator;
import it.xargon.util.Identifier;

public class MarXmpContentMessage extends AbstractMarshaller<XmpContentMessage> {
   public MarXmpContentMessage()  {super("XMPCMSG40");}

   public float getAffinity(Class<?> cl) {
      if (cl.equals(XmpContentMessage.class)) return 1f;
      return 0f;
   }
   
   @Override
   public ByteBuffer marshal(XmpContentMessage xmpmessage) {
      ByteBufferAccumulator accumulator=new ByteBufferAccumulator(allocator);
      //Message type
      accumulator.add(xmpmessage.getType().id());
      //Session ID
      accumulator.addWithByteSize(xmpmessage.getSessionId().getData());
      //Message ID
      accumulator.addWithByteSize(xmpmessage.getMessageId().getData());
      //Only answers have Parent ID
      if (xmpmessage.getType().equals(XmpContentMessageType.ANSWER)) accumulator.addWithSize(xmpmessage.getParentId().getData());
      //Finally, the contents.
      accumulator.addWithSize(xmpmessage.getContents());
      return accumulator.gather();
   }

   @Override
   public XmpContentMessage unmarshal(ByteBuffer buffer) {
      //Message type
      XmpContentMessageType mtype=XmpContentMessageType.getById(buffer.get());
      
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
      
      //Parent ID (if present)
      Identifier parentId=new Identifier();
      if (mtype.equals(XmpContentMessageType.ANSWER)) {
         int pidSize=Bitwise.asInt(buffer.get());
         byte[] pidBuf=new byte[pidSize];
         buffer.get(pidBuf);
         parentId=new Identifier(pidBuf);
      }
      
      //Contents
      int contentSize=buffer.getInt();
      ByteBuffer contents=alloc(contentSize);
      contents.put(buffer);
      
      return new XmpContentMessage(mtype)
            .setSessionId(sessionId)
            .setMessageId(messageId)
            .setParentId(parentId)
            .setContents(contents);
   }
   
}
