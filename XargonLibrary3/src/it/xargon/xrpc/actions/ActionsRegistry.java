package it.xargon.xrpc.actions;

import it.xargon.xrpc.XRpcEndpointImpl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class ActionsRegistry {
   private LinkedHashMap<Class<?>, XRpcAction<?,?>> actions=null;
   
   public ActionsRegistry() {
      actions=new LinkedHashMap<Class<?>, XRpcAction<?,?>>();
      
      addAction(new XRpcPubObjRequest());
      addAction(new XRpcPubObjsListRequest());
      addAction(new XRpcObjectRequest());
      addAction(new XRpcInvocation());
   }
   
   public void addAction(XRpcAction<?, ?> action) {
      ParameterizedType paramType=ParameterizedType.class.cast(this.getClass().getGenericSuperclass());
      Type[] paramClass=paramType.getActualTypeArguments();
      Class<?> messageClass=Class.class.cast(paramClass[0]);
      actions.put(messageClass, action);
   }
   
   public Object process(Object message,  XRpcEndpointImpl endpoint) throws NoSuchActionException {
      XRpcAction<?,?> action=actions.get(message.getClass());
      if (action==null) throw new NoSuchActionException("No actions available for specified message: " + message.getClass().getName());
      return action._process(message, endpoint);
   }
}
