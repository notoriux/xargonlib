package it.xargon.xrpc;

import it.xargon.xmp.*;
import it.xargon.xrpc.messages.MsgObjectDescription;

import java.lang.reflect.*;

public class XRpcFactory {
   private static XRpcFactory defaultFactory=null;
   private XRpcFactoryInternal ifact=null;

   public XRpcFactory() {
      ifact=new XRpcFactoryInternal(this);
   }
   
   public static XRpcFactory getDefaultFactory() {
      if (defaultFactory==null) defaultFactory=new XRpcFactory();
      return defaultFactory;
   }
   
   /**
    * Creates a new BRPC endpoint from an existing BXMP connection.
    * BRPC protocol is just a set of BXMP messages crafted expecially to convey RPC informations between two remote endpoints.
    * The underlying BXMP connection can still be used to exchange other kind of messages without disrupting RPC references
    * and internal states. 
    * @param conn an existing and started BXMP connection
    * @return
    */
   public XRpcEndpoint newEndpoint(XmpConnection conn) {return ifact.newEndpoint(conn);}
   
   /**
    * Creates a new BRPC server from an existing BXMP server.
    * @param serv an existing and started BXMP server
    * @return
    */
   public XRpcServer newServer(XmpServer serv) {return ifact.newServer(serv);}
   
   /**
    * Tests if the specified object is a BRPC remoted object. It merely checks if the object owns a BRPC endpoint.
    * @param obj a possibly remote object
    * @return <code>true</code> if it is a remoted object
    */
   public static boolean isRemoteObject(Object obj) {
      return (getEndpoint(obj)!=null);
   }
   
   /**
    * If the specified object is a BRPC remoted object, it returns its endpoint.
    * @param obj a possibly remote object
    * @return the referenced endpoint
    */
   public static XRpcEndpoint getEndpoint(Object obj) {
      return XRpcFactoryInternal.getRemoteObjectWrapper(obj).getEndpoint();
   }
   
   //public static XRpcEndpoint getCallingEndpoint() {return XRpcFactoryInternal.getEndpointForThread();}
   
   public static Object invokeDirect(Object obj, Method method, Object... args) throws Throwable {
      RemoteObjectWrapper objw=XRpcFactoryInternal.getRemoteObjectWrapper(obj);
      if (objw==null) throw new IllegalArgumentException("Specified object is not a remote object");      
      return objw.invoke(method, args);
   }
   
   public static Object invokeDirect(Object obj, String targetClassName, String targetMethodName, String[] signatureClasses, Object[] args, boolean async) throws Throwable {
      RemoteObjectWrapper objw=XRpcFactoryInternal.getRemoteObjectWrapper(obj);
      if (objw==null) throw new IllegalArgumentException("Specified object is not a remote object");
      
      XRpcEndpointImpl iendpoint=objw.getEndpoint();
      
      //Con invocazione diretta è necessario convertire gli argomenti in BXmpObjectDescription manualmente
      //Date le evidenti limitazioni di non avere nel proprio classloader le classi necessarie a
      //capire se si tratta di interfacce o meno, supponiamo che le classi degli argomenti siano tutti
      //oggetti concreti e serializzabili
      
      MsgObjectDescription[] xmpargs=new MsgObjectDescription[(args==null)?0:args.length];
      
      for(int iarg=0;iarg<xmpargs.length;iarg++) {
         xmpargs[iarg]=iendpoint.marshalObject(args[iarg], null, true, false);
      }
      
      return objw.sendInvocation(targetClassName, targetMethodName, signatureClasses, xmpargs, async);
   }
}
