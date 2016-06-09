package it.xargon.xrpc.marshallers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import it.xargon.marshal.AbstractMarshaller;
import it.xargon.util.Identifier;
import it.xargon.xrpc.messages.MsgInvocation;
import it.xargon.xrpc.messages.MsgObjectDescription;

public class MarInvocation extends AbstractMarshaller {   
   public MarInvocation() {
      super("XMP-INVOKE", Source.STREAM, MsgInvocation.class);
   }
   
   public void marshalToStream(Object obj, OutputStream out) throws IOException {
      MsgInvocation xobj=(MsgInvocation)obj;

      xobj.targetObject.writeOn(out);
      dataBridge.marshal(xobj.targetClass, true, out);
      dataBridge.marshal(xobj.methodName, true, out);
      
      if (xobj.signature==null) out.write(0);
      else {
         out.write(1);
         dataBridge.marshal(xobj.signature, true, out);
         dataBridge.marshal(xobj.arguments, true, out);
      }      
   }
   
   public Object unmarshalFromStream(InputStream in) throws IOException {
      MsgInvocation xobj=new MsgInvocation();
      
      xobj.targetObject=Identifier.readIdentifier(in);
      xobj.targetClass=dataBridge.unmarshal(in, String.class);
      xobj.methodName=dataBridge.unmarshal(in, String.class);
      
      int flag=0;
      
      flag=in.read();
      if (flag==0) {
         xobj.signature=null;
         xobj.arguments=null;
      } else {         
         xobj.signature=dataBridge.unmarshal(in, String[].class);
         xobj.arguments=dataBridge.unmarshal(in, MsgObjectDescription[].class);
      }

      return xobj;
   }
}


