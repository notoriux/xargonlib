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
   
   private void installBaseMarshallers() {
      installMarshallersFromPackage(DataBridge.class.getPackage().getName());
   }
   
   private final static Field dataBridgeField;
   static {
      Field tmp=null;
      try {
         tmp=AbstractMarshaller.class.getDeclaredField("dataBridge");
         tmp.setAccessible(true);
      } catch (NoSuchFieldException | SecurityException e) {
         throw new IllegalStateException(e); //This piece of code must always succeed, otherwise fail class loading
      }
      dataBridgeField=tmp;
   }
   
   @SuppressWarnings("unchecked")
   public void installMarshallersFromPackage(String pkg) {
      Class<?>[] allClassesInPackage=null;
      try {
         allClassesInPackage=Tools.getClasses(pkg);
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
         Constructor<? extends AbstractMarshaller<?>> marConstructor=marshallerClass.getConstructor();
         newMarshaller=marConstructor.newInstance();
         dataBridgeField.set(newMarshaller, this); //INJECTION
      } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
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
      AbstractMarshaller<?> mar=(obj==null)?mar=getBestMarshaller(Void.class):getBestMarshaller(obj.getClass());
      if (mar==null) throw new IllegalArgumentException("No suitable marshaller found");
      
      byte[] marName=mar.getEncName();
      ByteBuffer contents=mar.marshalObject(obj);
      
      ByteBuffer result=allocate(Integer.BYTES + marName.length +  Integer.BYTES + contents.remaining());
      result.putInt(marName.length);result.put(marName);
      result.putInt(contents.remaining()).put(contents).flip();
            
      return result;
   }

   public Object unmarshal(ByteBuffer contents) {return unmarshal(contents, Object.class);}
   
   public <T> T unmarshal(ByteBuffer contents, Class<T> expectedType) {
      if (contents==null) throw new NullPointerException();
      
      String marName=Tools.bufferToString(contents);
      AbstractMarshaller<?>mar=installedMarshallers.get(marName);
      
      if (mar==null) throw new IllegalArgumentException("Unknown marshaller in contents \"" + marName + "\"");
      if (!(Object.class.equals(expectedType)) && mar.getAffinity(expectedType)==0f)
         throw new IllegalArgumentException("Marshaller " + mar.getName() + " has nothing to do with \"" + expectedType.getName() + "\" class");
      
      int clen=contents.getInt();
      ByteBuffer objspec=contents.slice().asReadOnlyBuffer();
      objspec.limit(clen);
      contents.position(contents.position()+clen);
            
      return expectedType.cast(mar.unmarshal(objspec));
   }
}
