package it.xargon.niomarshal;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.stream.Collectors;

import it.xargon.util.*;

//component type (lunghezza nome, nome)
//numero di dimensioni
//per ogni dimensione: numero di elementi
//grandezza,contenuto
//grandezza,contenuto
//grandezza,contenuto
//...
//
//L'array scritto nel flusso è "appiattito": viene incrementato solo l'ultimo indice,
//incrementando quello di livello superiore se l'ultimo indice supera il limite
//
// Esempio [5][3]:
//
// a b c d e
// f g h i j
// k l m n o
//
// viene emesso come:
// a f k b g l c h m d i n e j o
//
// Su 3 dimensioni: [4][3][2]
//
//     /m n o p
//    / q r s t
//   /  u v w x
//  /        /
// a b c d  /
// e f g h /
// i j k l/
//
// viene emesso come:
// a m e q i u b n f r j v c o g s k w d p h t l x

public class MarArray extends AbstractMarshaller<Object> {
   public MarArray() {super("ARRAY");}
   
   public float getAffinity(Class<?> javaclass) {
      if (javaclass.isArray()) return 0.8f;
      return 0f;
   }
   
   @Override
   public ByteBuffer marshal(Object obj) {
      ArrayList<ByteBuffer> elements=new ArrayList<>();
      
      //marshaller del componentType
      AbstractMarshaller<?> ctmar=getDataBridge().getBestMarshaller(Tools.getBaseComponentType(obj));
      
      //serializzare il nome del marshaller
      ByteBuffer ctbuf=alloc(Integer.BYTES + ctmar.getEncName().length);
      ctbuf.putInt(ctmar.getEncName().length).put(ctmar.getEncName()).flip();
      elements.add(ctbuf);
      
      //serializzare il numero di dimensioni, e la grandezza di ogni dimensione
      int[] dims=Tools.getArrayDimensions(obj);
      ByteBuffer dimbuf=alloc(Integer.BYTES * (dims.length+1));
      dimbuf.asIntBuffer().put(dims.length).put(dims).flip();
      elements.add(dimbuf);
   
      //marshal di ogni elemento dell'array (grandezza/contenuto)
      //se vi è una sola dimensione di lunghezza zero: finire qui
      int[] indexCounters=(dims.length==1 && dims[0]==0)?null:new int[dims.length];
      
      while (indexCounters!=null) {
         Object arrayValue=Tools.getArrayValue(obj, indexCounters);
         ByteBuffer elemBuf=null;
         if (arrayValue==null) {
            //il contenuto di questo indice è NULL: inseriamo un indicatore byte "0"
            elemBuf=alloc(1).put((byte)0);
            elemBuf.flip();
            elements.add(elemBuf);
         } else {
            //il contenuto di questo indice è presente: inseriamo un indicatore byte "1" seguito
            //dalla serializzazione senza il nome del marshaller
            elemBuf=alloc(1).put((byte)1);
            elemBuf.flip();
            elemBuf=getDataBridge().marshal(arrayValue, true);
            elements.add(elemBuf);
         }
         indexCounters=Tools.nextArrayIndex(indexCounters, dims);
      }
      
      //Raccogliere tutti i risultati in un solo buffer
      int totalSize=elements.stream().collect(Collectors.summingInt(ByteBuffer::remaining));
      ByteBuffer result=alloc(totalSize);
      elements.forEach(result::put);
      
      return result;
   }
   
   @Override
   public Object unmarshal(ByteBuffer buffer) {
      //Leggiamo il nome del marshaller per il component type
      int ctlen=buffer.getInt();
      byte[] bmarname=new byte[ctlen];
      buffer.get(bmarname);
      String ctMarName=Bitwise.byteArrayToAsciiString(bmarname);
      
      //Otteniamo il marshaller corrispondente
      AbstractMarshaller<?> ctmar=getDataBridge().getMarshallerByName(ctMarName);
      
      //Numero di dimensioni contenute nell'array
      int dimcnt=buffer.getInt();
      int[] dims=new int[dimcnt];
      
      //Grandezza di ogni dimensione
      buffer.asIntBuffer().get(dims);
      
      //Contatori per la rilettura negli indici...
      int[] indexCounters=new int[dimcnt];
      
      //Prepariamo la destinazione
      Object tarray=Array.newInstance(ctmar.getAffineClass(), dims);
      
      //Nel caso in cui l'array abbia una sola dimensione di lunghezza zero
      //non vi sono altri dati, e l'oggetto va restituito così com'è
      if (dims.length==1 && dims[0]==0) return tarray;
      
      //Altrimenti reinseriamo tutti gli elementi nella loro posizione originale
      if (!((dims.length==1) && (dims[0]==0))) {
         while (indexCounters!=null) {
            if (buffer.get()!=0) { //saltiamo i null
               Object arrayValue=getDataBridge().unmarshal(buffer, ctmar.getAffineClass());
               Tools.setArrayValue(tarray, indexCounters, arrayValue);
            }
            
            indexCounters=Tools.nextArrayIndex(indexCounters, dims);
         }
      }

      return tarray;
   }
}
