package it.xargon.nioxmp.msg;

import it.xargon.util.*;

class XmpSessionKO extends XmpSessionMessage {
   public XmpSessionKO(Identifier sessionId) {
      super(sessionId);
   }

   @Override
   protected void addDetails(StringBuilder sb) {
      sb.append("SESSION INIT KO");
   }

}