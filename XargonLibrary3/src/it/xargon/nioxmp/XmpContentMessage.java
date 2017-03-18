package it.xargon.nioxmp;

import java.nio.ByteBuffer;
import java.util.Objects;

import it.xargon.util.*;

class XmpContentMessage implements it.xargon.util.Debug.Printable {
   private XmpContentMessageType itype=null;
   private Identifier messageId=null;
   private Identifier sessionId=null;
   private Identifier parentId=null;
   private ByteBuffer contents=null;
   
   public XmpContentMessage(XmpContentMessageType type) {
      itype=type;
      messageId=new Identifier();
      sessionId=new Identifier();
      parentId=new Identifier();
   }
   
   public XmpContentMessage setContents(ByteBuffer src) {
      this.contents=ByteBuffer.allocate(src.remaining()).put(src).asReadOnlyBuffer();
      this.contents.flip();
      return this;
   }
   
   public ByteBuffer getContents() {
      return contents.duplicate().asReadOnlyBuffer();
   }
   
   public XmpContentMessage setMessageId(Identifier msgid) {
      messageId=Objects.requireNonNull(msgid);
      return this;
   }
   
   public Identifier getMessageId() {return messageId;}
   
   public XmpContentMessage setSessionId(Identifier sessionId) {
      this.sessionId=Objects.requireNonNull(sessionId);
      return this;
   }
   
   public Identifier getSessionId() {return sessionId;}

   public XmpContentMessage setParentId(Identifier prnid) {
      parentId=Objects.requireNonNull(prnid);
      return this;
   }
   
   public Identifier getParentId() {return parentId;}

   public XmpContentMessage setType(XmpContentMessageType type) {
      itype=Objects.requireNonNull(type);
      return this;
   }
   
   public XmpContentMessageType getType() {return itype;}
   
   public XmpContentMessage answer() {
      if (itype.equals(XmpContentMessageType.ANSWER) || itype.equals(XmpContentMessageType.EVENT)) 
         throw new IllegalStateException("ANSWERs and EVENTs messages don't imply an answer themselves");
      
      return new XmpContentMessage(XmpContentMessageType.ANSWER).setParentId(messageId);
   }

   public boolean isAnswerFor(XmpContentMessage request) {
      if (!itype.equals(XmpContentMessageType.ANSWER)
            || request.getType().equals(XmpContentMessageType.EVENT)
            || request.getType().equals(XmpContentMessageType.ANSWER)) return false;
      if (!parentId.equals(request.messageId)) return false;      
      return true;
   }

   public String toString(String indent) {
      StringBuilder sb=new StringBuilder();
      sb.append(indent);
      switch (itype) {
         case EVENT:
            sb.append("[").append(sessionId.toString()).append(":").append(messageId.toString()).append("] ").append(" EVENT");
            break;
         case REQUEST:
            sb.append("[").append(sessionId.toString()).append(":").append(messageId.toString()).append("] ").append(" REQUEST");
            break;
         case ANSWER:
            sb.append("[").append(sessionId.toString()).append(":").append(messageId.toString()).append("] ").append(" ANSWER FOR ").append(parentId.toString());
            break;
         case NOSESSION:
            sb.append("[").append(sessionId.toString()).append(":").append(messageId.toString()).append("] ").append(" INVALID SESSION FOR ").append(parentId.toString());
            break;            
         default:
            sb.append("[").append(sessionId.toString()).append(":").append(messageId.toString()).append("] ").append(" (unknown)");
            break;
      }
      sb.append("\n");
      sb.append(Debug.dumpBufferFormatted(indent + indent, contents));
      return sb.toString();
   }
}