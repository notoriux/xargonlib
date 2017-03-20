package it.xargon.nioxmp.msg;

import it.xargon.util.Identifier;

public class XmpSessionClose extends XmpSessionMessage {
   public XmpSessionClose(Identifier sessionId) {
      super(sessionId);
   }

   @Override
   protected void addDetails(StringBuilder sb) {
      sb.append("SESSION CLOSE REQ");
   }
}