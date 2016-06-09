package it.xargon.xrpc;

public class XRpcNoSuchMethodException extends XRpcException {
   public XRpcNoSuchMethodException() {super();}
   public XRpcNoSuchMethodException(String message) {super(message);}
   public XRpcNoSuchMethodException(Throwable cause) {super(cause);}
   public XRpcNoSuchMethodException(String message, Throwable cause) {super(message, cause);}
}
