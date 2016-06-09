package it.xargon.xrpc;

import it.xargon.events.*;
import it.xargon.xmp.XmpServer;

public interface XRpcServer extends EventsSourceImpl<XRpcServer.Events> {
   @EventSink public interface Events {
      public abstract class Adapter implements XRpcServer.Events {
         public void published(XRpcServer server, String pubName) {}
         public void unpublished(XRpcServer server, String pubName) {}
         public void accepted(XRpcServer server, XRpcEndpoint endpoint) {}
         public void removed(XRpcServer server, XRpcEndpoint endpoint) {}
         public void exception(XRpcServer server, Exception ex) {}
      }
      
      @Event public void published(XRpcServer server, String pubName);
      @Event public void unpublished(XRpcServer server, String pubName);
      @Event public void accepted(XRpcServer server, XRpcEndpoint endpoint);
      @Event public void removed(XRpcServer server, XRpcEndpoint endpoint);
      @Event public void exception(XRpcServer server, Exception ex);
   }
   
   public boolean publish(String pubName, Object obj);
   public boolean publish(String pubName, Object obj, boolean register);
   public boolean isPublished(String pubName);
   public int getPublishedCount();
   public String[] getPublishedNames();
   public boolean unpublish(String pubName);
   public XRpcEndpoint[] getAllEndpoints();
   public XmpServer getXmpServer();
   public XRpcFactory getFactory();
}
