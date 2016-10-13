package it.xargon.niomarshal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

import it.xargon.util.Tools;

@SuppressWarnings("rawtypes")
public class MarCollection extends AbstractMarshaller<Collection> {
   public MarCollection() {super("COLL");}
   
   public float getAffinity(Class<?> javaclass) {
      if (Collection.class.isAssignableFrom(javaclass)) return 0.9f;
      return 0f;
   }

   @SuppressWarnings("unchecked")
   @Override
   public ByteBuffer marshal(Collection coll) {   
      ArrayList<ByteBuffer> elements=new ArrayList<>();
      
      //Implementazione concreta della collection
      byte[] className=Tools.getBytes(coll.getClass().getName());
      ByteBuffer clBuf=alloc(Integer.BYTES + className.length);
      clBuf.putInt(className.length).put(className).flip();
      elements.add(clBuf);
      
      //Numero di elementi nella collection
      ByteBuffer cntBuf=alloc(Integer.BYTES);
      cntBuf.putInt(coll.size()).flip();
      elements.add(cntBuf);

      //Serializziamo ogni singolo elemento della collection
      coll.forEach(obj -> {
         ByteBuffer elemBuf=null;
         if (obj==null) {
            elemBuf=alloc(1).put((byte)0x00); //prefissato da "0x00" se è null
            elemBuf.flip();
            elements.add(elemBuf);
         } else {
            elemBuf=alloc(1).put((byte)0xFF); //o da "0xFF" se presente
            elemBuf.flip();
            elements.add(elemBuf);
            elements.add(getDataBridge().marshal(obj));
         }
      });
      
      //Raccogliere tutti i risultati in un solo buffer
      int totalSize=elements.stream().collect(Collectors.summingInt(ByteBuffer::remaining));
      ByteBuffer result=alloc(totalSize);
      elements.forEach(result::put);
      result.flip();
      
      return result;
   }

   @SuppressWarnings("unchecked")
   @Override
   public Collection unmarshal(ByteBuffer buffer) {
      String listClassName=Tools.bufferToString(buffer);
      Collection<Object> result=null;
            
      try {
         Class<? extends Collection> collClass=(Class<? extends Collection>) Class.forName(listClassName);
         Constructor<? extends Collection> collConstructor=collClass.getConstructor();
         result=collConstructor.newInstance();
      } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
         throw new IllegalStateException("Unable to instantiate Collection implementation: " + listClassName, e);
      }
      
      int total=buffer.getInt();
      
      for(int cnt=0;cnt<total;cnt++) {
         if (buffer.get()==0x00) result.add(null);
         else result.add(getDataBridge().unmarshal(buffer));
      }
      
      return result;
   }
}
