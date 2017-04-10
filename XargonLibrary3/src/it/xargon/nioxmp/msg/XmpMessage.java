package it.xargon.nioxmp.msg;

import java.util.Objects;

import it.xargon.util.Identifier;
import it.xargon.util.Debug.Printable;

public abstract class XmpMessage implements Printable {
   protected Identifier sessionId=null;
   
   public XmpMessage(Identifier sessionId) {
      this.sessionId=Objects.requireNonNull(sessionId);
   }
   
   public Identifier getSessionId() {return sessionId;}
}
