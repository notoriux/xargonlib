package it.xargon.xrpc.marshallers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import it.xargon.marshal.AbstractMarshaller;
import it.xargon.xrpc.messages.MsgPubObjStatusChanged;

public class MarPubObjStatusChanged extends AbstractMarshaller {
   public MarPubObjStatusChanged() {
      super("XMP-PUBOBJ-ST", Source.STREAM, MsgPubObjStatusChanged.class);
   }
   
   public void marshalToStream(Object obj, OutputStream out) throws IOException {
      MsgPubObjStatusChanged xobj=(MsgPubObjStatusChanged)obj;
      dataBridge.marshal(xobj.objname, true, out);
      if (xobj.published) out.write(1); else out.write(0);
   }
   
   public Object unmarshalFromStream(InputStream in) throws IOException {
      MsgPubObjStatusChanged xobj=new MsgPubObjStatusChanged();
      xobj.objname=dataBridge.unmarshal(in, String.class);
      xobj.published=(in.read()==1);
      return xobj;
   }
}


