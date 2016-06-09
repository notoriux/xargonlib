package it.xargon.xrpc;

import java.io.IOException;
import java.lang.ref.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import it.xargon.util.Debug;
import it.xargon.util.Identifier;
import it.xargon.xmp.XmpConnection;
import it.xargon.xmp.XmpServer;
import it.xargon.xrpc.actions.ActionsRegistry;
import it.xargon.xrpc.marshallers.MarInvocation;
import it.xargon.xrpc.marshallers.MarInvocationAnswer;
import it.xargon.xrpc.marshallers.MarLocalObjectDeletion;
import it.xargon.xrpc.marshallers.MarObjectDescription;
import it.xargon.xrpc.marshallers.MarObjectRequest;
import it.xargon.xrpc.marshallers.MarPubObjAnswer;
import it.xargon.xrpc.marshallers.MarPubObjRequest;
import it.xargon.xrpc.marshallers.MarPubObjStatusChanged;
import it.xargon.xrpc.marshallers.MarPubObjsListAnswer;
import it.xargon.xrpc.marshallers.MarPubObjsListRequest;
import it.xargon.xrpc.marshallers.MarRemoteProxyFinalized;
import it.xargon.xrpc.messages.MsgRemoteProxyFinalized;
import it.xargon.marshal.*;

class XRpcFactoryInternal {
   private XRpcFactory publicFactory=null;
   private Thread refqueueCleanerThread=null;
   private ReferenceQueue<Object> gcQueue=null;
   private HashMap<SoftReference<Object>, RemoteObjectWrapper> allRemoteObjects=null;
   //private static ThreadLocal<XRpcEndpointImpl> currentEndpoint=new ThreadLocal<XRpcEndpointImpl>();
   private static DataBridge dataBridge=new DataBridge();
   private static ActionsRegistry actionsRegistry=new ActionsRegistry();
   
   static {
      dataBridge.installMarshaller(new MarInvocation());
      dataBridge.installMarshaller(new MarInvocationAnswer());
      dataBridge.installMarshaller(new MarLocalObjectDeletion());
      dataBridge.installMarshaller(new MarObjectDescription());
      dataBridge.installMarshaller(new MarObjectRequest());
      dataBridge.installMarshaller(new MarPubObjAnswer());
      dataBridge.installMarshaller(new MarPubObjRequest());
      dataBridge.installMarshaller(new MarPubObjsListAnswer());
      dataBridge.installMarshaller(new MarPubObjsListRequest());
      dataBridge.installMarshaller(new MarPubObjStatusChanged());
      dataBridge.installMarshaller(new MarRemoteProxyFinalized());      
   }
   
   private Runnable refqueueCleanerJob=new Runnable() {
      public void run() {
         try {
            internalRun();
         } catch (Throwable tr) {
            System.err.println("****** refqueueCleaner exception:" + Debug.exceptionToString(tr));            
         }
      }
      
      @SuppressWarnings("unchecked")
      private void internalRun() {
         boolean needStop=false;
         SoftReference<Object> ref=null;
         
         while(!needStop) {
            ref=null;
            try {ref=(SoftReference<Object>) gcQueue.remove();} catch (InterruptedException ex) {needStop=true;}
            if (ref!=null) {
               RemoteObjectWrapper wrapper=lookupWrapperByReference(ref);
               XRpcEndpointImpl iendpoint=wrapper.getEndpoint();
               Identifier objId=wrapper.getObjectId();
               MsgRemoteProxyFinalized message=new MsgRemoteProxyFinalized();
               message.target=objId;
               try {iendpoint.getXmpConnection().sendEvent(dataBridge.marshal(message));} catch (IOException ignored) {}
               synchronized (allRemoteObjects) {
                  allRemoteObjects.remove(ref);
                  iendpoint.getRemoteCache().removeCachedObject(objId);
               }
            }
            if (Thread.currentThread().isInterrupted()) needStop=true;
         }
      }
   };
   
   public XRpcFactoryInternal(XRpcFactory pubFact) {
      publicFactory=pubFact;
      gcQueue=new ReferenceQueue<Object>();
      allRemoteObjects=new HashMap<SoftReference<Object>, RemoteObjectWrapper>();
      refqueueCleanerThread=new Thread(refqueueCleanerJob, "ReferenceQueueCleaner");
      refqueueCleanerThread.setDaemon(true);
      refqueueCleanerThread.start();
   }

   private RemoteObjectWrapper lookupWrapperByReference(SoftReference<Object> ref) {
      synchronized (allRemoteObjects) {
         for(Map.Entry<SoftReference<Object>, RemoteObjectWrapper> entry: allRemoteObjects.entrySet()) {
            //Dato che stiamo cercando un oggetto specifico, e non un suo equivalente,
            //confrontiamo i puntatori senza usare equals (più sicuro)
            if (entry.getKey()==ref) return entry.getValue();
         }
      }
      return null;
   }
   
   public SoftReference<Object> enqueReference(Object iproxy, RemoteObjectWrapper wrapper) {
      SoftReference<Object> sr=null;
      
      synchronized (allRemoteObjects) {
         sr=new SoftReference<Object>(iproxy, gcQueue);
         allRemoteObjects.put(sr, wrapper);
      }

      return sr;
   }
   
   public XRpcEndpointImpl newEndpoint(XmpConnection conn) {
      return new XRpcEndpointImpl(conn, this);
   }
   
   public XRpcServerImpl newServer(XmpServer serv) {
      return new XRpcServerImpl(serv, this);
   }
   
   public XRpcFactory getPublicFactory() {return publicFactory;}
   
   public static DataBridge getDataBridge() {return dataBridge;}
   
   public static ActionsRegistry getActionsRegistry() {return actionsRegistry;}
   
   public static RemoteObjectWrapper getRemoteObjectWrapper(Object obj) {
      if (!Proxy.isProxyClass(obj.getClass())) return null;
      InvocationHandler ih=null;
      try {ih=Proxy.getInvocationHandler(obj);}
      catch (IllegalArgumentException ignored) {}
      if (ih==null) return null;
      if (!(ih instanceof RemoteObjectWrapper)) return null;
      return (RemoteObjectWrapper)ih;
   }
   
   /*public static void enterLocalInvocation(XRpcEndpointImpl endpoint) {
      currentEndpoint.set(endpoint);
   }

   public static void exitLocalInvocation() {
      currentEndpoint.remove();
   }
   
   public static XRpcEndpointImpl getEndpointForThread() {
      return currentEndpoint.get();
   }*/
}
