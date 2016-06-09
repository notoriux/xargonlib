package it.xargon.xrpc.marshallers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import it.xargon.marshal.AbstractMarshaller;
import it.xargon.xrpc.messages.MsgPubObjsListRequest;

public class MarPubObjsListRequest extends AbstractMarshaller {
   private static MsgPubObjsListRequest singleton=new MsgPubObjsListRequest();
   
   public MarPubObjsListRequest() {
      super("XMP-PUBOBJ-REQLIST", Source.STREAM, MsgPubObjsListRequest.class);
   }
   
   public void marshalToStream(Object obj, OutputStream out) throws IOException {
      //unico caso di messaggio senza contenuti
   }
   
   public Object unmarshalFromStream(InputStream in) throws IOException {
      return singleton;
   }
}


