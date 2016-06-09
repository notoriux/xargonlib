package it.xargon.xrpc.marshallers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import it.xargon.marshal.AbstractMarshaller;
import it.xargon.util.Bitwise;
import it.xargon.util.Identifier;
import it.xargon.util.Tools;
import it.xargon.xrpc.messages.MsgObjectDescription;
import it.xargon.xrpc.messages.MsgObjectDescription.Flavor;

public class MarObjectDescription extends AbstractMarshaller {
   public MarObjectDescription() {
      super("XMP-OBJDESC", Source.STREAM, MsgObjectDescription.class);
   }
   
   public void marshalToStream(Object obj, OutputStream out) throws IOException {
      MsgObjectDescription xobj=(MsgObjectDescription)obj;

      if (xobj.flavor==null) out.write(0);
      else {
         out.write(1);
         out.write(xobj.flavor.id());
      }
      
      if (xobj.sercontents==null) out.write(0);
      else {
         out.write(1);
         out.write(Bitwise.intToByteArray(xobj.sercontents.length));
         out.write(xobj.sercontents);
      }
      
      if (xobj.objid==null) out.write(0);
      else {
         out.write(1);
         xobj.objid.writeOn(out);
      }
      
      if (xobj.interfaces==null) out.write(0);
      else {
         out.write(1);
         dataBridge.marshal(xobj.interfaces, true, out);         
      }
   }
   
   public Object unmarshalFromStream(InputStream in) throws IOException {
      MsgObjectDescription xobj=new MsgObjectDescription();
      
      int flag=0;
      
      flag=in.read();
      if (flag==0) xobj.flavor=null;
      else xobj.flavor=Flavor.getById(Bitwise.asByte(in.read()));
      
      flag=in.read();
      if (flag==0) xobj.sercontents=null;
      else {
         int len=Bitwise.readInt(in);
         xobj.sercontents=new byte[len];
         Tools.forceRead(in, xobj.sercontents, false);
      }
      
      flag=in.read();
      if (flag==0) xobj.objid=null;
      else xobj.objid=Identifier.readIdentifier(in);
      
      flag=in.read();
      if (flag==0) xobj.interfaces=null;
      else xobj.interfaces=dataBridge.unmarshal(in, String[].class);
      
      return xobj;
   }
}
