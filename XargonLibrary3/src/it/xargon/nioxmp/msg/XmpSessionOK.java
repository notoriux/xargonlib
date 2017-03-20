package it.xargon.nioxmp.msg;

import it.xargon.util.*;

class XmpSessionOK extends XmpSessionMessage {
   public XmpSessionOK(Identifier sessionId) {
      super(sessionId);
   }

   @Override
   protected void addDetails(StringBuilder sb) {
      sb.append("SESSION INIT OK");
   }

}