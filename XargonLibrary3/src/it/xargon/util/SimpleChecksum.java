package it.xargon.util;

/**
 * This is a simple checksum tool. Every byte value passed in one of "feed" function will
 * be added (circular sum limited at 255) to the current checksum value.
 * @author Francesco Muccilli
 *
 */

public class SimpleChecksum {
   private int current=0;
   private final static int LAST=Integer.MAX_VALUE - 255;
   
   /**
    * Returns the current checksum value in the range 0..255
    */
   public int get() {return current;}

   /**
    * Sets the current checksum value to 0
    */
   public synchronized void reset() {current=0;}
      
   /**
    * Feeds a stream of bytes to the checksum
    */
   public synchronized void feed(byte[] data) {feed(data,0,data.length);}
   
   /**
    * Feeds a stream of bytes to the checksum, within the specified limits
    * @param data The array containing the bytes
    * @param off Starting point of the feeding
    * @param len Number of bytes to feed
    */
   public synchronized void feed(byte[] data, int off, int len) {
      for(int cnt=off;cnt<off+len;cnt++) _feed(Bitwise.asInt(data[cnt]));
      _norm();
   }
   
   /**
    * Feeds a single byte to the checksum
    */
   public synchronized void feed(byte data) {_feed(Bitwise.asInt(data));_norm();}
   
   /**
    * Feeds a single byte to the checksum, allowing unsigned bytes via integer
    */
   public synchronized void feed(int data) {_feed(data);_norm();}
   
   private void _feed(int data) {
      if (current > LAST) _norm();
      current+=data;
   }
   
   private void _norm() {current%=256;}
}
