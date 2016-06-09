package it.xargon.xrpc;

import it.xargon.marshal.DataBridge;
import it.xargon.util.*;
import it.xargon.xrpc.messages.MsgObjectDescription;
import it.xargon.xrpc.messages.MsgObjectRequest;
import it.xargon.xrpc.messages.MsgObjectDescription.Flavor;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.*;

public class RemoteObjectCache {
   private HashMap<Identifier, RemoteObjectWrapper> cachedObjects=null;
   private XRpcEndpointImpl iendpoint=null;
   private DataBridge dataBridge=XRpcFactoryInternal.getDataBridge();
   
   private Identifier lookupIdByObject(Object obj) {
      synchronized (cachedObjects) {
         for(Map.Entry<Identifier, RemoteObjectWrapper> entry: cachedObjects.entrySet()) {
            //Dato che stiamo cercando un oggetto specifico, e non un suo equivalente,
            //confrontiamo i puntatori
            Object jproxy=entry.getValue().getJavaProxy();
            if (jproxy==obj) return entry.getKey();
         }
      }
      return null;
   }
      
   public RemoteObjectCache(XRpcEndpointImpl endpoint) {
      cachedObjects=new HashMap<Identifier, RemoteObjectWrapper>();
      iendpoint=endpoint;
   }
      
   public void dispose() {
      //refqueueCleanerThread.interrupt();
      //try {refqueueCleanerThread.join();} catch (InterruptedException ignored) {}
      synchronized (cachedObjects) {cachedObjects.clear();}
   }
         
   public Object getCachedObjectById(Identifier objId) {
      Object result=null;
      
      if (!cachedObjects.containsKey(objId)) {
         MsgObjectRequest request=new MsgObjectRequest();
         request.target=objId;
         MsgObjectDescription answer=null;
         try {answer=dataBridge.unmarshal(iendpoint.getXmpConnection().sendRequest(dataBridge.marshal(request)), MsgObjectDescription.class);}
         catch (IOException ex) {return null;}
         if (answer.flavor==Flavor.VOID) return null;
         result=buildObjectFromDescription(answer);
      } else {
         result=cachedObjects.get(objId).getJavaProxy();
      }
            
      return result;
   }
   
   private Object buildObjectFromDescription(MsgObjectDescription objdesc) {
      if (cachedObjects.containsKey(objdesc.objid)) throw new XRpcException("Remote object already cached");
      Object result=null;
      RemoteObjectWrapper objwrap=null;
      objwrap = new RemoteObjectWrapper(iendpoint, objdesc);
      synchronized (cachedObjects) {cachedObjects.put(objdesc.objid, objwrap);}
      result=objwrap.getJavaProxy();
      return result;
   }
   
   public RemoteObjectWrapper getRemoteObjectWrapper(Identifier objId) {
      return cachedObjects.get(objId);
   }
   
   public Identifier getCachedObjectId(Object obj) {
      if (!Proxy.isProxyClass(obj.getClass())) return null;
      return lookupIdByObject(obj);
   }
   
   public boolean isObjectCached(Object obj) {
      if (!Proxy.isProxyClass(obj.getClass())) return false;
      return (getCachedObjectId(obj)!=null);
   }
   
   public void removeCachedObject(Identifier objId) {
      if (!cachedObjects.containsKey(objId)) return;
      synchronized (cachedObjects) {cachedObjects.remove(objId);}
   }
   
   public Identifier[] getAllObjectIds() {
      synchronized (cachedObjects) {
         return cachedObjects.keySet().toArray(new Identifier[cachedObjects.size()]);
      }
   }
}
