package it.xargon.nioxmp;

import java.nio.ByteBuffer;

import it.xargon.niomarshal.AbstractMarshaller;
import it.xargon.util.Bitwise;
import it.xargon.util.ByteBufferAccumulator;
import it.xargon.util.Identifier;

public class MarXmpMessage extends AbstractMarshaller<XmpMessage> {
   public MarXmpMessage()  {super("XMPMSG40");}

   public float getAffinity(Class<?> cl) {
      if (cl.equals(XmpMessageImpl.class)) return 1f;
      if (XmpMessage.class.isAssignableFrom(cl)) return 0.9f;
      return 0f;
   }
   
   @Override
   public ByteBuffer marshal(XmpMessage xmpmessage) {
      XmpMessageImpl xi=(XmpMessageImpl)xmpmessage;
      ByteBufferAccumulator accumulator=new ByteBufferAccumulator(allocator);
      accumulator.add(xi.getType().id()).addWithByteSize(xi.getMessageId().getData());
      if (xi.getType().equals(XmpMessageType.ANSWER)) accumulator.addWithSize(xi.getParentId().getData());
      accumulator.addWithSize(xi.getContents());
      return accumulator.gather();
   }

   @Override
   public XmpMessage unmarshal(ByteBuffer buffer) {
      XmpMessageType mtype=XmpMessageType.getById(buffer.get());
      int idSize=Bitwise.asInt(buffer.get());
      byte[] idBuf=new byte[idSize];
      buffer.get(idBuf);
      Identifier messageId=new Identifier(idBuf);
      Identifier parentId=null;
      if (mtype.equals(XmpMessageType.ANSWER)) {
         int pidSize=Bitwise.asInt(buffer.get());
         byte[] pidBuf=new byte[pidSize];
         buffer.get(pidBuf);
         parentId=new Identifier(pidBuf);
      }
      int contentSize=buffer.getInt();
      ByteBuffer contents=alloc(contentSize);
      contents.put(buffer);
      
      XmpMessageImpl result=new XmpMessageImpl(mtype, contents);
      result.setMessageId(messageId);
      result.setParentId(parentId);
      
      return result;
   }
   
}
