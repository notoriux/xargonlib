package it.xargon.nioxmp.msg;

import java.nio.ByteBuffer;
import java.util.Objects;

import it.xargon.util.Debug;
import it.xargon.util.Identifier;

public abstract class XmpContentMessage extends XmpMessage {
   protected Identifier messageId=null;
   protected ByteBuffer contents=null;
   
   public XmpContentMessage(Identifier sessionId, Identifier messageId, ByteBuffer contents) {
      super(sessionId);
      this.messageId=Objects.requireNonNull(messageId);
      this.contents=Objects.requireNonNull(contents);
   }

   public Identifier getMessageId() {return messageId;}
   
   public ByteBuffer getContents() {
      return contents.duplicate().asReadOnlyBuffer();
   }
   
   protected abstract void addDetails(StringBuilder sb);
   
   @Override
   public String toString(String indent) {
      StringBuilder sb=new StringBuilder();
      sb.append(indent);
      sb.append("[").append(sessionId.toString()).append(":").append(messageId.toString()).append("] ");
      addDetails(sb);
      sb.append("\n");
      sb.append(Debug.dumpBufferFormatted(indent + indent, contents));
      return sb.toString();
   }
}
