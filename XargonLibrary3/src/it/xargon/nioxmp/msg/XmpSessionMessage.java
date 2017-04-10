package it.xargon.nioxmp.msg;

import it.xargon.util.Identifier;

public abstract class XmpSessionMessage extends XmpMessage {
   
   public XmpSessionMessage(Identifier sessionId) {
      super(sessionId);
   }
      
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
