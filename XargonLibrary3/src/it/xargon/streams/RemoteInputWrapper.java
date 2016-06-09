package it.xargon.streams;

import it.xargon.util.*;

import java.io.IOException;
import java.io.InputStream;

public class RemoteInputWrapper extends InputStream {
   private Object lock=null;
   private IRemoteInputStream iris=null;
   
   public RemoteInputWrapper(IRemoteInputStream ris) {
      if (ris==null) throw new NullPointerException();
      if (!ris.isOpen()) throw new IllegalArgumentException("Remote input stream already closed");
      lock=new Object();
      iris=ris;
      
      final Holder<Identifier> regId=new Holder<Identifier>();
      
      regId.set(iris.onEvent(IRemoteInputStream.STREAMCLOSED, () -> {
         synchronized (lock) {
            iris.unregister(regId.get());
            iris=null;
         }
      }));
   }
   
   private void checkStream() throws IOException {
      if (iris==null) throw new IOException("Stream closed");      
   }

   public int available() throws IOException {
      synchronized (lock) {
         checkStream();
         return iris.available();
      }
   }
   
   public void close() throws IOException {
      synchronized (lock) {
         checkStream();
         iris.close();
      }
   }
   
   public int read() throws IOException {
      synchronized (lock) {
         checkStream();
         return iris.read();
      }
   }
   
   public int read(byte[] b, int off, int len) throws IOException {
      synchronized (lock) {
         checkStream();
         byte[] r=iris.read(len);
         if (r==null || r.length==0) return -1;
         System.arraycopy(r, 0, b, off, r.length);
         return r.length;
      }
   }
   
   public long skip(long n) throws IOException {
      synchronized (lock) {
         checkStream();
         return iris.skip(n);
      }
   }
}
