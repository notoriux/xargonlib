package it.xargon.xrpc.actions;

import it.xargon.xrpc.XRpcEndpointImpl;
import it.xargon.xrpc.messages.MsgRemoteProxyFinalized;

public class XRpcRemoteProxyFinalized extends XRpcAction<MsgRemoteProxyFinalized, Void> {
   public Void process(MsgRemoteProxyFinalized message, XRpcEndpointImpl endpoint) {
      //Un proxy remoto (RemoteObjectWrapper) è stato finalizzato,
      //inutile mantenere riferimenti in locale (tranne se l'oggetto
      //è legato ad un nome pubblico)
      //Nel contempo evitiamo di rinotificare all'indietro (tramite emissione
      //di evento) la rimozione dalla cache locale
      endpoint.getLocalCache().uncacheObject(message.target, false);
      return null;
   }
}
