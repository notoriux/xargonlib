package it.xargon.nioxmp.msg;

import it.xargon.util.Identifier;

public class XmpSessionInit extends XmpSessionMessage {
   public XmpSessionInit(Identifier sessionId) {
      super(sessionId);
   }

   @Override
   protected void addDetails(StringBuilder sb) {
      sb.append("SESSION INIT REQ");
   }
}