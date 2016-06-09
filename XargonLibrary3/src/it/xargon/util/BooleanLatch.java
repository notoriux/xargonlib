package it.xargon.util;

public class BooleanLatch {
   private boolean iopen=false;
   private Object sync=null;
   
   public BooleanLatch() {this(false);}
   public BooleanLatch(boolean open) {
      iopen=open;
      sync=new Object();
   }
   
   public void setOpen(boolean open) {
      synchronized (sync) {
         if (open==iopen) return;
         
         if (iopen && !open) {
            iopen=false;
         } else {
            iopen=true;
            sync.notifyAll();
         }
      }
   }
   public void open() {setOpen(true);}
   public void close() {setOpen(false);}
   public boolean isOpen() {return iopen;}
   
   public void await() throws InterruptedException {await(0);}
   public boolean await(long timeout) throws InterruptedException {
      synchronized (sync) {
         if (iopen) return true;
         sync.wait(timeout);
         return iopen;
      }
   }
}
