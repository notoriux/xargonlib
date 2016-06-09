package it.xargon.xrpc;

public class XRpcRemoteException extends XRpcException {
   public XRpcRemoteException() {super();}
   public XRpcRemoteException(String message) {super(message);}
   public XRpcRemoteException(Throwable cause) {super(cause);}
   public XRpcRemoteException(String message, Throwable cause) {super(message, cause);}
}
