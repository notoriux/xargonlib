package it.xargon.xrpc;

import it.xargon.marshal.DataBridge;
import it.xargon.util.*;
import it.xargon.xmp.*;
import it.xargon.xrpc.messages.MsgInvocation;
import it.xargon.xrpc.messages.MsgInvocationAnswer;
import it.xargon.xrpc.messages.MsgObjectDescription;

import java.lang.ref.*;
import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.ArrayList;

class RemoteObjectWrapper implements InvocationHandler {
   private Identifier remObjectId=null;
   private Class<?>[] ifaces=null;
   private SoftReference<Object> iproxyRef=null;
   private XRpcEndpointImpl iendpoint=null;
   private DataBridge dataBridge=XRpcFactoryInternal.getDataBridge();
   
   private static Method HASHCODE=null;
   private static Method EQUALS=null;
   private static Method TOSTRING=null;
   private static Exception initEx=null;
   private static Field causeField=null;

   
   static {
      try {
         HASHCODE=Object.class.getMethod("hashCode");
         EQUALS=Object.class.getMethod("equals", Object.class);
         TOSTRING=Object.class.getMethod("toString");
         causeField=Throwable.class.getDeclaredField("cause");
         causeField.setAccessible(true);
      } catch (Exception ex) {
         HASHCODE=null;
         EQUALS=null;
         TOSTRING=null;
         causeField=null;
         initEx=ex;
      }
   }
   
   //Verranno implementate solo le interfacce conosciute
   public RemoteObjectWrapper(XRpcEndpointImpl endpoint, MsgObjectDescription objDesc) {
      if (initEx!=null) throw new IllegalStateException("Basic method references not initialized", initEx);
      if (objDesc.flavor!=MsgObjectDescription.Flavor.SOURCE_REF) throw new IllegalArgumentException("Not a remote object description");
      iendpoint=endpoint;
      remObjectId=objDesc.objid;
      String[] interfaceNames=objDesc.interfaces;
      
      ArrayList<Class<?>> availClasses=new ArrayList<Class<?>>();
      for(int i=0;i<interfaceNames.length;i++) {
         try {availClasses.add(Tools.getTypeForName(interfaceNames[i]));}
         catch (ClassNotFoundException ignored) {/*le interfacce sconosciute vanno ignorate*/}
      }
      
      if (availClasses.size()==0) throw new XRpcException("No known interfaces found on remote instance " + remObjectId.toString());
      
      ifaces=availClasses.toArray(new Class<?>[interfaceNames.length]);
   }
   
   public void rewire(XRpcEndpointImpl newendpoint, Identifier newid) {
      iendpoint=newendpoint;
      remObjectId=newid;
   }
   
   public synchronized Object getJavaProxy() {
      Object result=null;
      
      if (iproxyRef==null || iproxyRef.get()==null) {
         result=Proxy.newProxyInstance(this.getClass().getClassLoader(), ifaces, this);
         iproxyRef=iendpoint.getInternalFactory().enqueReference(result, this);
      } else {
         result=iproxyRef.get();         
      }
      
      return result;
   }
   
   public Identifier getObjectId() {return remObjectId;}
   
   public XRpcEndpointImpl getEndpoint() {return iendpoint;}

   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      //Non è necessario passare "proxy", perchè già contenuto in iproxyRef
      return invoke(method, args);
   }
   
   public Object invoke(Method method, Object[] args) throws Throwable {
      Object result=null;
      try {
         result=invokeProtected(method, args);
      } catch (Throwable tr) {
         if (tr instanceof XRpcException) {
            XRpcException lrex=(XRpcException)tr;
            lrex.objid=remObjectId;
            lrex.methodName=method.getDeclaringClass().getName() + ":" + method.getName();
         }
         throw tr;
      }
      return result;
   }
      
   public Object invokeProtected(Method method, Object[] args) throws Throwable {
      Object tproxy=iproxyRef.get();
      XmpConnection iconn=iendpoint.getXmpConnection();
      if (tproxy==null) throw new XRpcNullPointerException("Object finalized");
      //Se la connessione è assente e il metodo è hashCode, equals, toString:
      //   hashCode restituisce this.hashCode
      //   equals effettua un confronto di identità (==) tra 'iproxy.get' e args[0]
      //   toString restituisce this.toString
      //     -> questo per consentire almeno di rimuovere un riferimento remoto disconnesso da
      //        una collection senza provocare eccezioni
      //Se la connessione è assente e il metodo non è hashCode/equals/toString: lancia XRpcNullPointerException
      if ((iconn==null) || (!iconn.isConnected())) {
         if (method.equals(HASHCODE)) return this.hashCode();
         else if (method.equals(EQUALS)) return (args[0]==tproxy);
         else if (method.equals(TOSTRING)) return this.toString();
         else throw new XRpcNullPointerException("Not connected");
      }

      //Caso particolare: anche se connesso, se il metodo è equals e l'argomento
      //è esattamente il proxy che contiene questo handler (quindi equals restituirebbe true)
      //evita la chiamata remota (e evita loop)
      if ((method.equals(EQUALS)) && (args[0]==tproxy)) return true;
      
      String targetClassName=null;
      String targetMethodName=null;
      
      //prepara il nome della classe target
      targetClassName=method.getDeclaringClass().getName();
      //prepara il nome del metodo
      targetMethodName=method.getName();
      
      //Prepariamo le risorse per la signature e il marshalling
      Class<?>[] signature=method.getParameterTypes();
      String[] signatureClasses=new String[signature.length];
      for(int i=0;i<signature.length;i++) signatureClasses[i]=signature[i].getName();
      Annotation[][] paramsAnnot=method.getParameterAnnotations();
      MsgObjectDescription[] xmpargs=new MsgObjectDescription[(args==null)?0:args.length];

      for(int iarg=0;iarg<xmpargs.length;iarg++) {
         //Per ogni argomento passato in args
         //estrai dalla firma del metodo la classe attesa dell'argomento e l'annotazione (solo se @BySer o @ByRef)
         boolean isByRef=false;
         boolean isBySer=false;
         Annotation[] paramAnnot=paramsAnnot[iarg];
         for(Annotation iann:paramAnnot) {
            if (iann.annotationType().equals(ByRef.class)) isByRef=true;
            if (iann.annotationType().equals(BySer.class)) isBySer=true;;
         }
         //crea un XmpObjectDescription tramite processArgument
         //caso particolare: se il metodo è "equals", un'eventuale IllegalArgumentException
         //deve risultare nella restituzione di "false" al chiamante: equals non deve mai fallire,
         //pena il malfunzionamento dei container (List, Map, Set, ecc...)
         if (method.equals(EQUALS)) {
            try {
               xmpargs[iarg]=iendpoint.marshalObject(args[iarg], signature[iarg], isBySer, isByRef);
            } catch (IllegalArgumentException ex) {
               return false;
            }
         } else {
            xmpargs[iarg]=iendpoint.marshalObject(args[iarg], signature[iarg], isBySer, isByRef);
         }
         
      }
                  
      //Controlla annotazione su metodo:
      //  Se annotato con @BRpcImmediate, invia l'invocazione tramite sendEvent e ritorna null
      //  ma soltanto se il metodo ritorna void e non rilancia eccezioni
      boolean immediate=false;
      if (method.isAnnotationPresent(XRpcImmediate.class) &&
          (method.getReturnType().equals(Void.TYPE)) &&
          (method.getExceptionTypes().length==0)) immediate=true;

      return sendInvocation(targetClassName, targetMethodName, signatureClasses, xmpargs, immediate);
   }
   
   public Object sendInvocation(String targetClassName, String targetMethodName, String[] signatureClasses, MsgObjectDescription[] xmpargs, boolean immediate) throws Throwable {
      XmpConnection iconn=iendpoint.getXmpConnection();
      if ((iconn==null) || (!iconn.isConnected())) throw new XRpcNullPointerException("Not connected");
      
      //Assembla richiesta
      MsgInvocation request=new MsgInvocation();
      request.targetClass=targetClassName;
      request.targetObject=remObjectId;
      request.methodName=targetMethodName;
      request.signature=signatureClasses;
      request.arguments=xmpargs;
      
      if (immediate) {iconn.sendEvent(dataBridge.marshal(request)); return null;}
      MsgInvocationAnswer answer=dataBridge.unmarshal(iconn.sendRequest(dataBridge.marshal(request)), MsgInvocationAnswer.class);      
      if (answer==null) throw new XRpcNullPointerException("Lost connection to " + remObjectId.toString());
      
      //Elaborare XmpInvocationAnswer.getAnswerType:
      switch (answer.answtype) {
         case FAILURE:
         //  Se =FAILURE -> lancia XRpcException
            Exception originalFailure=Exception.class.cast(Bitwise.deserializeObject(answer.content.sercontents));
            throw new XRpcException("Invocation failed", originalFailure);
         case NOCLASS:
         //  Se =NOCLASS -> lancia XRpcClassNotFoundException
            throw new XRpcClassNotFoundException(request.targetClass);
         case NOMETHOD:
         //  Se =NOMETHOD -> lancia XRpcNoSuchMethodException
            throw new XRpcNoSuchMethodException(request.methodName);
         case NOOBJECT:
         //  Se =NOTARGET -> lancia XRpcNullPointerException
            throw new XRpcNullPointerException(remObjectId.toString());
         case EXCEPTION:
         //  Se =EXCEPTION ->
         //Estrazione di due copie di tutto il pacchetto (mi aspetto che sia una LRpcRemoteException)
            XRpcRemoteException remoteException=XRpcRemoteException.class.cast(Bitwise.deserializeObject(answer.content.sercontents));
            XRpcRemoteException copyException=XRpcRemoteException.class.cast(Bitwise.deserializeObject(answer.content.sercontents));
         //Estrazione dell'eccezione originale dalla copia (per rilanciarla in locale)
            Throwable originalException=copyException.getCause();
         //Inserire la remoteException come causa dell'eccezione originale
         //Forziamo la mano: un'eccezione che possiede già una causa normalmente
         //non può ricevere una causa diversa. Ma qui siamo fuori da ogni "normalità"
            
            causeField.set(originalException, remoteException);
            
         //  esempio: se su remoto avviene una NullPointerException, in locale
         //           verrà resa come
         //           NullPointerException
         //              caused by XRpcRemoteException
         //                 caused by NullPointerException [stack trace originale]
         //  rilancia l'eccezione verso il chiamante
            throw originalException;
         case SUCCESS:
         //  Se =SUCCESS: Estrai l'oggetto dalla descrizione
            return iendpoint.unmarshalObject(answer.content);
      }
      
      return null;
   }
}
