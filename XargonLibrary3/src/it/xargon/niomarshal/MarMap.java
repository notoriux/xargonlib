package it.xargon.niomarshal;

import java.nio.ByteBuffer;
import java.util.*;

import it.xargon.util.ByteBufferAccumulator; 
@SuppressWarnings("rawtypes")
public class MarMap extends AbstractMarshaller<Map> {
   public MarMap() {super("MAP");}
   
   public float getAffinity(Class<?> javaclass) {
      if (Map.class.isAssignableFrom(javaclass)) return 1f;
      return 0f;
   }

   @SuppressWarnings("unchecked")
   @Override
   public ByteBuffer marshal(Map map) {
      ByteBufferAccumulator accumulator=new ByteBufferAccumulator(getAllocator());
      //Numero di elementi nella mappa
      accumulator.add(map.size());
      //Serializziamo ogni singola coppia chiave-valore
      map.entrySet().forEach(kv -> accumulator.add(getDataBridge().marshal(kv)));
      //Raccogliere tutti i risultati in un solo buffer
      return accumulator.gather();
   }

   @Override
   public Map unmarshal(ByteBuffer buffer) {
      Map<Object,Object> result=new HashMap<Object,Object>();
      
      for(int total=buffer.getInt();total>0;total--) {
         Map.Entry<?,?> entry=getDataBridge().unmarshal(buffer, Map.Entry.class);
         result.put(entry.getKey(), entry.getValue());
      }
      
      return result;
   }
}
