package it.xargon.xrpc.actions;

import it.xargon.util.Identifier;
import it.xargon.xrpc.LocalObjectWrapper;
import it.xargon.xrpc.XRpcEndpointImpl;
import it.xargon.xrpc.messages.MsgObjectDescription;
import it.xargon.xrpc.messages.MsgObjectRequest;
import it.xargon.xrpc.messages.MsgObjectDescription.Flavor;

public class XRpcObjectRequest extends XRpcAction<MsgObjectRequest, MsgObjectDescription> {
   public MsgObjectDescription process(MsgObjectRequest message, XRpcEndpointImpl endpoint) {
      Identifier objid=((MsgObjectRequest)message).target;
      LocalObjectWrapper objwrap=endpoint.getLocalCache().getObjectWrapper(objid);
      MsgObjectDescription answer=new MsgObjectDescription();
      if (objwrap==null) answer.flavor=Flavor.VOID;
      else {
         answer.objid=objid;
         answer.flavor=Flavor.SOURCE_REF;
         answer.interfaces=objwrap.getInterfaceNames();
      }
      return answer;
   }
}
