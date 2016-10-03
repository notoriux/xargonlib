package it.xargon.niomarshal;

import it.xargon.util.Bitwise;

import java.io.*;
import java.util.*;

public class MarList extends AbstractMarshaller {
   public MarList() {super("LIST",Source.STREAM, List.class);}
   
   public float getAffinity(Class<?> javaclass) {
      if (List.class.isAssignableFrom(javaclass)) return 1f;
      return 0f;
   }
   
   @SuppressWarnings("unchecked")
   public void marshalToStream(Object obj, OutputStream out) throws IOException {
      List<Object> tlist=(List<Object>)obj;
      out.write(Bitwise.intToByteArray(tlist.size()));
      
      for(int cnt=0;cnt<tlist.size();cnt++) {
         Object o=tlist.get(cnt);
         if (o!=null) {out.write(0xFF);dataBridge.marshal(o, false, out);}
         else {out.write(0x00);}
      }
      out.flush();
   }
   
   public Object unmarshalFromStream(InputStream in) throws IOException {
      int total=Bitwise.readInt(in);
      ArrayList<Object> result=new ArrayList<Object>(total);
      for(int cnt=0;cnt<total;cnt++) {
         boolean notnull=(in.read()!=0);
         if (notnull) result.set(cnt, dataBridge.unmarshal(in));
         else result.set(cnt, null);
      }
      return result;
   }
}
