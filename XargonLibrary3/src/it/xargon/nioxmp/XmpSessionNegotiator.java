package it.xargon.nioxmp;

import java.io.IOException;

import it.xargon.nioxmp.msg.XmpMessage;
import it.xargon.nioxmp.msg.XmpSessionInit;
import it.xargon.nioxmp.msg.XmpSessionKO;
import it.xargon.nioxmp.msg.XmpSessionOK;
import it.xargon.util.Identifier;

class XmpSessionNegotiator {   
   public static Identifier clientNewSession(XmpWire wire) throws IOException {
      if (wire.getCurrentWorkMode().equals(XmpWire.WorkMode.ASYNC))
         throw new IllegalArgumentException("XMP Wire must be in SYNC mode");
      
      wire.send(new XmpSessionInit(Identifier.IDZERO)); //Will require a new session
      XmpMessage incoming=wire.receive();
      
      if (incoming==null) throw new IOException("Unable to negotiate new session: channel closed");
      if (!(incoming instanceof XmpSessionOK))
         throw new IOException("Unexpected message on the wire: " + incoming.getClass().getName());
      
      return incoming.getSessionId();
   }
   
   public static void clientResumeSession(XmpWire wire, Identifier sessionId) throws IOException {
      if (wire.getCurrentWorkMode().equals(XmpWire.WorkMode.ASYNC))
         throw new IllegalArgumentException("XMP Wire must be in SYNC mode");
      
      wire.send(new XmpSessionInit(sessionId)); //Will resume an existing session
      XmpMessage incoming=wire.receive();
      
      if (incoming==null) throw new IOException("Unable to resume session: channel closed");
      if (incoming instanceof XmpSessionKO)
         throw new IOException("Server refused to resume the requested session: " + ((XmpSessionKO)incoming).getReason());
      if (!(incoming instanceof XmpSessionOK))
         throw new IOException("Unexpected message on the wire: " + incoming.getClass().getName());
   }
   
   public static Identifier serverNegotiateSession(XmpWire wire, XmpServerSessionManager smgr) throws IOException {
      if (wire.getCurrentWorkMode().equals(XmpWire.WorkMode.ASYNC))
         throw new IllegalArgumentException("XMP Wire must be in SYNC mode");

      XmpMessage incoming=wire.receive();
      
      if (incoming==null) {
         wire.send(new XmpSessionKO(Identifier.IDZERO, "No requests detected"));
         throw new IOException("No session request on wire");
      }
      if (!(incoming instanceof XmpSessionInit)) {
         wire.send(new XmpSessionKO(Identifier.IDZERO, "Sorry, I don't understand you"));
         throw new IOException("Unexpected message on the wire: " + incoming.getClass().getName());
      }

      Identifier sessionId=incoming.getSessionId();
      
      if (sessionId.equals(Identifier.IDZERO)) {
         Identifier newSessionId=smgr.createSession();
         wire.send(new XmpSessionOK(newSessionId));
         return newSessionId;
      }
      
      XmpAbstractEndpointImpl client=smgr.getClientBySession(sessionId);
      
      if (client==null) {
         wire.send(new XmpSessionKO(Identifier.IDZERO, "Unknown session ID"));
         return null;
      }
      
      if (client.getWire().isOpen()) {
         wire.send(new XmpSessionKO(Identifier.IDZERO, "Session is still alive"));
         return null;
      }
      
      wire.send(new XmpSessionOK(sessionId));
      return sessionId;
   }
}
