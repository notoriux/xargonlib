package it.xargon.xrpc.marshallers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import it.xargon.marshal.AbstractMarshaller;
import it.xargon.util.Identifier;
import it.xargon.xrpc.messages.MsgRemoteProxyFinalized;

public class MarRemoteProxyFinalized extends AbstractMarshaller {
   public MarRemoteProxyFinalized() {
      super("XMP-REMOTE-FIN", Source.STREAM, MsgRemoteProxyFinalized.class);
   }
   
   public void marshalToStream(Object obj, OutputStream out) throws IOException {
      MsgRemoteProxyFinalized xobj=(MsgRemoteProxyFinalized)obj;

      xobj.target.writeOn(out);
   }
   
   public Object unmarshalFromStream(InputStream in) throws IOException {
      MsgRemoteProxyFinalized xobj=new MsgRemoteProxyFinalized();
      
      xobj.target=Identifier.readIdentifier(in);
      
      return xobj;
   }
}


