package it.xargon.xrpc.actions;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import it.xargon.util.Bitwise;
import it.xargon.util.Identifier;
import it.xargon.util.Tools;
import it.xargon.xrpc.ByRef;
import it.xargon.xrpc.BySer;
import it.xargon.xrpc.LocalObjectWrapper;
import it.xargon.xrpc.XRpcEndpointImpl;
import it.xargon.xrpc.XRpcException;
import it.xargon.xrpc.XRpcImmediate;
import it.xargon.xrpc.XRpcRemoteException;
import it.xargon.xrpc.messages.MsgInvocation;
import it.xargon.xrpc.messages.MsgInvocationAnswer;
import it.xargon.xrpc.messages.MsgObjectDescription;
import it.xargon.xrpc.messages.MsgInvocationAnswer.AnswerType;
import it.xargon.xrpc.messages.MsgObjectDescription.Flavor;

public class XRpcInvocation extends XRpcAction<MsgInvocation, MsgInvocationAnswer> {   
   public MsgInvocationAnswer process(MsgInvocation invocation, XRpcEndpointImpl endpoint) {
      //Ricerca dell'oggetto richiesto
      Identifier target=invocation.targetObject;
      if (!endpoint.getLocalCache().isObjectCached(target)) {
         MsgInvocationAnswer answer=new MsgInvocationAnswer();
         answer.answtype=MsgInvocationAnswer.AnswerType.NOOBJECT;
         answer.content=new MsgObjectDescription();
         answer.content.flavor=Flavor.DEST_REF;
         answer.content.objid=target;
         return answer;
      }
      
      LocalObjectWrapper wrappedObj=endpoint.getLocalCache().getObjectWrapper(target);      
      
      //estrai classe target (che contiene il metodo)
      String targetClassName=invocation.targetClass;
      //estrai id metodo target
      String targetMethodName=invocation.methodName;
      //estrai signature metodo
      String[] targetMethodSignature=invocation.signature;
      
      //Prepara risposta
      MsgInvocationAnswer answer=new MsgInvocationAnswer();
      MsgObjectDescription desc=new MsgObjectDescription();
      
      //--- Ottieni Method da invocare ---
      //Classe di appartenenza;
      Class<?> targetClass=null;
      try {targetClass=Tools.getTypeForName(targetClassName);}
      catch (ClassNotFoundException ex) {
         answer.answtype=AnswerType.NOCLASS;
         answer.content=new MsgObjectDescription();
         answer.content.flavor=Flavor.SERIALIZED;
         answer.content.sercontents=Bitwise.serializeObject(targetClassName);
         return answer;
      }

      //Firma del metodo
      Class<?>[] signature=new Class<?>[targetMethodSignature.length];
      for(int i=0;i<signature.length;i++) {
         try {
            signature[i]=Tools.getTypeForName(targetMethodSignature[i]);
         } catch (ClassNotFoundException ex) {
            answer.answtype=AnswerType.NOCLASS;
            answer.content=new MsgObjectDescription();
            answer.content.flavor=Flavor.SERIALIZED;
            answer.content.sercontents=Bitwise.serializeObject(targetMethodSignature[i]);
            return answer;
         }
      }
      
      //Ed ecco il metodo stesso
      Method method=null;
      try {method=targetClass.getMethod(targetMethodName, signature);}
      catch (NoSuchMethodException ex) {
         answer.answtype=AnswerType.NOMETHOD;
         answer.content=new MsgObjectDescription();
         answer.content.flavor=Flavor.SERIALIZED;
         answer.content.sercontents=Bitwise.serializeObject(targetMethodName);
         return answer;
      }
      
      //Otteniamo la classe del ritorno del metodo, per il marshaling del risultato
      Class<?> returnType=method.getReturnType();
      
      //Otteniamo eventuali annotazioni sul metodo (@BySer o @ByRef) per aiutare il marshaling
      Annotation[] methodAnnots=method.getAnnotations();
      boolean isByRef=false;
      boolean isBySer=false;
      for(Annotation iann:methodAnnots) {
         if (iann.annotationType().equals(ByRef.class)) isByRef=true;
         if (iann.annotationType().equals(BySer.class)) isBySer=true;;
      }

      //Il riferimento all'oggetto su cui invocare il metodo è già contenuto in wrappedobject
      
      //Traduci ogni descrizione in oggetto effettivo tramite unmarshaling
      MsgObjectDescription[] xmpargs=invocation.arguments;
      Object[] args=new Object[xmpargs.length];
      try {
         for(int iarg=0;iarg<args.length;iarg++) {
            args[iarg]=endpoint.unmarshalObject(xmpargs[iarg]);
         }
      } catch (ClassNotFoundException ex) {
         answer.answtype=AnswerType.FAILURE;
         desc.flavor=Flavor.SERIALIZED;
         desc.sercontents=Bitwise.serializeObject(ex);
         answer.content=desc;
      }
      
      //EFFETTUA INVOCAZIONE
      Object result=null;
      Throwable externalException=null;
      Throwable internalException=null;
      //XRpcFactoryInternal.enterLocalInvocation(iendpoint);
      try {result=method.invoke(wrappedObj, args);}
      catch (IllegalArgumentException e) {externalException=e;}
      catch (IllegalAccessException e) {externalException=e;}
      //Le eccezioni generate dall'interno del metodo vengono catturate e gestite
      //ritrasmettendole al richiedente sotto forma di RemoteException
      catch (InvocationTargetException e) {internalException=e.getCause();}
      //Qualsiasi altra eccezione non prevista, failure
      catch (Throwable t) {externalException=t;}
      //XRpcFactoryInternal.exitLocalInvocation();
      
      //Controllare annotazione su metodo: se annotato con @XRpcImmediate, ritorna null immediatamente
      if (method.isAnnotationPresent(XRpcImmediate.class)) return null;
      
      //Esamina eccezioni
      if (externalException!=null) {
      //  Se ECCEZIONE ESTERNA prima di entrare nel metodo: failure!
         answer.answtype=AnswerType.FAILURE;
         desc.flavor=Flavor.SERIALIZED;
         desc.sercontents=Bitwise.serializeObject(externalException);
      } else if (internalException!=null) {
         //se ECCEZIONE DURANTE CHIAMATA serializzare eccezione verso il chiamante         
         answer.answtype=AnswerType.EXCEPTION;
         desc.flavor=Flavor.SERIALIZED;
         XRpcRemoteException wrapperException=new XRpcRemoteException("Exception happened on remote side of " + endpoint.getXmpConnection().toString(), internalException);
         desc.sercontents=Bitwise.serializeObject(wrapperException);
      } else {
         //Trasmetti il risultato al richiedente tramite nuovo marshaling
         try {
            desc=endpoint.marshalObject(result, returnType, isBySer, isByRef);
            answer.answtype=AnswerType.SUCCESS;
         } catch (IllegalArgumentException ex) {
            answer.answtype=AnswerType.FAILURE;
            desc.flavor=Flavor.SERIALIZED;
            desc.sercontents=Bitwise.serializeObject(new XRpcException("Return value not marshallable", ex));
         }         
      }
      
      answer.content=desc;

      return answer;
   }
}
