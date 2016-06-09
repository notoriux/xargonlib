package it.xargon.xrpc.actions;

import it.xargon.xrpc.*;

public abstract class XRpcAction<T,K> {
   public abstract K process(T message, XRpcEndpointImpl endpoint);
   
   @SuppressWarnings("unchecked")
   public final Object _process(Object message, XRpcEndpointImpl endpoint) {
      return process((T)message, endpoint);
   }
}
