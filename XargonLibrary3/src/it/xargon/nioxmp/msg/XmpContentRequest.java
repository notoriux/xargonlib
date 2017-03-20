package it.xargon.nioxmp.msg;

import java.nio.ByteBuffer;

import it.xargon.util.*;

public class XmpContentRequest extends XmpContentMessage {   
   public XmpContentRequest(Identifier sessionId, Identifier messageId, ByteBuffer contents) {
      super(sessionId, messageId, contents);
   }
   
   @Override
   protected void addDetails(StringBuilder sb) {
      sb.append("REQUEST");
   }   
}