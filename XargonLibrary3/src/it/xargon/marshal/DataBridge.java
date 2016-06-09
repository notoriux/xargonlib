package it.xargon.marshal;

import it.xargon.util.Bitwise;
import it.xargon.util.Tools;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

public class DataBridge {
   private LinkedHashMap<String, AbstractMarshaller> installedMarshallers=null;
   private static Field dataBridgeField=null;
   
   static {      
      try {
         dataBridgeField=AbstractMarshaller.class.getDeclaredField("dataBridge");
         dataBridgeField.setAccessible(false);
      } catch (NoSuchFieldException e) {
         e.printStackTrace();
      } catch (SecurityException e) {
         e.printStackTrace();
      }
   }
   
   public DataBridge() {
      installedMarshallers=new LinkedHashMap<String, AbstractMarshaller>();
      installBaseMarshallers();
   }
   
   private void installBaseMarshallers() {
      installMarshaller(new MarVoid());
      installMarshaller(new MarPrimBoolean());
      installMarshaller(new MarPrimByte());
      installMarshaller(new MarPrimCharacter());
      installMarshaller(new MarPrimShort());
      installMarshaller(new MarPrimInteger());
      installMarshaller(new MarPrimLong());
      installMarshaller(new MarPrimFloat());
      installMarshaller(new MarPrimDouble());
      installMarshaller(new MarBoolean());
      installMarshaller(new MarByte());
      installMarshaller(new MarCharacter());
      installMarshaller(new MarShort());
      installMarshaller(new MarInteger());
      installMarshaller(new MarLong());
      installMarshaller(new MarFloat());
      installMarshaller(new MarDouble());
      installMarshaller(new MarString());
      installMarshaller(new MarIdentifier());
      installMarshaller(new MarPrimByteArray());
      installMarshaller(new MarArray());
      installMarshaller(new MarKVPair());
      installMarshaller(new MarMap());
      installMarshaller(new MarList());
      installMarshaller(new MarSet());
      installMarshaller(new MarSerialized());
   }
   
   public void installMarshaller(AbstractMarshaller marshaller) {
      String mname=marshaller.getName();
      if (installedMarshallers.containsKey(mname))
         throw new IllegalArgumentException("Marshaller " + mname + " already registered");
      
      //Usiamo reflection + accessibility per iniettare la dipendenza
      try {
         dataBridgeField.set(marshaller, this);
      } catch (Exception e) {
         throw new IllegalStateException(e);
      }
      
      installedMarshallers.put(mname, marshaller);
   }
   
   public AbstractMarshaller getBestMarshaller(Class<?> javaClass) {
      AbstractMarshaller curMar=null;
      float maxAff=0f;
      float curAff=maxAff;
            
      for(Map.Entry<String, AbstractMarshaller> entry:installedMarshallers.entrySet()) {
         AbstractMarshaller mar=entry.getValue();
         curAff=mar.getAffinity(javaClass);
         if ((curAff>0.0) && (curAff>=maxAff)) {
            maxAff=curAff;
            curMar=mar;
         }
      }
      
      return curMar;
   }
   
   public AbstractMarshaller getMarshallerByName(String name) {
      AbstractMarshaller result=installedMarshallers.get(name);
      if (result==null) throw new IllegalArgumentException("Unknown marshaller \"" + name + "\"");
      return result;
   }
   
   public byte[] marshal(Object obj) {return marshal(obj, false);}
   
   public byte[] marshal(Object obj, boolean dropTypeName) {
      AbstractMarshaller mar=(obj==null)?mar=getBestMarshaller(Void.class):getBestMarshaller(obj.getClass());
      if (mar==null) throw new IllegalArgumentException("No suitable marshaller found");
      
      byte[] marName=dropTypeName?new byte[0]:Bitwise.asciiStringToByteArray(mar.getName());
      byte[] contents=marshal(obj, mar);
      byte[] result=new byte[marName.length + contents.length + 5];
      
      result[0]=Bitwise.asByte(marName.length);
      System.arraycopy(marName, 0, result, 1, marName.length);
      System.arraycopy(Bitwise.intToByteArray(contents.length), 0, result, marName.length+1, 4);
      System.arraycopy(contents, 0, result, marName.length+5, contents.length);            
            
      return result;
   }
   
   public void marshal(Object obj,OutputStream out) throws IOException {marshal(obj, false, out);}

   public void marshal(Object obj, boolean dropTypeName, OutputStream out) throws IOException {
      if (obj==null || out==null) throw new NullPointerException();
      AbstractMarshaller mar=getBestMarshaller(obj.getClass());
      if (mar==null) throw new IllegalArgumentException("No suitable marshaller found");

      byte[] marName=dropTypeName?new byte[0]:Bitwise.asciiStringToByteArray(mar.getName());
      
      out.write(Bitwise.asByte(marName.length));
      out.write(marName);
      
      byte[] contents=marshal(obj, mar);

      out.write(Bitwise.intToByteArray(contents.length));
      out.write(contents);
      out.flush();
   }
   
   private byte[] marshal(Object obj, AbstractMarshaller mar) {
      byte[] contents=null;
      
      switch (mar.getPreferredSource()) {
         case MEMORY:
            contents=mar.marshalToMemory(obj);
            break;
         case STREAM:
            ByteArrayOutputStream bos=new ByteArrayOutputStream();
            try {
               mar.marshalToStream(obj, bos);
               bos.flush();bos.close();
            } catch (IOException ignored) {}
            contents=bos.toByteArray();
            break;
      }
            
      return contents;
   }
   
   public Object unmarshal(byte[] contents) {return unmarshal(contents, Object.class);}
   
   public <T> T unmarshal(byte[] contents, Class<T> expectedType) {
      if (contents==null) throw new NullPointerException();
      
      String marName=null;
      AbstractMarshaller mar=null;
      
      int nlen=Bitwise.asInt(contents[0]);
      if (nlen>0) {
         byte[] bmarName=new byte[nlen];
         System.arraycopy(contents, 1, bmarName, 0, nlen);
         marName=Bitwise.byteArrayToAsciiString(bmarName);
         mar=installedMarshallers.get(marName);
         if (mar==null) throw new IllegalArgumentException("Unknown marshaller in stream \"" + marName + "\"");
         if (!(expectedType.equals(Object.class)) && mar.getAffinity(expectedType)==0f)
            throw new IllegalArgumentException(mar.getName() + " has nothing to do with \"" + expectedType.getName() + "\" class");
      } else {
         mar=expectedType.equals(Object.class)?null:getBestMarshaller(expectedType);
         if (mar==null) throw new IllegalArgumentException("No suitable marshaller for \"" + expectedType.getName() + "\" class and no one was specified in the stream");
      }

      int clen=Bitwise.sequenceToInt(contents[nlen+1], contents[nlen+2], contents[nlen+3], contents[nlen+4]);
      
      byte[] objspec=new byte[clen];
      System.arraycopy(contents, nlen+5, objspec, 0, objspec.length);
      
      return expectedType.cast(unmarshal(objspec, mar));
   }
   
   public Object unmarshal(InputStream in) throws IOException {return unmarshal(in, Object.class);}
   
   public <T> T unmarshal(InputStream in, Class<T> expectedType) throws IOException {
      if (in==null) throw new NullPointerException();
      
      String marName=null;
      AbstractMarshaller mar=null;

      int nlen=in.read();
      if (nlen==-1) throw new IllegalArgumentException("Malformed contents: end of stream");
      
      if (nlen>0) {
         marName=Bitwise.readAsciiString(nlen, in);
         mar=installedMarshallers.get(marName);
         if (mar==null) throw new IllegalArgumentException("Unknown marshaller in stream \"" + marName + "\"");
         if (!(expectedType.equals(Object.class)) && mar.getAffinity(expectedType)==0f)
            throw new IllegalArgumentException(mar.getName() + " has nothing to do with \"" + expectedType.getName() + "\" class");
      } else {
         mar=expectedType.equals(Object.class)?null:getBestMarshaller(expectedType);
         if (mar==null) throw new IllegalArgumentException("No suitable marshaller for \"" + expectedType.getName() + "\" class and no one was specified in the stream");
      }
      
      int clen=Bitwise.readInt(in);
      byte[] contents=new byte[clen];
      Tools.forceRead(in, contents, false);
      
      return expectedType.cast(unmarshal(contents, mar));
   }
   
   private Object unmarshal(byte[] contents, AbstractMarshaller mar) {
      Object result=null;
      
      switch (mar.getPreferredSource()) {
         case MEMORY:
            result=mar.unmarshalFromMemory(contents);
            break;
         case STREAM:
            ByteArrayInputStream bis=new ByteArrayInputStream(contents);
            try {result=mar.unmarshalFromStream(bis);bis.close();}
            catch (IOException ignored) {}
            break;
      }
      
      return result;
   }
}
