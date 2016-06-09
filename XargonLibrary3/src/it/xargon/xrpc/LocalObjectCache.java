package it.xargon.xrpc;

import it.xargon.events.*;
import it.xargon.util.*;

import java.util.*;

public class LocalObjectCache extends EventsSourceImpl<LocalObjectCache.Events> {
   private HashMap<Identifier, LocalObjectWrapper> cachedObjects=null;
   private HashMap<String, Identifier> publishedObjects=null;
   private IdGenerator objidGen=null;
   
   @EventSink public interface Events {
      @Event public void objectPublished(LocalObjectCache cache, String objName, Identifier objId);
      @Event public void objectUnpublished(LocalObjectCache cache, String objName, Identifier objId);
      @Event public void objectUnavailable(LocalObjectCache cache, Identifier objId, Object obj);
      @Event public void cacheDisposed(LocalObjectCache cache);
   }
   
   public LocalObjectCache() {
      cachedObjects=new HashMap<Identifier, LocalObjectWrapper>();
      publishedObjects=new HashMap<String, Identifier>();
      objidGen=new IdGenerator();
   }
   
   public void dispose() {
      synchronized (publishedObjects) {
         String[] pubnames=publishedObjects.keySet().toArray(new String[publishedObjects.size()]);
         for(String pubname:pubnames) unpublishObject(pubname);
      }
      
      synchronized (cachedObjects) {
         Identifier[] objIds=cachedObjects.keySet().toArray(new Identifier[cachedObjects.size()]);
         for(Identifier id:objIds) uncacheObject(id);
      }
            
      raiseEvent.cacheDisposed(this);
   }
   
   private Identifier lookupIdByObject(HashMap<Identifier, LocalObjectWrapper> icache, Object obj) {
      synchronized (icache) {
         for(Map.Entry<Identifier, LocalObjectWrapper> entry: icache.entrySet()) {
            //Dato che stiamo cercando un oggetto specifico, e non un suo equivalente,
            //confrontiamo i puntatori
            if (entry.getValue().getWrappedObject()==obj) return entry.getKey();
         }
      }
      return null;
   }
   
   public Identifier getCachedObjectId(Object obj) {
      Identifier objid=lookupIdByObject(cachedObjects, obj);
      if (objid!=null) return objid;
      
      objid=objidGen.next();
      synchronized (cachedObjects) {
         LocalObjectWrapper objw=new LocalObjectWrapper(objid, obj);
         cachedObjects.put(objid, objw);
      }
      return objid;
   }
   
   public void uncacheObject(Identifier objId) {uncacheObject(objId, true);}
   
   public void uncacheObject(Identifier objId, boolean notify) { 
      if ((!cachedObjects.containsKey(objId)) || (isObjectPublished(objId))) return;
      Object obj=null;
      synchronized (cachedObjects) {
         LocalObjectWrapper objw=cachedObjects.get(objId);
         cachedObjects.remove(objId);
         objw.dispose();
      }
      if (notify) raiseEvent.objectUnavailable(this, objId, obj);
   }
   
   public void uncacheObject(Object obj) {
      if (!cachedObjects.containsValue(obj)) return;
      uncacheObject(lookupIdByObject(cachedObjects, obj));
   }
   
   public boolean isObjectCached(Identifier objId) {
      return cachedObjects.containsKey(objId);
   }
   
   public boolean isObjectCached(Object obj) {
      return lookupIdByObject(cachedObjects, obj)!=null;
   }
   
   public Object getCachedObjectById(Identifier objId) {
      if (!cachedObjects.containsKey(objId)) return null;
      return cachedObjects.get(objId).getWrappedObject();
   }
   
   public LocalObjectWrapper getObjectWrapper(Identifier objId) {
      if (!cachedObjects.containsKey(objId)) return null;
      return cachedObjects.get(objId);      
   }

   public LocalObjectWrapper getObjectWrapper(Object obj) {
      return cachedObjects.get(lookupIdByObject(cachedObjects, obj));
   }
   
   public Identifier[] getAllCachedObjectId() {
      HashSet<Identifier> result=new HashSet<Identifier>();
      result.addAll(cachedObjects.keySet());
      return result.toArray(new Identifier[result.size()]);
   }
      
   public void publishObject(Identifier objId, String objName) {
      if (isObjectPublished(objId)) throw new IllegalArgumentException("Another object published with the same name");
      if (isObjectPublished(objName)) throw new IllegalArgumentException("Object already published with another name");
      synchronized (publishedObjects) {
         publishedObjects.put(objName, objId);
      }
      raiseEvent.objectPublished(this, objName, objId);
   }
   
   public void unpublishObject(String objName) {
      if (!publishedObjects.containsKey(objName)) return;
      Identifier objId=publishedObjects.get(objName);
      synchronized (publishedObjects) {
         publishedObjects.remove(objName);
      }
      raiseEvent.objectUnpublished(this, objName, objId);
   }
   
   public void unpublishObject(Identifier objId) {
      if (!publishedObjects.containsValue(objId)) return;
      String objName=Tools.inverseLookup(publishedObjects, objId);
      synchronized (publishedObjects) {
         publishedObjects.remove(objName);
      }
      raiseEvent.objectUnpublished(this, objName, objId);
   }
   
   public boolean isObjectPublished(String objName) {
      return publishedObjects.containsKey(objName);
   }
   
   public boolean isObjectPublished(Identifier objId) {
      return publishedObjects.containsValue(objId);
   }
   
   public Identifier getPublishedObjectId(String objName) {
      return publishedObjects.get(objName);
   }
   
   public String getPublishedObjectName(Identifier objId) {
      return Tools.inverseLookup(publishedObjects, objId);
   }
   
   public String[] getAllPublishedNames() {
      HashSet<String> result=new HashSet<String>();
      result.addAll(publishedObjects.keySet());
      return result.toArray(new String[result.size()]);
   }
}
