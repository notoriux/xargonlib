package it.xargon.nioxmp;

public class XmpException extends RuntimeException {
   private Object attachedInfo=null;
   
   public XmpException() {
      super();
   }

   public XmpException(String message) {
      super(message);
   }

   public XmpException(Throwable cause) {
      super(cause);
   }

   public XmpException(String message, Throwable cause) {
      super(message, cause);
   }

   public XmpException(String message, Throwable cause,
         boolean enableSuppression, boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
   }
   
   public XmpException setAttachedInfo(Object attachedInfo) {
      this.attachedInfo=attachedInfo;
      return this;
   }

   public Object getAttachedInfo() {return attachedInfo;}
}
