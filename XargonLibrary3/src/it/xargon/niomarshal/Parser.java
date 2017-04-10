package it.xargon.niomarshal;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import it.xargon.util.ByteBufferAccumulator;
import it.xargon.util.ByteBufferAllocator;

public class Parser {
   private enum EXPECTING {
      NAMELEN,
      NAMEBUFFER,
      CONTENTLEN,
      CONTENTBUFFER
   }
   
   private ByteBufferAllocator allocator=null;
   private EXPECTING expect=null;
   private ByteBuffer buffer=null;
   private ByteBufferAccumulator accumulator=null;
   private int len=0;
   
   public Parser(ByteBufferAllocator allocator) {
      this.allocator=allocator;
      
      accumulator=new ByteBufferAccumulator(allocator);
      reset();
   }
   
   public void reset() {
      expect=EXPECTING.NAMELEN;
      buffer=allocator.alloc(Integer.BYTES);
      accumulator.clear();
   }

   public ByteBuffer feed(byte b) {
      ByteBuffer result=null;
      
      switch (expect) {
         case NAMELEN:
            buffer.put(b);
            if (!buffer.hasRemaining()) {
               buffer.flip();
               len=buffer.getInt();
               buffer=allocator.alloc(len);
               expect=EXPECTING.NAMEBUFFER;
            }
            break;
         case NAMEBUFFER:
            buffer.put(b);
            if (!buffer.hasRemaining()) {
               accumulator.addWithSize(buffer, true);
               buffer=allocator.alloc(Integer.BYTES);
               expect=EXPECTING.CONTENTLEN;
            }
            break;
         case CONTENTLEN:
            buffer.put(b);
            if (!buffer.hasRemaining()) {
               buffer.flip();
               len=buffer.getInt();
               buffer=allocator.alloc(len);
               expect=EXPECTING.CONTENTBUFFER;
            }
            break;
         case CONTENTBUFFER:
            buffer.put(b);
            if (!buffer.hasRemaining()) {
               accumulator.addWithSize(buffer, true);
               result=accumulator.gather();
               reset();
            }
            break;
      }
      
      return result; //If and only if a whole data block is available (namelen + name + contentlen + content)
   }
   
   public ByteBuffer[] feed(byte[] src) {
      ArrayList<ByteBuffer> result=new ArrayList<>();
      
      for(byte b:src) {
         ByteBuffer partialResult=feed(b);
         if (partialResult!=null) result.add(partialResult);
      }
      
      return result.toArray(new ByteBuffer[result.size()]);
   }

   public ByteBuffer[] feed(ByteBuffer... src) {
      ArrayList<ByteBuffer> result=new ArrayList<>();
      
      for(ByteBuffer buf:src) {
         if (buf.position()>0) buf.flip();
         while (buf.hasRemaining()) {
            ByteBuffer partialResult=feed(buf.get());
            if (partialResult!=null) result.add(partialResult);
         }
      }
      
      return result.toArray(new ByteBuffer[result.size()]);
   }
}
