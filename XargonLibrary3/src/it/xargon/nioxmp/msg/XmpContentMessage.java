package it.xargon.nioxmp.msg;

import java.nio.ByteBuffer;
import java.util.Objects;

import it.xargon.util.Debug;
import it.xargon.util.Identifier;
import it.xargon.util.Debug.Printable;

public abstract class XmpContentMessage implements Printable {
   protected Identifier messageId=null;
   protected Identifier sessionId=null;
   protected ByteBuffer contents=null;
   
   public XmpContentMessage(Identifier sessionId, Identifier messageId, ByteBuffer contents) {
      this.sessionId=Objects.requireNonNull(sessionId);
      this.messageId=Objects.requireNonNull(messageId);
      this.contents=Objects.requireNonNull(contents);
   }
   
   public Identifier getSessionId() {return sessionId;}

   public Identifier getMessageId() {return messageId;}
   
   public ByteBuffer getContents() {
      return contents.duplicate().asReadOnlyBuffer();
   }
   
   protected abstract void addDetails(StringBuilder sb);
   
   @Override
   public final String toString(String indent) {
      StringBuilder sb=new StringBuilder();
      sb.append(indent);
      sb.append("[").append(sessionId.toString()).append(":").append(messageId.toString()).append("] ");
      addDetails(sb);
      sb.append("\n");
      sb.append(Debug.dumpBufferFormatted(indent + indent, contents));
      return sb.toString();
   }
}
