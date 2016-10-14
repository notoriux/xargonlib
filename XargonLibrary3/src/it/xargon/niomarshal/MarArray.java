package it.xargon.niomarshal;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import it.xargon.util.*;

public class MarArray extends AbstractMarshaller<Object> {
   public MarArray() {super("ARRAY");}
   
   public float getAffinity(Class<?> javaclass) {
      if (javaclass.isArray()) return 0.8f;
      return 0f;
   }

   @Override
   @SuppressWarnings("unchecked")
   public ByteBuffer marshal(Object array) {
      ByteBufferAccumulator accumulator=new ByteBufferAccumulator(allocator);
      
      //Ottenere un marshaller più fedele possibile al component type e annotarlo nell'accumulatore
      AbstractMarshaller<Object> ctmar=(AbstractMarshaller<Object>) getDataBridge().getBestMarshaller(array.getClass().getComponentType());
      accumulator.addWithSize(ctmar.getEncName());

      //Ottenere il numero di elementi e annotarli nell'accumulatore
      int cnt=Array.getLength(array);
      accumulator.add(cnt);
      
      //Passare ogni elemento dell'array direttamente nell'unmarshaller
      //segnalando la presenza con un tag "FF" e null con "00"
      for(int index=0;index<cnt;index++) {
         Object element=Array.get(array, index);
         if (element==null) {
            //il contenuto di questo indice è NULL: inseriamo un indicatore byte "00"
            accumulator.add((byte)0x00);
         } else {
            //il contenuto di questo indice è presente: inseriamo un indicatore byte "FF" seguito
            //dalla serializzazione diretta senza passare dal databridge
            accumulator.add((byte)0xFF);
            ByteBuffer elemBuf=ctmar.marshalObject(element);
            accumulator.addWithSize(elemBuf);
         }
      }
      
      //Raccogliere tutti i risultati in un solo buffer
      return accumulator.gather();
   }
   
   @Override
   public Object unmarshal(ByteBuffer buffer) {
      //Prima di tutto: nome del marshaller da usare per i singoli elementi
      String ctMarName=Tools.bufferToString(buffer);
      
      //Otteniamo il marshaller corrispondente
      AbstractMarshaller<?> ctmar=getDataBridge().getMarshallerByName(ctMarName);
      
      //Otteniamo il numero di elementi contenuti nel buffer
      int cnt=buffer.getInt();
      
      //Prepariamoci ad accogliere gli elementi in un array generico
      //(ci servirà per ottenere il component type definitivo)
      Object[] elements=new Object[cnt];
      Class<?> foundComponentType=null;
      
      //Estraiamo gli elementi dal buffer
      for(int index=0;index<cnt;index++) {
         if (buffer.get()!=0) { //se l'elemento è null, saltiamo
            //Dimensione complessiva dell'elemento
            int elemLen=buffer.getInt();
            
            //slice del buffer (come farebbe il databridge)
            ByteBuffer elemBuf=buffer.slice().asReadOnlyBuffer();
            elemBuf.limit(elemLen);
            
            //Lettura dell'elemento
            elements[index]=ctmar.unmarshal(elemBuf);
            
            //portiamo avanti il buffer
            buffer.position(buffer.position()+elemLen);
            
            //rileviamo il component type
            if (foundComponentType==null) foundComponentType=elements[index].getClass();
            else if (!foundComponentType.isAssignableFrom(elements[index].getClass()))
               foundComponentType=Object.class; //caso particolare: l'array contiene oggetti incompatibili tra loro
         }
      }
      
      //Il buffer indicava un array vuoto. Fidiamoci della classe indicata nel marshaller.
      if (foundComponentType==null) foundComponentType=ctmar.getAffineClass();
      
      //Ricreiamo un array secondo il component type trovato
      //e riportiamo tutti gli elementi ricavati al suo interno
      Object array=Array.newInstance(foundComponentType, cnt);
      for(int index=0;index<cnt;index++) Array.set(array, index, elements[index]);
      
      return array;
   }
}
