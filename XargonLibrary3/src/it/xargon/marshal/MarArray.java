package it.xargon.marshal;

import java.io.*;
import java.lang.reflect.Array;

import it.xargon.util.*;

public class MarArray extends AbstractMarshaller {
   public MarArray() {super("ARRAY", Source.STREAM, Object.class);}
   
   public float getAffinity(Class<?> javaclass) {
      if (javaclass.isArray()) return 0.8f;
      return 0f;
   }
   
   public void marshalToStream(Object obj, OutputStream out) throws IOException {
      //marshaller del componentType
      AbstractMarshaller ctmar=dataBridge.getBestMarshaller(Tools.getBaseComponentType(obj));
      
      //Scriviamo il nome del marshaller sullo stream
      out.write(ctmar.getName().length());
      out.write(Bitwise.asciiStringToByteArray(ctmar.getName()));
            
      int[] dims=Tools.getArrayDimensions(obj);
      //numero di dimensioni
      out.write(dims.length);
      //grandezza di ogni dimensione
      for(int dim:dims) out.write(Bitwise.intToByteArray(dim));
      
      //Caso particolare: array con una sola dimensione di lunghezza zero
      //Non procedere con ulteriore marshaling
      if ((dims.length==1) && (dims[0]==0)) return;
      
      //marshal di ogni elemento dell'array, comprensivo di
      //proprio marshaller, dimensione e contenuto
      int[] indexes=new int[dims.length];
      
      while (indexes!=null) {
         Object arrayValue=Tools.getArrayValue(obj, indexes);
         if (arrayValue==null) {
            //il contenuto di questo indice è NULL
            out.write(0x00);
         } else {
            //il contenuto di questo indice è presente
            out.write(0xFF);
            dataBridge.marshal(arrayValue, true, out);
         }
         indexes=Tools.nextArrayIndex(indexes, dims);
      }
      out.flush();
   }
   
   public Object unmarshalFromStream(InputStream in) throws IOException {
      //Leggiamo il nome del marshaller per il component type
      int nlen=in.read();
      byte[] bmarname=new byte[nlen];
      Tools.forceRead(in, bmarname, false);
      String marname=Bitwise.byteArrayToAsciiString(bmarname);
      
      //Otteniamo il marshaller corrispondente
      AbstractMarshaller ctmar=dataBridge.getMarshallerByName(marname);

      //Numero di dimensioni contenute nell'array
      int dimcnt=in.read();
      int[] dims=new int[dimcnt];

      //Grandezza di ogni dimensione
      for(int cnt=0;cnt<dimcnt;cnt++) dims[cnt]=Bitwise.readInt(in);
      int[] indexes=new int[dimcnt];
      
      //Prepariamo la destinazione
      Object tarray=Array.newInstance(ctmar.getAffineClass(), dims);
            
      //Nel caso in cui l'array abbia una sola dimensione di lunghezza zero
      //non vi sono altri dati, e l'oggetto va restituito così com'è
      
      if (!((dims.length==1) && (dims[0]==0))) {
         while (indexes!=null) {
            int nullcheck=in.read();
            if (nullcheck!=0x00) {
               Object arrayValue=dataBridge.unmarshal(in, ctmar.getAffineClass());
               Tools.setArrayValue(tarray, indexes, arrayValue);
            }
            indexes=Tools.nextArrayIndex(indexes, dims);
         }
      }

      return tarray;
   }
}
