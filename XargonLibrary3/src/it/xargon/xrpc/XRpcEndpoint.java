package it.xargon.xrpc;

import java.util.HashMap;
import java.io.*;

import it.xargon.events.*;
import it.xargon.xmp.*;

public interface XRpcEndpoint extends EventsSourceImpl<XRpcEndpoint.Events> {
   @EventSink public interface Events {
      public abstract class Adapter implements XRpcEndpoint.Events {
         public void disconnected(XRpcEndpoint endpoint) {}
         public void remoteObjectUnavailable(XRpcEndpoint endpoint, Object remObj) {}
         public void remotePublished(XRpcEndpoint endpoint, String pubname) {}
         public void remoteUnpublished(XRpcEndpoint endpoint, String pubname) {}
      }
      @Event public void remotePublished(XRpcEndpoint endpoint, String pubname);
      @Event public void remoteUnpublished(XRpcEndpoint endpoint, String pubname);
      @Event public void remoteObjectUnavailable(XRpcEndpoint endpoint, Object remObj);
      @Event public void disconnected(XRpcEndpoint endpoint);
   }
   
   public HashMap<String, Class<?>[]> getAllRemoteInstances() throws IOException;
   public HashMap<String, String[]> exploreAllRemoteInstances() throws IOException;
   public <T> T getRemoteInstance(Class<T> javaInterface, String pubName) throws IOException;
   public <T> T waitRemoteInstance(Class<T> javaInterface, String pubName) throws IOException, InterruptedException;
   public <T> T waitRemoteInstance(Class<T> javaInterface, String pubName, long timeout) throws IOException, InterruptedException;
   public XmpConnection getXmpConnection();
   public XRpcFactory getFactory();
   
   public boolean registerAndPublish(String pubName, XRpcEndpoint.Events obj);
   public boolean publish(String pubName, Object obj);
   public boolean isPublished(String pubName);
   public int getPublishedCount();
   public String[] getPublishedNames();
   public boolean unpublishAndUnregister(String pubName);
   public boolean unpublish(String pubName);
   
   public boolean isLocallyCached(Object obj);
   public boolean isRemotelyCached(Object obj);
   public void setRemotableClass(Class<?> cl, boolean remotable);
   public boolean isRemotableClass(Class<?> cl);
}
