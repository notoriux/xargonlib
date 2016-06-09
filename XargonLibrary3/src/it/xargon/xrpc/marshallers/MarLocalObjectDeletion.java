package it.xargon.xrpc.marshallers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import it.xargon.marshal.AbstractMarshaller;
import it.xargon.util.Identifier;
import it.xargon.xrpc.messages.MsgLocalObjectDeletion;

public class MarLocalObjectDeletion extends AbstractMarshaller {
   public MarLocalObjectDeletion() {
      super("XMP-LOCAL-DEL", Source.STREAM, MsgLocalObjectDeletion.class);
   }
   
   public void marshalToStream(Object obj, OutputStream out) throws IOException {
      MsgLocalObjectDeletion xobj=(MsgLocalObjectDeletion)obj;

      xobj.target.writeOn(out);
   }
   
   public Object unmarshalFromStream(InputStream in) throws IOException {
      MsgLocalObjectDeletion xobj=new MsgLocalObjectDeletion();
      
      xobj.target=Identifier.readIdentifier(in);
      
      return xobj;
   }
}


