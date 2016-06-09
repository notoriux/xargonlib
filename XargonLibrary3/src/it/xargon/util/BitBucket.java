package it.xargon.util;

import java.util.*;

/**
 * <P>BitBucket implements a bit container similar to java.util.BitSet. There have been
 * implemented some methods to save, retrieve and iterate through the contents.</P>
 * <P>BitBucket is completely interoperable with BitSet.</P>
 * @author Francesco Muccilli
 *
 */

public class BitBucket implements Iterable<Boolean> {   
   private class IteratorProxy {
      private BitBucket target=null;
      
      public IteratorProxy(BitBucket itarget) {target=itarget;}
      
      public int getSize() {
         if (target==null) throw new ConcurrentModificationException();
         return target.getSize();
      }

      public boolean getBit(int loc) {
         if (target==null) throw new ConcurrentModificationException();
         return target.getBit(loc);
      }
      
      public void disconnect() {target=null;}
   }
   
   private class BitBucketIterator implements Iterator<Boolean> {
      private int cnt=0;
      private IteratorProxy prx=null;
      
      public BitBucketIterator(IteratorProxy iproxy) {prx=iproxy;}
      public boolean hasNext() {return (cnt<prx.getSize());}
      public Boolean next() {cnt++; return prx.getBit(cnt-1);}
      public void remove() {throw new UnsupportedOperationException();}
      
   }
   
   private final static byte[] BIT={(byte)1,(byte)2,(byte)4,(byte)8,(byte)16,(byte)32,(byte)64,(byte)128};
   private int size=0;
   private byte[] contents=null;
   private IteratorProxy currentIProxy=null;

   /**
    * Builds a BitBucket containing 'isize' bits. Once built, size can be changed via setSize() method
    * @param isize Bucket size in bits
    */
   public BitBucket(int isize) {this(isize,null);}
   
   /**
    * <P>Builds a BitBucket containing 'isize' bits, initializing the contens with 'icontents'.</P>
    * <P>This constructor is designed to initialize a BitBucket with a raw block loaded from a binary file.
    * Specifying 'isize' is necessary because 'icontents' size, measured in bits, is always a multiple of 8.</P>
    * @param isize Expected bits count
    * @param icontents Initial contents
    */
   public BitBucket(int isize, byte[] icontents) {
      setSize(isize);
      if (icontents!=null) setContents(icontents);
   }
   
   /**
    * Builds a BitBucket from a BitSet
    */
   public BitBucket(BitSet original) {
      setSize(original.size());
      for(int cnt=0;cnt<original.size();cnt++) setBit(cnt,original.get(cnt));
   }
   
   /**
    * Returns an iterator over bucket contents. The iterator stays valid as long the bucket contents
    * or size aren't modified. Whenever this happens, every access to previously built iterator throws
    * a ConcurrentModificationException.
    */
   public Iterator<Boolean> iterator() {
      if (currentIProxy==null) currentIProxy=new IteratorProxy(this);
      return new BitBucketIterator(currentIProxy);
   }

   /**
    * @return The size of the contents, in bits.
    */
   public int getSize() {return size;}
   
   /**
    * <P>Modifies current contents size (in bits). If the new size is greater than current size, the
    * content will remain untouched (adding '0's at LSB end), otherwise excessive contents will be
    * lost.</P>
    * <P>Every active iterator will be invalidated</P>
    * @param isize the new size in bits
    */
   public void setSize(int isize) {
      if (currentIProxy!=null) {currentIProxy.disconnect();currentIProxy=null;}
      int bsize=(isize/8) + ((isize%8)>0?1:0);
      byte[] newcont=new byte[bsize];
      if ((contents!=null) & (bsize!=0)) System.arraycopy(contents,0,newcont,0,((contents.length<=newcont.length)?contents.length:newcont.length));
      contents=newcont;
      size=isize;
   }

   /**
    * Returns the bit value at index 'loc'.
    */
   public boolean getBit(int loc) {
      if ((loc<0) || (loc>=size)) throw new IllegalArgumentException("Location out of bounds ("+loc+")");

      int obit=loc%8;
      int obyte=(loc-obit)/8;
      return (contents[obyte] & BIT[obit]) == BIT[obit];
   }

   /**
    * Set the bit value at index 'loc'. Every active iterator will be invalidated.
    * @return the old value;
    */
   public boolean setBit(int loc, boolean val) {
      if ((loc<0) || (loc>=size)) throw new IllegalArgumentException("Location out of bounds ("+loc+")");
      boolean result=getBit(loc);
      if (currentIProxy!=null) {currentIProxy.disconnect();currentIProxy=null;}
      int obit=loc%8;
      int obyte=(loc-obit)/8;
      if (val) contents[obyte]=(byte)((contents[obyte] | BIT[obit]) & 0xFF);
      else contents[obyte]=(byte)((contents[obyte] & ~BIT[obit]) & 0xFF);
      return result;
   }

   /**
    * Reverses the bit value at index 'loc' (true->false, false->true). Every active iterator will be invalidated.
    * @return the old value
    */
   public boolean flipBit(int loc) {
      return setBit(loc, !getBit(loc));
   }

   /**
    * Zeroes the whole contents. Every active iterator will be invalidated.
    */
   public void clean() {
      if (currentIProxy!=null) {currentIProxy.disconnect();currentIProxy=null;}
      java.util.Arrays.fill(contents,(byte)0);
   }

   /**
    * Same as getContents(null)
    * @return Bucket contents as byte array
    */
   public byte[] getContents() {return getContents(null);}

   /**
    * <P>Returns the bucket contents as a byte array that can be written on an OutputStream</P>
    * <P><STRONG>WARNING!</STRONG> The effective bucket size (in bits) isn't stored in this
    * array. It must be managed separately, via getSize/setSize</P>
    * @param buffer A preallocated buffer. If large enough, will be used to store a copy of the
    * contents and returned from this function, otherwise a new one will be allocated and returned.
    */
   public byte[] getContents(byte[] buffer) {
      byte[] res=((buffer==null)?new byte[contents.length]:buffer);
      if (res.length<contents.length) res=new byte[contents.length];
      System.arraycopy(contents,0,res,0,contents.length);
      return res;
   }
   
   /**
    * Same as getBitSet(null) .
    * @return A java.util.BitSet inizialized with bucket contents
    */
   public BitSet getBitSet() {return getBitSet(null);}
   
   /**
    * <P>Restituisce un BitSet inizializzato tramite il contenuto e la dimensione in bit del bucket</P>
    * @param buffer Un eventuale BitSet preallocato. Se di dimensione sufficiente, verrà utilizzato
    * come destinazione del contenuto (e restituito), altrimenti ne verrà allocato uno nuovo.
    * @return Un BitSet inizializzato tramite il contenuto del bucket
    */
   public BitSet getBitSet(BitSet buffer) {
      BitSet result=null;
      if ((buffer==null) || (buffer.size()<size)) result=new BitSet(size); else result=buffer;
      for(int cnt=0;cnt<result.size();cnt++) result.set(cnt, getBit(cnt));     
      return result;
   }

   /**
    * Salva i contenuti passati per argomento nel bucket. La dimensione in bit rimane invariata. Se la
    * quantità di contenuti specificati è superiore alla dimensione del bucket, i contenuti in eccedenza
    * verranno tagliati. Se è in corso un'iterazione, tutti gli iteratori attivi sono invalidati.
    * @param icontents Nuovi contenuti in forma di array di byte
    */
   public void setContents(byte[] icontents) {
      if (icontents==null) throw new NullPointerException();
      if (currentIProxy!=null) {currentIProxy.disconnect();currentIProxy=null;}
      System.arraycopy(icontents,0,contents,0,Math.min(icontents.length, contents.length));
   }
   
   /**
    * Salva i contenuti passati per argomento nel bucket. La dimensione in bit viene adeguata alla
    * dimensione del BitSet. Se è in corso un'iterazione, tutti gli iteratori attivi sono invalidati.
    * @param original BitSet originale che verrà replicato nel bucket.
    */
   public void setContents(BitSet original) {
      if (original==null) throw new NullPointerException();
      if (currentIProxy!=null) {currentIProxy.disconnect();currentIProxy=null;}
      setSize(original.size());
      for(int cnt=0;cnt<original.size();cnt++) setBit(cnt,original.get(cnt));
   }
}
