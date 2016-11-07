package it.xargon.nioxmp;

import java.nio.ByteBuffer;
import java.util.Objects;

import it.xargon.util.*;

class XmpMessage implements it.xargon.util.Debug.Printable {
   private XmpMessageType itype=null;
   private Identifier messageId=null;
   private Identifier sessionId=null;
   private Identifier parentId=null;
   private ByteBuffer contents=null;
   private long timestamp=0;
   
   public XmpMessage(XmpMessageType type) {
      itype=type;
      messageId=new Identifier();
      sessionId=new Identifier();
      parentId=new Identifier();
   }
   
   public XmpMessage setContents(ByteBuffer src) {
      this.contents=ByteBuffer.allocate(src.remaining()).put(src).asReadOnlyBuffer();
      this.contents.flip();
      return this;
   }
   
   public ByteBuffer getContents() {
      return contents.duplicate().asReadOnlyBuffer();
   }
   
   public XmpMessage setMessageId(Identifier msgid) {
      messageId=Objects.requireNonNull(msgid);
      timestamp=System.currentTimeMillis();
      return this;
   }
   
   public Identifier getMessageId() {return messageId;}
   
   public long getTimestamp() {return timestamp;}

   public XmpMessage setSessionId(Identifier sessionId) {
      this.sessionId=Objects.requireNonNull(sessionId);
      return this;
   }
   
   public Identifier getSessionId() {return sessionId;}

   public XmpMessage setParentId(Identifier prnid) {
      parentId=Objects.requireNonNull(prnid);
      return this;
   }
   
   public Identifier getParentId() {return parentId;}

   public XmpMessage setType(XmpMessageType type) {
      itype=Objects.requireNonNull(type);
      return this;
   }
   
   public XmpMessageType getType() {return itype;}
   
   public XmpMessage answer() {
      if (itype.equals(XmpMessageType.ANSWER) || itype.equals(XmpMessageType.EVENT)) 
         throw new IllegalStateException("ANSWERs and EVENTs messages don't imply an answer themselves");
      
      return new XmpMessage(XmpMessageType.ANSWER).setParentId(messageId);
   }

   public boolean isAnswerFor(XmpMessage request) {
      if (!itype.equals(XmpMessageType.ANSWER)
            || request.getType().equals(XmpMessageType.EVENT)
            || request.getType().equals(XmpMessageType.ANSWER)) return false;
      if (!parentId.equals(request.messageId)) return false;      
      return true;
   }

   public String toString(String indent) {
      StringBuilder sb=new StringBuilder();
      sb.append(indent);
      switch (itype) {
         case EVENT:
            sb.append("[").append(messageId.toString()).append("] ").append(" EVENT");
            break;
         case REQUEST:
            sb.append("[").append(messageId.toString()).append("] ").append(" REQUEST");
            break;
         case ANSWER:
            sb.append("[").append(messageId.toString()).append("] ").append(" ANSWER FOR ").append(parentId.toString());
            break;
         case NEWSESSION:
            sb.append("[").append(messageId.toString()).append("] ").append(" NEW SESSION REQUEST");
         case ENDSESSION:
            sb.append("[").append(messageId.toString()).append("] ").append(" ENDING SESSION ").append(sessionId.toString());
            break;
         case RESTORE:
            sb.append("[").append(messageId.toString()).append("] ").append(" RESTORE SESSION ").append(sessionId.toString());
            break;
         default:
            sb.append("[").append(messageId.toString()).append("] ").append(" (unknown)");
            break;
      }
      sb.append("\n");
      sb.append(Debug.dumpBufferFormatted(indent + indent, contents));
      return sb.toString();
   }
}