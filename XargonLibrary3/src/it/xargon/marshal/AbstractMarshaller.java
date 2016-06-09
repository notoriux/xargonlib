package it.xargon.marshal;

import java.io.*;

public abstract class AbstractMarshaller {
   public enum Source {MEMORY,STREAM}
   
   private String iName=null;
   private Source iprefSource=null;
   private Class<?> iaffineClass=null;
   
   //Questo riferimento verrà iniettato in fase di registrazione sul databridge
   protected DataBridge dataBridge=null;

   protected AbstractMarshaller(String name, Source preferredSource, Class<?> affineClass) {
      if (name.length()>255) throw new IllegalArgumentException("Type name must not exceed 255 ascii characters");
      iName=name;
      if (preferredSource==null) throw new IllegalArgumentException("Must specify the preferred data source");
      iprefSource=preferredSource;
      iaffineClass=affineClass;
   }
   
   public final String getName() {return iName;}
   
   public final Source getPreferredSource() {return iprefSource;}
   
   public final Class<?> getAffineClass() {return iaffineClass;}
         
   public float getAffinity(Class<?> cl) {
      if (cl.equals(iaffineClass)) return 1.0f;
      return 0f;
   }
      
   public void marshalToStream(Object obj, OutputStream out) throws IOException {
      throw new UnsupportedOperationException();
   }
   
   public byte[] marshalToMemory(Object obj) {
      throw new UnsupportedOperationException();
   }
   
   public Object unmarshalFromStream(InputStream in) throws IOException {
      throw new UnsupportedOperationException();
   }
   
   public Object unmarshalFromMemory(byte[] contents) {
      throw new UnsupportedOperationException();
   }
}
