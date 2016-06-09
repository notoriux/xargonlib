package it.xargon.xrpc.actions;

import it.xargon.util.Identifier;
import it.xargon.xrpc.*;
import it.xargon.xrpc.messages.MsgPubObjAnswer;
import it.xargon.xrpc.messages.MsgPubObjRequest;

public class XRpcPubObjRequest extends XRpcAction<MsgPubObjRequest, MsgPubObjAnswer> {
   public MsgPubObjAnswer process(MsgPubObjRequest message, XRpcEndpointImpl endpoint) {
      String objname=message.pubname;
      Identifier objid=endpoint.getLocalCache().getPublishedObjectId(objname);
      MsgPubObjAnswer answer=new MsgPubObjAnswer();
      answer.target=objid;
      return answer;
   }
}
