package it.xargon.niomarshal;

import it.xargon.util.Tools;

import java.util.*;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.ByteBuffer;

public class DataBridge {
   private LinkedHashMap<String, AbstractMarshaller<?>> installedMarshallers=null;
   private boolean directMemory=false;
      
   public DataBridge(boolean directMemory) {
      this.directMemory=directMemory;
      installedMarshallers=new LinkedHashMap<String, AbstractMarshaller<?>>();
      installBaseMarshallers();
   }
   
   ByteBuffer allocate(int capacity) {
      return directMemory?ByteBuffer.allocateDirect(capacity):ByteBuffer.allocate(capacity);
   }
   
   @SuppressWarnings("unchecked")
   private void installBaseMarshallers() {
      Class<?>[] allClassesInPackage=null;
      try {
         allClassesInPackage=Tools.getClasses(DataBridge.class.getPackage().getName());
      } catch (ClassNotFoundException | IOException e) {
         throw new IllegalStateException(e);
      }
      
      for (Class<?> testClass:allClassesInPackage) {
         if (AbstractMarshaller.class.isAssignableFrom(testClass) && !Modifier.isAbstract(testClass.getModifiers()))
            installMarshaller((Class<? extends AbstractMarshaller<?>>) testClass);
      }
   }
   
   public void installMarshaller(Class<? extends AbstractMarshaller<?>> marshallerClass) {
      AbstractMarshaller<?> newMarshaller=null;

      try {
         Constructor<? extends AbstractMarshaller<?>> marConstructor=marshallerClass.getConstructor(DataBridge.class);
         newMarshaller=marConstructor.newInstance(this);
         Field dataBridgeField=marshallerClass.getDeclaredField("dataBridge");
         dataBridgeField.setAccessible(true);
         dataBridgeField.set(newMarshaller, this);
      } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchFieldException e) {
         throw new IllegalArgumentException(e);
      }
      
      String mname=newMarshaller.getName();
      if (installedMarshallers.containsKey(mname))
         throw new IllegalArgumentException("Marshaller " + mname + " already registered");
      
      installedMarshallers.put(mname, newMarshaller);
   }
   
   @SuppressWarnings("unchecked")
   public <T> AbstractMarshaller<T> getBestMarshaller(Class<T> javaClass) {
      AbstractMarshaller<T> curMar=null;
      float maxAff=0f;
      float curAff=maxAff;
            
      for(Map.Entry<String, AbstractMarshaller<?>> entry:installedMarshallers.entrySet()) {
         AbstractMarshaller<?> mar=entry.getValue();
         curAff=mar.getAffinity(javaClass);
         if ((curAff>0.0) && (curAff>=maxAff)) {
            maxAff=curAff;
            curMar=(AbstractMarshaller<T>) mar;
         }
      }
      
      return curMar;
   }
   
   public AbstractMarshaller<?> getMarshallerByName(String name) {
      AbstractMarshaller<?> result=installedMarshallers.get(name);
      if (result==null) throw new IllegalArgumentException("Unknown marshaller \"" + name + "\"");
      return result;
   }
   
   public ByteBuffer marshal(Object obj) {
      return marshal(obj, false);
   }
   
   ByteBuffer marshal(Object obj, boolean dropMarshallerName) {
      AbstractMarshaller<?> mar=(obj==null)?mar=getBestMarshaller(Void.class):getBestMarshaller(obj.getClass());
      if (mar==null) throw new IllegalArgumentException("No suitable marshaller found");
      
      byte[] marName=mar.getEncName();
      ByteBuffer contents=mar.marshalObject(obj);
      
      ByteBuffer result=allocate((dropMarshallerName?Integer.BYTES:(Integer.BYTES + marName.length)) +  Integer.BYTES + contents.remaining());
      result.putInt(marName.length);
      if (!dropMarshallerName) result.put(marName);
      result.putInt(contents.remaining()).put(contents);
            
      return result;
   }
   
   //In questa forma ci si aspetta che il tipo di dato sia contenuto nell'intestazione
   public Object unmarshal(ByteBuffer contents) {return unmarshal(contents, null);}
   
   public <T> T unmarshal(ByteBuffer contents, Class<T> expectedType) {
      if (contents==null) throw new NullPointerException();
      
      String marName=null;
      AbstractMarshaller<?> mar=null;
      
      int nlen=contents.getInt();
      if (nlen>0) {
         byte[] bmarName=new byte[nlen];
         contents.get(bmarName);
         marName=new String(bmarName);
         mar=installedMarshallers.get(marName);
         if (mar==null) throw new IllegalArgumentException("Unknown marshaller in contents \"" + marName + "\"");
         
         if (expectedType!=null && mar.getAffinity(expectedType)==0f)
            throw new IllegalArgumentException("Marshaller " + mar.getName() + " has nothing to do with \"" + expectedType.getName() + "\" class");
      } else {
         if (expectedType==null)
            throw new IllegalArgumentException("Contents have any marshaller indication, please tell me how to treat them via \"expectedType\" argument");
         mar=getBestMarshaller(expectedType);
         if (mar==null) throw new IllegalArgumentException("No suitable marshaller for \"" + expectedType.getName() + "\" class and no one was specified in the contents");
      }

      int clen=contents.getInt();
      ByteBuffer objspec=contents.slice().asReadOnlyBuffer();
      objspec.limit(objspec.position()+clen);
            
      return expectedType.cast(mar.unmarshal(objspec));
   }
}
