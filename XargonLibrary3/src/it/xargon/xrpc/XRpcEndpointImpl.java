package it.xargon.xrpc;

import it.xargon.events.*;
import it.xargon.util.*;
import it.xargon.xmp.*;
import it.xargon.xrpc.actions.ActionsRegistry;
import it.xargon.xrpc.actions.NoSuchActionException;
import it.xargon.xrpc.messages.*;
import it.xargon.xrpc.messages.MsgObjectDescription.Flavor;
import it.xargon.marshal.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class XRpcEndpointImpl extends EventsSourceImpl<XRpcEndpoint.Events> implements XRpcEndpoint {
   private XmpConnection iconn=null;
   private XRpcFactoryInternal ifact=null;
   private DataBridge dataBridge=XRpcFactoryInternal.getDataBridge();
   private ActionsRegistry actionsRegistry=XRpcFactoryInternal.getActionsRegistry();
   private LocalObjectCache localObjectCache=null;
   private RemoteObjectCache remoteObjectCache=null;
   private HashSet<Class<?>> unremotableClasses=null;
   private HashMap<String, BooleanLatch> remoteObjectAwaiters=null;
   
   //Consente di delegare l'emissione di eventi ad agenti esterni
   //(vedi XRpcPubObjStatusChanged)
   public XRpcEndpoint.Events getEventRaiser() {return raiseEvent;}
   
   private XmpConnection.Events xmpEventsInterceptor=new XmpConnection.Events.Adapter() {
      public void disconnected(XmpConnection conn) {xmp_disconnected(conn);}
      
      public void processEvent(XmpConnection conn, byte[] contents) {
         Object message=dataBridge.unmarshal(contents);
         try {
            actionsRegistry.process(message, XRpcEndpointImpl.this);
         } catch (NoSuchActionException ignored) {
            //ignoriamo l'eventuale eccezione perché non c'è modo di restituire
            //una risposta al chiamante
         }
      }
      
      public byte[] processRequest(XmpConnection conn, byte[] contents) {
         Object message=dataBridge.unmarshal(contents);
         Object answer;
         try {
            answer = actionsRegistry.process(message, XRpcEndpointImpl.this);
         } catch (NoSuchActionException e) {
            return dataBridge.marshal(new XRpcRemoteException("Unrecognized BXMP message"));
         }
         return dataBridge.marshal(answer);
      }
   };
   
   private LocalObjectCache.Events localCacheEventsInterceptor=new LocalObjectCache.Events() {
      public void cacheDisposed(LocalObjectCache cache) {}

      public void objectUnavailable(LocalObjectCache cache, Identifier objId, Object obj) {
         if (!(iconn.isConnected() && iconn.isRunning())) return;
         MsgLocalObjectDeletion objevent=new MsgLocalObjectDeletion();
         objevent.target=objId;
         try {iconn.sendEvent(dataBridge.marshal(objevent));} catch (IOException ignored) {}
      }

      public void objectPublished(LocalObjectCache cache, String objName, Identifier objId) {
         if (!(iconn.isConnected() && iconn.isRunning())) return;
         MsgPubObjStatusChanged objevent=new MsgPubObjStatusChanged();
         objevent.objname=objName;
         objevent.published=true;
         try {iconn.sendEvent(dataBridge.marshal(objevent));} catch (IOException ignored) {}
      }

      public void objectUnpublished(LocalObjectCache cache, String objName, Identifier objId) {
         if (!(iconn.isConnected() && iconn.isRunning())) return;
         MsgPubObjStatusChanged objevent=new MsgPubObjStatusChanged();
         objevent.objname=objName;
         objevent.published=false;
         try {iconn.sendEvent(dataBridge.marshal(objevent));} catch (IOException ignored) {}
      }  
   };
   
   public XRpcEndpointImpl(XmpConnection conn, XRpcFactoryInternal fact) {
      iconn=conn;
      ifact=fact;
      unremotableClasses=new HashSet<Class<?>>();
      remoteObjectAwaiters=new HashMap<String, BooleanLatch>();
      localObjectCache=new LocalObjectCache();
      localObjectCache.register(localCacheEventsInterceptor);
      remoteObjectCache=new RemoteObjectCache(this);
      iconn.register(xmpEventsInterceptor);
   }
   
   private void xmp_disconnected(XmpConnection conn) {
      iconn.unregister(xmpEventsInterceptor);
      localObjectCache.dispose();
      localObjectCache.unregister(localCacheEventsInterceptor);
      Identifier[] allRemoteIds=remoteObjectCache.getAllObjectIds();
      for(Identifier id:allRemoteIds) {
         Object obj=remoteObjectCache.getCachedObjectById(id);
         raiseEvent.remoteObjectUnavailable(this, obj);
      }
      remoteObjectCache.dispose();
      raiseEvent.disconnected(this);
      unregisterAll();
   }
   
   public HashMap<String, Class<?>[]> getAllRemoteInstances() throws IOException {
      if (!(iconn.isConnected() && iconn.isRunning())) throw new IOException("Not connected");
      HashMap<String, Class<?>[]> result=new HashMap<String, Class<?>[]>();
      MsgPubObjsListAnswer xanswer=dataBridge.unmarshal(iconn.sendRequest(dataBridge.marshal(new MsgPubObjsListRequest())), MsgPubObjsListAnswer.class);
      HashMap<String, String[]> answer=xanswer.pubObjList;
      
      for(String objname:answer.keySet()) {
         String[] classNames=answer.get(objname);
         ArrayList<Class<?>> availClasses=new ArrayList<Class<?>>();
         for(int i=0;i<classNames.length;i++) {
            try {availClasses.add(Tools.getTypeForName(classNames[i]));}
            catch (ClassNotFoundException ignored) {/*le interfacce sconosciute vanno ignorate*/}
         }
         if (availClasses.size()>0) {
            //Solo oggetti che possiedono interfacce comuni tra locale e remoto
            //sono accessibili da qui
            Class<?>[] classes=availClasses.toArray(new Class<?>[availClasses.size()]);
            result.put(objname, classes);
         }
      }
      
      return result;
   }

   public HashMap<String, String[]> exploreAllRemoteInstances() throws IOException {
      //Tramite questa procedura sarà possibile conoscere anche il nome
      //di interfacce sconosciute al classloader locale
      MsgPubObjsListAnswer xanswer=dataBridge.unmarshal(iconn.sendRequest(dataBridge.marshal(new MsgPubObjsListRequest())), MsgPubObjsListAnswer.class);
      return xanswer.pubObjList;
   }
   
   public int getPublishedCount() {return localObjectCache.getAllPublishedNames().length;}

   public String[] getPublishedNames() {return localObjectCache.getAllPublishedNames();}

   @SuppressWarnings("unchecked")
   public <T> T getRemoteInstance(Class<T> javaInterface, String pubName) throws IOException {
      if (!(iconn.isConnected() && iconn.isRunning())) throw new IOException("Not connected");
      MsgPubObjRequest request=new MsgPubObjRequest();
      request.pubname=pubName;
      MsgPubObjAnswer answer=dataBridge.unmarshal(iconn.sendRequest(dataBridge.marshal(request)), MsgPubObjAnswer.class);
      if (answer.target==null) return null;
      return (T)remoteObjectCache.getCachedObjectById(answer.target);
   }

   public XmpConnection getXmpConnection() {return iconn;}

   public boolean isPublished(String pubName) {return localObjectCache.isObjectPublished(pubName);}

   public boolean isLocallyCached(Object obj) {
      return localObjectCache.isObjectCached(obj);
   }

   public boolean isRemotelyCached(Object obj) {
      if (!Proxy.isProxyClass(obj.getClass())) return false;
      return remoteObjectCache.isObjectCached(obj);
   }

   public boolean publish(String pubName, Object obj) {
      if (localObjectCache.isObjectPublished(pubName)) return false;
      Identifier objid=localObjectCache.getCachedObjectId(obj);
      if (localObjectCache.isObjectPublished(objid)) return false;
      localObjectCache.publishObject(objid, pubName);
      return true;
   }

   public boolean unpublish(String pubName) {
      if (!localObjectCache.isObjectPublished(pubName)) return false;
      localObjectCache.unpublishObject(pubName);
      return true;
   }

   public HashMap<String, BooleanLatch> getRemoteObjectAwaiters() {return remoteObjectAwaiters;}

   public <T> T waitRemoteInstance(Class<T> javaInterface, String pubName) throws IOException, InterruptedException {
      return waitRemoteInstance(javaInterface, pubName, 0);
   }

   public <T> T waitRemoteInstance(Class<T> javaInterface, String pubName, long timeout) throws IOException, InterruptedException {
      if (!(iconn.isConnected() && iconn.isRunning())) throw new IOException("Not connected");
      T result=null;
      BooleanLatch latch=null;
      
      synchronized (remoteObjectAwaiters) {
         result=getRemoteInstance(javaInterface, pubName);
         if (result!=null) return result;
         latch=new BooleanLatch();
         remoteObjectAwaiters.put(pubName, latch);
      }
      latch.await(timeout);
      result=getRemoteInstance(javaInterface, pubName);
      
      return result;
   }

   public boolean registerAndPublish(String pubName, Events obj) {
      if (isPublished(pubName)) return false;
      if (isRegistered(obj)) return false;
      Identifier objId=localObjectCache.getCachedObjectId(obj);
      localObjectCache.publishObject(objId, pubName);
      register(obj);
      return true;
   }

   public boolean unpublishAndUnregister(String pubName) {
      if (!isPublished(pubName)) return false;
      Object obj=localObjectCache.getCachedObjectById(localObjectCache.getPublishedObjectId(pubName));
      if (!(obj instanceof Events)) return false;
      Events sink=Events.class.cast(obj);
      if (!isRegistered(sink)) return false;
      localObjectCache.unpublishObject(pubName);
      unregister(sink);
      return true;
   }

   public boolean isRemotableClass(Class<?> cl) {
      if (Serializable.class.isAssignableFrom(cl)) return false;
      return !(unremotableClasses.contains(cl));
   }

   public void setRemotableClass(Class<?> cl, boolean remotable) {
      if (remotable) unremotableClasses.remove(cl);
      else unremotableClasses.add(cl);
   }
   
   public Object unmarshalObject(MsgObjectDescription objdesc) throws ClassNotFoundException {
      Object arg=null;
      
      switch (objdesc.flavor ) {
         case VOID: arg=null; break;
         case SERIALIZED:
            arg=dataBridge.unmarshal(objdesc.sercontents);
            break;
         case DEST_REF:
            arg=localObjectCache.getCachedObjectById(objdesc.objid);
            break;
         case SOURCE_REF:
            arg=remoteObjectCache.getCachedObjectById(objdesc.objid);
            break;
      }
      
      return arg;
   }
   
   public MsgObjectDescription marshalObject(Object arg, Class<?> expectedType, boolean isBySer, boolean isByRef) {
      MsgObjectDescription desc=new MsgObjectDescription();
      
      //  Se arg==null -> desc.setFlavor(VOID); return desc;
      if (arg==null) {desc.flavor=Flavor.VOID; return desc;}
      
      //  Controlla se è necessario forzare il sistema di marshaling
      if (isBySer && isByRef) {
         throw new IllegalArgumentException("Ambiguous marshalling type specified");
      } else if (isBySer && !isByRef) {
         //l'annotazione forza l'invio dell'oggetto come serializzato
         desc.flavor=Flavor.SERIALIZED;
         desc.sercontents=dataBridge.marshal(arg);
      } else if (!isBySer && isByRef) {
         //l'annotazione forza l'invio dell'oggetto come riferimento
         if (remoteObjectCache.isObjectCached(arg)) {
            //l'oggetto è nella cache remota (si tratta di riferimento remoto ricevuto in anticipo)
            Identifier objid=remoteObjectCache.getCachedObjectId(arg);
            desc.flavor=Flavor.DEST_REF;
            desc.objid=objid;
         } else {
            if (localObjectCache.isObjectCached(arg)) {
               //l'oggetto è nella cache locale
               LocalObjectWrapper objwrap=localObjectCache.getObjectWrapper(arg);
               desc.flavor=Flavor.SOURCE_REF;
               desc.objid=objwrap.getObjectId();
            } else {
               //l'oggetto non è in nessuna cache: creiamo il riferimento
               Identifier objid=localObjectCache.getCachedObjectId(arg);
               desc.flavor=Flavor.SOURCE_REF;
               desc.objid=objid;
            }
         }
      } else {
         //Se non c'è alcuna annotazione: distinguiamo se il metodo si aspetta
         //un Object o una classe specifica
         
         if (expectedType.equals(Object.class)) {
            //la firma specifica un Object come argomento (es. i metodi put dei container)
            
            //Se l'oggetto è già in qualche cache, manda il riferimento
            if (remoteObjectCache.isObjectCached(arg)) {
               //l'oggetto è nella cache remota 
               Identifier objid=remoteObjectCache.getCachedObjectId(arg);
               desc.flavor=Flavor.DEST_REF;
               desc.objid=objid;
            } else if (localObjectCache.isObjectCached(arg)) {
               //l'oggetto è nella cache locale
               LocalObjectWrapper objwrap=localObjectCache.getObjectWrapper(arg);
               desc.flavor=Flavor.SOURCE_REF;
               desc.objid=objwrap.getObjectId();
            } else {               
               //Altrimenti verifica contro la cache di oggetti locali se la
               //classe dell'argomento è remotizzabile
               if (isRemotableClass(arg.getClass())) {
                  //Si, è remotizzabile: crea un riferimento locale
                  Identifier objid=localObjectCache.getCachedObjectId(arg);
                  desc.flavor=Flavor.SOURCE_REF;
                  desc.objid=objid;
               } else {
                  //Non è remotizzabile: serializza tramite databridge
                  try {
                     desc.flavor=Flavor.SERIALIZED;
                     desc.sercontents=dataBridge.marshal(arg);
                  } catch (IllegalStateException ex) {
                     //Non è nemmeno serializzabile
                     throw new IllegalArgumentException("Unmarshallable object", ex);
                  }
               }
            }
         } else {
            //c'è una classe specifica nella firma, distinguiamo tra un'interfaccia o
            //una classe concreta
            if ((expectedType.isInterface()) && (Modifier.isPublic(expectedType.getModifiers()))) {
               //è un'interfaccia pubblica: manda il riferimento
               if (remoteObjectCache.isObjectCached(arg)) {
                  //l'oggetto è nella cache remota (si tratta di riferimento remoto ricevuto in anticipo)
                  Identifier objid=remoteObjectCache.getCachedObjectId(arg);
                  desc.flavor=Flavor.DEST_REF;
                  desc.objid=objid;
               } else {
                  if (localObjectCache.isObjectCached(arg)) {
                     //l'oggetto è nella cache locale
                     LocalObjectWrapper objwrap=localObjectCache.getObjectWrapper(arg);
                     desc.flavor=Flavor.SOURCE_REF;
                     desc.objid=objwrap.getObjectId();
                  } else {
                     //l'oggetto non è in nessuna cache: creiamo il riferimento
                     Identifier objid=localObjectCache.getCachedObjectId(arg);
                     desc.flavor=Flavor.SOURCE_REF;
                     desc.objid=objid;
                  }
               }
            } else {
               //è una classe concreta: serializza l'argomento
               try {
                  //Proviamo a vedere se args[x] è serializzabile
                  desc.flavor=Flavor.SERIALIZED;
                  desc.sercontents=dataBridge.marshal(arg);
               } catch (IllegalStateException ex) {
                  //Non è serializzabile
                  throw new IllegalArgumentException("Unmarshallable object", ex);
               }
            }
         }
      }
     
      return desc;
   }

   public XRpcFactory getFactory() {return ifact.getPublicFactory();}
   
   public XRpcFactoryInternal getInternalFactory() {return ifact;}
   
   public RemoteObjectCache getRemoteCache() {return remoteObjectCache;}
   
   public LocalObjectCache getLocalCache() {return localObjectCache;}
}
