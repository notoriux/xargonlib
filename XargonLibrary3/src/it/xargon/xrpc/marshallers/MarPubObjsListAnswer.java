package it.xargon.xrpc.marshallers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import it.xargon.marshal.AbstractMarshaller;
import it.xargon.xrpc.messages.MsgPubObjsListAnswer;

public class MarPubObjsListAnswer extends AbstractMarshaller {
   public MarPubObjsListAnswer() {
      super("XMP-PUBOBJ-ANSLIST", Source.STREAM, MsgPubObjsListAnswer.class);
   }
   
   public void marshalToStream(Object obj, OutputStream out) throws IOException {
      MsgPubObjsListAnswer xobj=(MsgPubObjsListAnswer)obj;
      dataBridge.marshal(xobj.pubObjList, true, out);
   }
   
   public Object unmarshalFromStream(InputStream in) throws IOException {
      MsgPubObjsListAnswer xobj=new MsgPubObjsListAnswer();
      
      @SuppressWarnings("unchecked")
      Map<Object,Object> result=dataBridge.unmarshal(in, Map.class);
      
      xobj.pubObjList=new HashMap<String, String[]>();
      
      for(Map.Entry<Object, Object> entry:result.entrySet()) {
         String objname=(String)entry.getKey();
         String[] ifaces=(String[])entry.getValue();
         xobj.pubObjList.put(objname, ifaces);
      }
      
      return xobj;
   }
}


