package it.xargon.xrpc.actions;

import it.xargon.xrpc.XRpcEndpointImpl;
import it.xargon.xrpc.messages.MsgLocalObjectDeletion;

public class XRpcLocalObjectDeletion extends XRpcAction<MsgLocalObjectDeletion, Void> {
   public Void process(MsgLocalObjectDeletion message, XRpcEndpointImpl endpoint) {
      endpoint.getRemoteCache().removeCachedObject(message.target);
      return null;
   }
}
