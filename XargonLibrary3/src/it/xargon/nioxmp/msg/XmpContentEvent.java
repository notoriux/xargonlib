package it.xargon.nioxmp.msg;

import java.nio.ByteBuffer;

import it.xargon.util.*;

public class XmpContentEvent extends XmpContentMessage {
   public XmpContentEvent(Identifier messageId, Identifier sessionId, ByteBuffer contents) {
      super(messageId, sessionId, contents);
   }

   @Override
   protected void addDetails(StringBuilder sb) {
      sb.append("EVENT");
   }
}