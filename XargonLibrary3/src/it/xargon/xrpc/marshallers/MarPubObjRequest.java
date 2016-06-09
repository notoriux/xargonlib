package it.xargon.xrpc.marshallers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import it.xargon.marshal.AbstractMarshaller;
import it.xargon.xrpc.messages.MsgPubObjRequest;

public class MarPubObjRequest extends AbstractMarshaller {
   public MarPubObjRequest() {
      super("XMP-PUBOBJ-REQ", Source.STREAM, MsgPubObjRequest.class);
   }
   
   public void marshalToStream(Object obj, OutputStream out) throws IOException {
      MsgPubObjRequest xobj=(MsgPubObjRequest)obj;
      dataBridge.marshal(xobj.pubname, true, out);
   }
   
   public Object unmarshalFromStream(InputStream in) throws IOException {
      MsgPubObjRequest xobj=new MsgPubObjRequest();
      xobj.pubname=dataBridge.unmarshal(in, String.class);
      return xobj;
   }
}


