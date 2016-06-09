package it.xargon.xrpc;

import it.xargon.events.*;
import it.xargon.xmp.*;

import java.io.IOException;
import java.util.*;

class XRpcServerImpl extends EventsSourceImpl<XRpcServer.Events> implements XRpcServer {
   private class ServerObject {
      public boolean reg=false; //Se TRUE, l'oggetto verrà registrato su ogni client che si connette
      public boolean rew=false; //Se TRUE, è un oggetto in attesa di rewire
      public Object obj=null;    //oltre ad essere pubblicato
      
      public ServerObject(Object _obj, boolean _reg, boolean _rew) {obj=_obj;reg=_reg;rew=_rew;}
      public XRpcEndpoint.Events asSink() {
         if (!reg) return null;
         if (!(obj instanceof XRpcEndpoint.Events)) return null;
         return (XRpcEndpoint.Events)obj;
      }
   }

   private XRpcFactoryInternal ifact=null;
   private XmpServer iserv=null;
   private HashMap<String, ServerObject> serverObjectCache=null;
   private HashMap<XmpConnection, XRpcEndpoint> clients=null;
   
   private XmpServer.Events xmpServerEventsInterceptor=new XmpServer.Events.Adapter() {
      public void accepted(XmpConnection conn) {_xmp_accepted(conn);}
      public void removed(XmpConnection conn) {_xmp_removed(conn);}
      public void stopped(XmpServer serv) {_xmp_stopped();}
   };

   public XRpcServerImpl(XmpServer serv, XRpcFactoryInternal fact) {
      ifact=fact;
      serverObjectCache=new HashMap<String, ServerObject>();
      clients=new HashMap<XmpConnection, XRpcEndpoint>();
      iserv=serv;
      iserv.register(xmpServerEventsInterceptor);
   }
   
   private void _xmp_accepted(XmpConnection conn) {
      XRpcEndpoint endpoint=new XRpcEndpointImpl(conn, ifact);
      String[] pubnames=getPublishedNames();
      for(String pubname:pubnames) {
         //Non dovrebbero esserci errori: su un endpoint appena creato non esitono
         //oggetti pubblicati. Nulla toglie che gli oggetti pubblicati ad opera
         //del server possano essere rimossi o rimpiazzati più avanti
         try {
            ServerObject sobj=serverObjectCache.get(pubname);
            if (sobj.reg) endpoint.registerAndPublish(pubname, sobj.asSink());
            else endpoint.publish(pubname, sobj.obj);
         } catch (Exception ignored) {}
      }
      synchronized (clients) {clients.put(conn, endpoint);}
      try {
         conn.start();
         raiseEvent.accepted(this, endpoint);
      } catch (IOException ex) {
         raiseEvent.exception(this, ex);
      }      
   }
   
   private void _xmp_removed(XmpConnection conn) {
      XRpcEndpoint endpoint=clients.get(conn);
      synchronized (clients) {clients.remove(conn);}
      raiseEvent.removed(this, endpoint);
   }
   
   private void _xmp_stopped() {
      XRpcEndpoint[] endpoints=getAllEndpoints();
      for(XRpcEndpoint endpoint:endpoints) {
         XmpConnection xconn=endpoint.getXmpConnection();
         if (xconn.isRunning()) try {xconn.close();} catch (IOException ignored) {}
      }
      iserv.unregister(xmpServerEventsInterceptor);
   }
   
   public int getPublishedCount() {return serverObjectCache.size();}

   public String[] getPublishedNames() {return serverObjectCache.keySet().toArray(new String[serverObjectCache.size()]);}

   public boolean isPublished(String pubName) {return serverObjectCache.containsKey(pubName);}
   
   public boolean isRewiring(String pubName) {
      if (!isPublished(pubName)) return false;
      ServerObject sObj=serverObjectCache.get(pubName);
      return sObj.rew;
   }
   
   public boolean publish(String pubName, Object obj) {return publish(pubName, obj, false);}

   public boolean publishForRewire(String pubName, Object obj) {
      boolean result=publish(pubName, obj, false);
      ServerObject sObj=serverObjectCache.get(pubName);
      sObj.rew=true;
      return result;
   }
   
   public boolean publish(String pubName, Object obj, boolean register) {
      if ((register) && (!(obj instanceof XRpcEndpoint.Events))) return false;      
      XRpcEndpoint[] allEndpoints=clients.values().toArray(new XRpcEndpoint[clients.size()]);
      for(XRpcEndpoint endpoint:allEndpoints) if (endpoint.isPublished(pubName)) return false;
      ServerObject sObj=new ServerObject(obj, register, false);      
      serverObjectCache.put(pubName, sObj);
      for(XRpcEndpoint endpoint:allEndpoints) {
         if (register) endpoint.registerAndPublish(pubName, sObj.asSink());
         else endpoint.publish(pubName, obj);
      }
      return true;
   }

   public boolean unpublish(String pubName) {
      XRpcEndpoint[] allEndpoints=clients.values().toArray(new XRpcEndpoint[clients.size()]);      
      for(XRpcEndpoint endpoint:allEndpoints) {
         ServerObject sobj=serverObjectCache.remove(pubName);
         if (sobj!=null) {
            if (sobj.reg) endpoint.unpublishAndUnregister(pubName);
            else endpoint.unpublish(pubName);
         }
      }
      return true;
   }

   public XRpcEndpoint[] getAllEndpoints() {
      synchronized (clients) {return clients.values().toArray(new XRpcEndpoint[clients.size()]);}
   }
   
   public XmpServer getXmpServer() {return iserv;}
   
   public XRpcFactory getFactory() {return ifact.getPublicFactory();}
}
