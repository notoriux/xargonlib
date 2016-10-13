package it.xargon.niomarshal;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@SuppressWarnings("rawtypes")
public class MarKVPair extends AbstractMarshaller<Map.Entry> {
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
   
   public MarKVPair() {super("KVPAIR");}
   
   public float getAffinity(Class<?> javaclass) {
      if (Map.Entry.class.isAssignableFrom(javaclass)) return 1f;
      return 0f;
   }

   @Override
   public ByteBuffer marshal(Entry mapentry) {
      ArrayList<ByteBuffer> elements=new ArrayList<>();
      
      if (mapentry.getKey()==null) throw new IllegalArgumentException("Null-key not allowed");
      elements.add(getDataBridge().marshal(mapentry.getKey()));

      if (mapentry.getValue()==null) { //Il valore è null (può succedere)
         ByteBuffer flag=alloc(1).put((byte)0x00);
         flag.flip();
         elements.add(flag);
      } else { //Il valore è presente
         ByteBuffer flag=alloc(1).put((byte)0xFF);
         flag.flip();
         elements.add(flag);
         elements.add(getDataBridge().marshal(mapentry.getValue()));
      }
      
      //Raccogliere tutti i risultati in un solo buffer
      int totalSize=elements.stream().collect(Collectors.summingInt(ByteBuffer::remaining));
      ByteBuffer result=alloc(totalSize);
      elements.forEach(result::put);
      result.flip();
      
      return result;
   }

   @Override
   public Entry unmarshal(ByteBuffer buffer) {
      Object key=getDataBridge().unmarshal(buffer);
      Object value=(buffer.get()==0?null:getDataBridge().unmarshal(buffer));
      return new MapEntryImpl<Object, Object>(key, value);
   }
}
