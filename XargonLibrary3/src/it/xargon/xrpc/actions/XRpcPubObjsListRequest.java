package it.xargon.xrpc.actions;

import java.util.HashMap;

import it.xargon.util.Identifier;
import it.xargon.xrpc.*;
import it.xargon.xrpc.messages.MsgPubObjsListRequest;
import it.xargon.xrpc.messages.MsgPubObjsListAnswer;;

public class XRpcPubObjsListRequest extends XRpcAction<MsgPubObjsListRequest, MsgPubObjsListAnswer> {
   public MsgPubObjsListAnswer process(MsgPubObjsListRequest message, XRpcEndpointImpl endpoint) {
      MsgPubObjsListAnswer answer=new MsgPubObjsListAnswer();
      
      answer.pubObjList=new HashMap<String, String[]>();
      String[] objnames=endpoint.getLocalCache().getAllPublishedNames();
      for(String objname:objnames) {
         Identifier objid=endpoint.getLocalCache().getPublishedObjectId(objname);
         LocalObjectWrapper objwrap=endpoint.getLocalCache().getObjectWrapper(objid);
         String[] ifaces=objwrap.getInterfaceNames();
         answer.pubObjList.put(objname, ifaces);
      }
      
      return answer;
   }
}
