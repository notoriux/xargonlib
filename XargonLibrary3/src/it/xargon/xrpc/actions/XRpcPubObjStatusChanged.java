package it.xargon.xrpc.actions;

import java.util.HashMap;
import it.xargon.util.BooleanLatch;
import it.xargon.xrpc.XRpcEndpoint;
import it.xargon.xrpc.XRpcEndpointImpl;
import it.xargon.xrpc.messages.MsgPubObjStatusChanged;

public class XRpcPubObjStatusChanged extends XRpcAction<MsgPubObjStatusChanged, Void> {
   public Void process(MsgPubObjStatusChanged message, XRpcEndpointImpl endpoint) {
      HashMap<String, BooleanLatch> remoteObjectAwaiters=endpoint.getRemoteObjectAwaiters();
      XRpcEndpoint.Events raiseEvent=endpoint.getEventRaiser();
      
      synchronized (remoteObjectAwaiters) {
         String pubname=message.objname;
         if (message.published) {
            raiseEvent.remotePublished(endpoint, pubname);
            BooleanLatch latch=remoteObjectAwaiters.remove(pubname);
            if (latch!=null) latch.open();
         } else {
            raiseEvent.remoteUnpublished(endpoint, pubname);
         }
      }
      return null;
   } 
}
