package it.xargon.xrpc;

import it.xargon.util.Identifier;

public class XRpcException extends RuntimeException {
   public Identifier objid=null;
   public String methodName=null;

   public XRpcException() {super();}
   public XRpcException(String message) {super(message);}
   public XRpcException(Throwable cause) {super(cause);}
   public XRpcException(String message, Throwable cause) {super(message, cause);}
   
   public String getMessage() {
      String result=super.getMessage();
      if ((objid!=null) && (methodName!=null)) {
         result+=" (caught while invoking " + methodName + " on " + objid.toString() + ")";
      }
      return result;
   }
}
