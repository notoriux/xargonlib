package it.xargon.util;

public class CountedLatch {
   private Object sync=null;
   private long count=0;
   
   public CountedLatch(long initial) {
      sync=new Object();
      count=initial;
   }
   
   public CountedLatch() {this(0);}
   
   public void increment() {synchronized (sync) {count++;}}
   
   public void decrement() {synchronized (sync) {
      if (count==0) throw new IllegalStateException("Count is zero");
      count--;
      if (count==0) sync.notifyAll();
   }}
   
   public long getCount() {synchronized (sync) {return count;}}
   
   public void await() throws InterruptedException {await(0);}
   public boolean await(long timeout) throws InterruptedException {
      synchronized (sync) {
         if (count==0) return true;
         sync.wait(timeout);
         return (count==0);
      }
   }
}
