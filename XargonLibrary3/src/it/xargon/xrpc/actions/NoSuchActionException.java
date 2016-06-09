package it.xargon.xrpc.actions;

public class NoSuchActionException extends Exception {
   public NoSuchActionException() {super();}
   public NoSuchActionException(String message, Throwable cause) {super(message, cause);}
   public NoSuchActionException(String message) {super(message);}
   public NoSuchActionException(Throwable cause) {super(cause);}
}
