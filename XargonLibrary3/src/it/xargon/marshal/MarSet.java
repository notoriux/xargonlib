package it.xargon.marshal;

import it.xargon.util.Bitwise;

import java.io.*;
import java.util.*;

public class MarSet extends AbstractMarshaller {
   public MarSet() {super("SET",Source.STREAM, Set.class);}
   
   public float getAffinity(Class<?> javaclass) {
      if (Set.class.isAssignableFrom(javaclass)) return 1f;
      return 0f;
   }
   
   @SuppressWarnings("unchecked")
   public void marshalToStream(Object obj, OutputStream out) throws IOException {
      Set<Object> tlist=(Set<Object>)obj;
      out.write(Bitwise.intToByteArray(tlist.size()));
      for(Object o:tlist) dataBridge.marshal(o, false, out);
      out.flush();
   }
   
   public Object unmarshalFromStream(InputStream in) throws IOException {
      int total=Bitwise.readInt(in);
      HashSet<Object> result=new HashSet<Object>();
      for(int cnt=0;cnt<total;cnt++) result.add(dataBridge.unmarshal(in));
      return result;
   }
}
