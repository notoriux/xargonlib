package it.xargon.xrpc.marshallers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import it.xargon.marshal.AbstractMarshaller;
import it.xargon.util.Identifier;
import it.xargon.xrpc.messages.MsgObjectRequest;

public class MarObjectRequest extends AbstractMarshaller {
   public MarObjectRequest() {
      super("XMP-OBJ-REQ", Source.STREAM, MsgObjectRequest.class);
   }
   
   public void marshalToStream(Object obj, OutputStream out) throws IOException {
      MsgObjectRequest xobj=(MsgObjectRequest)obj;

      xobj.target.writeOn(out);
   }
   
   public Object unmarshalFromStream(InputStream in) throws IOException {
      MsgObjectRequest xobj=new MsgObjectRequest();
      
      xobj.target=Identifier.readIdentifier(in);
      
      return xobj;
   }
}


