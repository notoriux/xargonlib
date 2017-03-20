package it.xargon.nioxmp.msg;

import java.nio.ByteBuffer;
import java.util.Objects;

import it.xargon.util.*;

public class XmpContentAnswer extends XmpContentMessage {
   private Identifier parentId=null;
   
   public XmpContentAnswer(Identifier sessionId, Identifier messageId, Identifier parentId, ByteBuffer contents) {
      super(sessionId, messageId, contents);
      parentId=Objects.requireNonNull(parentId);
   }
   
   public Identifier getParentId() {return parentId;}

   public boolean isAnswerFor(XmpContentRequest request) { 
      return parentId.equals(request.getMessageId());
   }
   
   @Override
   protected void addDetails(StringBuilder sb) {
      sb.append("ANSWER FOR ").append(parentId.toString());
   }
}