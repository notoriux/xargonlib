package it.xargon.niomarshal;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;

import it.xargon.util.ByteBufferAccumulator;

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
      ByteBufferAccumulator accumulator=new ByteBufferAccumulator(getAllocator());
      
      if (mapentry.getKey()==null) throw new IllegalArgumentException("Null-key not allowed");
      accumulator.add(getDataBridge().marshal(mapentry.getKey()));

      if (mapentry.getValue()==null) { //Il valore è null (può succedere)
         accumulator.add((byte)0x00);
      } else { //Il valore è presente
         accumulator.add((byte)0xFF);
         accumulator.add(getDataBridge().marshal(mapentry.getValue()));
      }
      
      //Raccogliere tutti i risultati in un solo buffer
      return accumulator.gather();
   }

   @Override
   public Entry unmarshal(ByteBuffer buffer) {
      Object key=getDataBridge().unmarshal(buffer);
      Object value=(buffer.get()==0?null:getDataBridge().unmarshal(buffer));
      return new MapEntryImpl<Object, Object>(key, value);
   }
}
