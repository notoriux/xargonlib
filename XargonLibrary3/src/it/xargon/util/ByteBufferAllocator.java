package it.xargon.util;

import java.nio.ByteBuffer;

public interface ByteBufferAllocator {
   public ByteBuffer alloc(int capacity);
   
   public static ByteBufferAllocator DIRECT=new ByteBufferAllocator() {
      @Override
      public ByteBuffer alloc(int capacity) {
         return ByteBuffer.allocateDirect(capacity);
      }
   };

   public static ByteBufferAllocator INDIRECT=new ByteBufferAllocator() {
      @Override
      public ByteBuffer alloc(int capacity) {
         return ByteBuffer.allocate(capacity);
      }
   };
}
