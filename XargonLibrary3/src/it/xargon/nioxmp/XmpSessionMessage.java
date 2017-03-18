package it.xargon.nioxmp;

import java.util.Objects;

import it.xargon.util.*;

class XmpSessionMessage implements it.xargon.util.Debug.Printable {
   private XmpSessionMessageType itype=null;
   private Identifier sessionId=null;
   
   public XmpSessionMessage(XmpSessionMessageType type) {
      itype=type;
      sessionId=new Identifier();
   }

   public XmpSessionMessage setSessionId(Identifier sessionId) {
      this.sessionId=Objects.requireNonNull(sessionId);
      return this;
   }
   
   public Identifier getSessionId() {return sessionId;}

   public XmpSessionMessage setType(XmpSessionMessageType type) {
      itype=Objects.requireNonNull(type);
      return this;
   }
   
   public XmpSessionMessageType getType() {return itype;}
   
   public String toString(String indent) {
      StringBuilder sb=new StringBuilder();
      sb.append(indent);
      switch (itype) {
         case INIT:
            sb.append("SESSION INIT REQ");
            break;
         case INIT_ACK:
            sb.append("SESSION INIT ACK: ").append(sessionId.toString());
            break;
         case RESUME:
            sb.append("SESSION RESUME REQ: ").append(sessionId.toString());
            break;
         case RESUME_OK:
            sb.append("SESSION RESUME OK: ").append(sessionId.toString());
            break;
         case RESUME_KO:
            sb.append("SESSION RESUME KO: ").append(sessionId.toString());
            break;
         case CLOSE:
            sb.append("SESSION CLOSE REQ: ").append(sessionId.toString());
            break;
         case CLOSE_ACK:
            sb.append("SESSION CLOSE ACK: ").append(sessionId.toString());
            break;
         default:
            sb.append("(unknown session message type)");
            break;
      }
      sb.append("\n");
      return sb.toString();
   }
}