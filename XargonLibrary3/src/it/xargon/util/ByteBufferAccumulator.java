package it.xargon.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ByteBufferAccumulator {
   public interface Allocator {
      public ByteBuffer alloc(int size);
   }
   
   private ArrayList<ByteBuffer> accumulator=null;
   private Allocator allocator=null;
   
   public ByteBufferAccumulator(Allocator allocator) {
      this.allocator=allocator;
      accumulator=new ArrayList<>();
   }

   public ByteBufferAccumulator add(ByteBuffer src) {return add(src,false);}
   
   public ByteBufferAccumulator add(ByteBuffer src, boolean flip) {
      ByteBuffer isrc=src.duplicate();
      if (flip) isrc.flip();
      accumulator.add(isrc);
      return this;
   }
   
   public ByteBufferAccumulator addWithSize(ByteBuffer src) {return addWithSize(src, false);}

   public ByteBufferAccumulator addWithSize(ByteBuffer src, boolean flip) {
      ByteBuffer isrc=src.duplicate();
      if (flip) isrc.flip();
      add(isrc.remaining());
      accumulator.add(isrc);
      return this;
   }
   
   public ByteBufferAccumulator add(Integer intValue) {
      ByteBuffer cntBuf=allocator.alloc(Integer.BYTES);
      cntBuf.putInt(intValue).flip();
      accumulator.add(cntBuf);
      return this;
   }
   
   public ByteBufferAccumulator add(byte[] src) {
      ByteBuffer buf=allocator.alloc(src.length);
      buf.put(src).flip();
      accumulator.add(buf);
      return this;
   }
   
   public ByteBufferAccumulator addWithSize(byte[] src) {
      add(src.length);
      ByteBuffer buf=allocator.alloc(src.length);
      buf.put(src).flip();
      accumulator.add(buf);
      return this;
   }
   
   public ByteBufferAccumulator addWithByteSize(byte[] src) {
      if (src.length>255) throw new IllegalArgumentException("Source buffer has more than 255 bytes");
      add(Bitwise.asByte(src.length));
      ByteBuffer buf=allocator.alloc(src.length);
      buf.put(src).flip();
      accumulator.add(buf);
      return this;
   }
   
   public ByteBufferAccumulator add(byte src) {
      ByteBuffer buf=allocator.alloc(1);
      buf.put(src).flip();
      accumulator.add(buf);
      return this;
   }
   
   public ByteBuffer gather() {
      int totalSize=accumulator.stream().collect(Collectors.summingInt(ByteBuffer::remaining));
      ByteBuffer result=allocator.alloc(totalSize);
      accumulator.forEach(result::put);
      result.flip();
      return result;
   }
}
