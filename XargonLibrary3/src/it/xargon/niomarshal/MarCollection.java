package it.xargon.niomarshal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.*;

import it.xargon.util.ByteBufferAccumulator;
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
      //ArrayList<ByteBuffer> elements=new ArrayList<>();
      ByteBufferAccumulator accumulator=new ByteBufferAccumulator(getAllocator());
      
      //Implementazione concreta della collection
      byte[] className=Tools.getBytes(coll.getClass().getName());
      accumulator.addWithSize(className);
      
      //Numero di elementi nella collection
      accumulator.add(coll.size());

      //Serializziamo ogni singolo elemento della collection
      coll.forEach(obj -> {
         if (obj==null) {
            //prefissato da "0x00" se è null
            accumulator.add((byte)0x00);
         } else {
            //o da "0xFF" se presente
            accumulator.add((byte)0xFF);
            accumulator.add(getDataBridge().marshal(obj));
         }
      });
      
      //Raccogliere tutti i risultati in un solo buffer
      return accumulator.gather();
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
