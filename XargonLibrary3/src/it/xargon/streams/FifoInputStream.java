package it.xargon.streams;

import java.io.*;

public class FifoInputStream extends InputStream {
   private StreamFifo f=null;
   
   public FifoInputStream(StreamFifo fifo) {f=fifo;}

   public int read() throws IOException {
      try {
         if (!f.isReadable()) return -1;
      } catch (IllegalStateException ex) {
         throw new IOException(ex);
      }

      try {
         return f.read();
      } catch (InterruptedException ex) {
         throw new IOException(ex);
      } catch (IllegalStateException ex) {
         return -1;
      }
   }
   
   public int read(byte[] b, int off, int len) throws IOException {
      try {
         if (!f.isReadable()) return -1;
      } catch (IllegalStateException ex) {
         throw new IOException(ex);
      }

      try {
         int wasblocked=0;
         int total=0;
         if (f.getReadableData()==0) {wasblocked=1;total=f.read(b, off, 1);}
         total+=f.drain(b, off + wasblocked, len - wasblocked);
         return total;
      } catch (IllegalStateException ex) {
         return -1;
      } catch (InterruptedException ex) {
         throw new IOException(ex);
      }
   }
   
   public long skip(long n) throws IOException {
      try {
         if (!f.isReadable()) return -1;
      } catch (IllegalStateException ex) {
         throw new IOException(ex);
      }

      try {
         return f.skip((int)n);
      } catch (InterruptedException ex) {
         throw new IOException(ex);
      } catch (IllegalStateException ex) {
         return -1;
      }
   }
   
   public void close() throws IOException {
      f.close();
   }
   
   public int available() throws IOException {
      return f.getReadableData();
   }
}
