package it.xargon.nioxmp;

import java.io.*;
import java.nio.ByteBuffer;
import it.xargon.util.*;

class XmpMessageImpl implements XmpMessage {
   private XmpMessageType itype=null;
   private Identifier messageId=null;
   private Identifier parentId=null;
   private ByteBuffer contents=null;
   private long timestamp=0;
   
   public XmpMessageImpl(XmpMessageType type) {this(type, null);}
   public XmpMessageImpl(XmpMessageType type, ByteBuffer src) {
      itype=type;
      setContents(src);
      messageId=new Identifier();
      parentId=new Identifier();
   }
   
   public void setContents(ByteBuffer src) {
      this.contents=ByteBuffer.allocate(src.remaining()).put(src).asReadOnlyBuffer();
      this.contents.flip();
   }
   
   public ByteBuffer getContents() {
      return contents.duplicate().asReadOnlyBuffer();
   }
   
   public void setMessageId(Identifier msgid) {messageId=msgid;timestamp=System.currentTimeMillis();}
   public Identifier getMessageId() {return messageId;}
   
   public long getTimestamp() {return timestamp;}

   public void setParentId(Identifier prnid) {parentId=prnid;}
   public Identifier getParentId() {return parentId;}

   public void setType(XmpMessageType type) {itype=type;}
   public XmpMessageType getType() { return itype;}
   
   public XmpMessageImpl createAnswer() {
      if (itype!=XmpMessageType.REQUEST) return null;
      XmpMessageImpl answ=new XmpMessageImpl(XmpMessageType.ANSWER);
      answ.setParentId(messageId);
      return answ;
   }

   public boolean isChildOf(XmpMessage request) {
      if (!(request instanceof XmpMessageImpl)) return false;
      XmpMessageImpl irequest=XmpMessageImpl.class.cast(request);
      if ((itype!=XmpMessageType.ANSWER) || (request.getType()!=XmpMessageType.REQUEST)) return false;
      if (!parentId.equals(irequest.messageId)) return false;      
      return true;
   }

   public void printout(String indent, PrintWriter out) {
      out.print(indent);
      switch (itype) {
         case EVENT:
            out.print("EVENT ");
            out.print(messageId.toString());
            break;
         case REQUEST:
            out.print("REQUEST ");
            out.print(messageId.toString());
            break;
         case ANSWER:
            out.print("ANSWER ");
            out.print(messageId.toString());
            out.print(" FOR REQUEST ");
            out.print(parentId.toString());
            break;
         case CLOSING:
            out.print("CLOSING ");
            out.print(messageId.toString());
            break;
         default:
            out.print("(unknown) ");
            out.print(messageId.toString());
            break;
      }
      out.println();
      Debug.dumpBufferFormatted(indent + "  ", contents, out);
   }
}