package it.xargon.niomarshal;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

public class MarSet extends AbstractMarshaller<Set<?>> {
   public MarSet() {super("SET");}
   
   public float getAffinity(Class<?> javaclass) {
      if (Set.class.isAssignableFrom(javaclass)) return 1f;
      return 0f;
   }

   @Override
   public ByteBuffer marshal(Set<?> set) {
      ArrayList<ByteBuffer> elements=new ArrayList<>();
      
      //Numero di elementi nel set
      ByteBuffer cntBuf=alloc(Integer.BYTES);
      cntBuf.putInt(set.size()).flip();
      elements.add(cntBuf);
      
      //Serializziamo ogni singolo elemento del set
      set.forEach(obj -> elements.add(getDataBridge().marshal(obj)));
      
      //Raccogliere tutti i risultati in un solo buffer
      int totalSize=elements.stream().collect(Collectors.summingInt(ByteBuffer::remaining));
      ByteBuffer result=alloc(totalSize);
      elements.forEach(result::put);
      
      return result;
   }

   @Override
   public Set<?> unmarshal(ByteBuffer buffer) {
      HashSet<Object> result=new HashSet<Object>();

      for(int total=buffer.getInt();total>0;total--)
         result.add(getDataBridge().unmarshal(buffer));
      
      return result;
   }
}
