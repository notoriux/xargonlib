package it.xargon.smartnet;

public class SmartnetException extends RuntimeException {
   private boolean mayStop=true;
   
   public SmartnetException() {super();}
   public SmartnetException(String message) {super(message);}
   public SmartnetException(String message, Throwable cause) {super(message,cause);}
   public SmartnetException(Throwable cause) {super(cause);}
   
   public void dontStop() {mayStop=false;}
   public boolean stopRequested() {return mayStop;}
}