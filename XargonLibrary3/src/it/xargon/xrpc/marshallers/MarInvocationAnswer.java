package it.xargon.xrpc.marshallers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import it.xargon.marshal.AbstractMarshaller;
import it.xargon.xrpc.messages.MsgInvocationAnswer;
import it.xargon.xrpc.messages.MsgObjectDescription;
import it.xargon.xrpc.messages.MsgInvocationAnswer.AnswerType;

public class MarInvocationAnswer extends AbstractMarshaller {
   public MarInvocationAnswer() {
      super("XMP-INVOKE-ANS", Source.STREAM, MsgInvocationAnswer.class);
   }
   
   public void marshalToStream(Object obj, OutputStream out) throws IOException {
      MsgInvocationAnswer xobj=(MsgInvocationAnswer)obj;
      
      out.write(xobj.answtype.getId());
      dataBridge.marshal(xobj.content, true, out);
   }
   
   public Object unmarshalFromStream(InputStream in) throws IOException {
      MsgInvocationAnswer xobj=new MsgInvocationAnswer();
      
      xobj.answtype=AnswerType.getById(in.read());
      xobj.content=dataBridge.unmarshal(in, MsgObjectDescription.class);
      
      return xobj;
   }
}
