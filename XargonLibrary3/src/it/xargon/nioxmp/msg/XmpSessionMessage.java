package it.xargon.nioxmp.msg;

import java.util.Objects;

import it.xargon.util.Identifier;
import it.xargon.util.Debug.Printable;

public abstract class XmpSessionMessage implements Printable {
   protected Identifier sessionId=null;
   
   public XmpSessionMessage(Identifier sessionId) {
      this.sessionId=Objects.requireNonNull(sessionId);
   }
   
   public Identifier getSessionId() {return sessionId;}
   
   protected abstract void addDetails(StringBuilder sb);
   
   @Override
   public String toString(String indent) {
      StringBuilder sb=new StringBuilder();
      sb.append(indent).append(sessionId.toString()).append("] ");
      addDetails(sb);
      sb.append("\n");
      return sb.toString();
   }

}
