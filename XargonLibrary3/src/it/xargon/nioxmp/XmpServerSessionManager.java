package it.xargon.nioxmp;

import it.xargon.util.Identifier;

interface XmpServerSessionManager {
   public XmpAbstractEndpointImpl getClientBySession(Identifier sessionId);
   
   public Identifier createSession();
}
