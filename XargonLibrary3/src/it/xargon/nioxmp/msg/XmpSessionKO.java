package it.xargon.nioxmp.msg;

import it.xargon.util.*;

public class XmpSessionKO extends XmpSessionMessage {
   private String reason=null;
   
   public XmpSessionKO(Identifier sessionId, String reason) {
      super(sessionId);
      this.reason=reason;
   }
   
   public String getReason() {return reason;}

   @Override
   protected void addDetails(StringBuilder sb) {
      sb.append("SESSION INIT KO: ").append(reason);
   }

}