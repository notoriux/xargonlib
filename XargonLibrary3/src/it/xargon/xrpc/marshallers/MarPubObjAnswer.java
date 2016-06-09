package it.xargon.xrpc.marshallers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import it.xargon.marshal.AbstractMarshaller;
import it.xargon.util.Identifier;
import it.xargon.xrpc.messages.MsgPubObjAnswer;

public class MarPubObjAnswer extends AbstractMarshaller {
   public MarPubObjAnswer() {
      super("XMP-PUBOBJ-ANS", Source.STREAM, MsgPubObjAnswer.class);
   }
   
   public void marshalToStream(Object obj, OutputStream out) throws IOException {
      MsgPubObjAnswer xobj=(MsgPubObjAnswer)obj;

      if (xobj.target==null) out.write(0); else out.write(1);
      xobj.target.writeOn(out);
   }
   
   public Object unmarshalFromStream(InputStream in) throws IOException {
      MsgPubObjAnswer xobj=new MsgPubObjAnswer();
      
      boolean flag=(in.read()==1);
      if (flag) xobj.target=Identifier.readIdentifier(in);
      
      return xobj;
   }
}


