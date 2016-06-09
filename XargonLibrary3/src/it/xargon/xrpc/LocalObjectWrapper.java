package it.xargon.xrpc;

import java.lang.reflect.*;
import java.util.*;

import it.xargon.util.*;

public class LocalObjectWrapper {
   private Identifier objectId=null;
   private Class<?>[] interfaces=null;
   private Object wrappedObj=null;
   
   public LocalObjectWrapper(Identifier objId, Object obj) {
      objectId=objId;
      wrappedObj=obj;
      interfaces=extractAllInterfaces(wrappedObj);
      if (interfaces==null) {
         String etx="Object has no publishable interfaces!\n";
         etx+="toString: " + obj.toString() + "\n";
         etx+="getClass: " + obj.getClass().getName();
         throw new IllegalStateException(etx);
      }
   }
   
   private static Class<?>[] extractAllInterfaces(Object ref) {
      if (ref==null) throw new IllegalArgumentException();
      HashSet<Class<?>> ifaces=new HashSet<Class<?>>();
      Class<?> scanning=ref.getClass();
      while (scanning!=null) {
         Class<?>[] scanned=scanning.getInterfaces();
         for(Class<?> iface:scanned) {
            if (Modifier.isPublic(iface.getModifiers())) ifaces.add(iface);
         }
         scanning=scanning.getSuperclass();
      }
      if (ifaces.size()==0) return null;
      return ifaces.toArray(new Class<?>[ifaces.size()]);
   }
   
   public Object getWrappedObject() {return wrappedObj;}
   
   public Identifier getObjectId() {return objectId;}
   
   public void dispose() {
      objectId=null;
      interfaces=null;
      wrappedObj=null;
   }
   
   public Class<?>[] getInterfaces() {return Arrays.copyOf(interfaces, interfaces.length);}
   
   public String[] getInterfaceNames() {
      String[] result=new String[interfaces.length];
      for(int i=0;i<interfaces.length;i++) result[i]=interfaces[i].getName();
      return result;
   }
}
