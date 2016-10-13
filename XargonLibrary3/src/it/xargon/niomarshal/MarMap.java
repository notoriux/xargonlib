package it.xargon.niomarshal;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

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
      ArrayList<ByteBuffer> elements=new ArrayList<>();
      
      //Numero di elementi nella mappa
      ByteBuffer cntBuf=alloc(Integer.BYTES);
      cntBuf.putInt(map.size()).flip();
      elements.add(cntBuf);
      
      //Serializziamo ogni singola coppia chiave-valore
      map.entrySet().forEach(kv -> elements.add(getDataBridge().marshal(kv)));
     
      //Raccogliere tutti i risultati in un solo buffer
      int totalSize=elements.stream().collect(Collectors.summingInt(ByteBuffer::remaining));
      ByteBuffer result=alloc(totalSize);
      elements.forEach(result::put);
      result.flip();
      
      return result;
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
