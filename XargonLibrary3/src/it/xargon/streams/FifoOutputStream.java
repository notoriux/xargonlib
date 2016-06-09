package it.xargon.streams;

import java.io.*;

public class FifoOutputStream extends OutputStream {
   private StreamFifo f=null;
   
   public FifoOutputStream(StreamFifo fifo) {f=fifo;}
   
   public void write(int b) throws IOException {
      try {
         f.write(b);
      } catch (InterruptedException ex) {
         throw new IOException(ex);
      } catch (IllegalStateException ex) {
         throw new IOException(ex);
      }
   }
   
   public void write(byte[] b, int off, int len) throws IOException {
      try {
         f.write(b, off, len);
      } catch (InterruptedException ex) {
         throw new IOException(ex);
      } catch (IllegalStateException ex) {
         throw new IOException(ex);
      }
   }
   
   public void close() throws IOException {
      f.close();
   }
}
