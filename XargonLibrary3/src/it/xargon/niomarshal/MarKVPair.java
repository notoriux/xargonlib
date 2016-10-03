package it.xargon.niomarshal;

import java.io.*;
import java.util.*;

public class MarKVPair extends AbstractMarshaller {
   private static class MapEntryImpl<K, V> implements Map.Entry<K, V> {
      private K ikey=null;
      private V ivalue=null;
      
      public MapEntryImpl(K key, V value) {ikey=key;ivalue=value;}

      public K getKey() {return ikey;}
      public V getValue() {return ivalue;}
      public V setValue(V value) {
         V oldvalue=ivalue;
         ivalue=value;
         return oldvalue;
      }
   }
   
   public MarKVPair() {super("KVPAIR", Source.STREAM, Map.Entry.class);}
   
   public float getAffinity(Class<?> javaclass) {
      if (Map.Entry.class.isAssignableFrom(javaclass)) return 1f;
      return 0f;
   }
   
   public void marshalToStream(Object obj, OutputStream out) throws IOException {
      Map.Entry<?, ?> mapentry=(Map.Entry<?, ?>)obj;
      
      if (mapentry.getKey()==null) {
         out.write(0); //La chiave è null (non dovrebbe mai accadere)
      } else {
         out.write(0xFF); //La chiave è presente
         dataBridge.marshal(mapentry.getKey(), false, out);
      }

      if (mapentry.getValue()==null) {
         out.write(0); //Il valore è null (può succedere)
      } else {
         out.write(0xFF); //Il valore è presente
         dataBridge.marshal(mapentry.getValue(), false, out);
      }
      
      out.flush();
   }
   
   public Object unmarshalFromStream(InputStream in) throws IOException {
      Object key=null;
      Object value=null;
      
      if (in.read()!=0) key=dataBridge.unmarshal(in);
      if (in.read()!=0) value=dataBridge.unmarshal(in);
      
      return new MapEntryImpl<Object, Object>(key, value);
   }
}
