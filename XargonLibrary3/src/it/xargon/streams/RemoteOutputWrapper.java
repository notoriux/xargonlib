package it.xargon.streams;

import it.xargon.util.Holder;
import it.xargon.util.Identifier;

import java.io.*;
import java.util.Arrays;

public class RemoteOutputWrapper extends OutputStream {
   private Object lock=null;
   private IRemoteOutputStream iros=null;
   
   public RemoteOutputWrapper(IRemoteOutputStream ros) {
      if (!ros.isOpen()) throw new IllegalArgumentException("Remote output stream already closed");
      lock=new Object();
      iros=ros;
      
      final Holder<Identifier> regId=new Holder<Identifier>();
      
      regId.set(iros.onEvent(IRemoteInputStream.STREAMCLOSED, () -> {
         synchronized (lock) {
            iros.unregister(regId.get());
            iros=null;
         }
      }));
   }
   
   private void checkStream() throws IOException {
      if (iros==null) throw new IOException("Stream closed");      
   }
   
   public void write(int b) throws IOException {
      synchronized (lock) {
         checkStream();
         iros.write(b);
      }
   }
   
   public void write(byte[] b) throws IOException {
      synchronized (lock) {
         checkStream();
         iros.write(b);
      }      
   }

   public void write(byte[] b, int off, int len) throws IOException {
      byte[] buffer=Arrays.copyOfRange(b, off, off+len);
      synchronized (lock) {
         checkStream();
         iros.write(buffer);
      }            
   }
   
   public void flush() throws IOException {
      synchronized (lock) {
         checkStream();
         iros.flush();
      }                  
   }

   public void close() throws IOException {
      synchronized (lock) {
         checkStream();
         iros.close();
      }                  
   }
}
