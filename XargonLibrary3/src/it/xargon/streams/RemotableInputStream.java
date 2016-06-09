package it.xargon.streams;

import java.io.*;

import it.xargon.events.*;

public class RemotableInputStream extends EventsSourceImpl implements IRemoteInputStream {
   private Object lock=null;
   private InputStream _is=null;
   
   public RemotableInputStream(InputStream is) {
      if (is==null) throw new NullPointerException();
      lock=new Object();
      _is=is;
   }
   
   public boolean isOpen() {
      synchronized (lock) {
         return _is!=null;         
      }
   }
   
   private void checkStream() throws IOException {
      if (!isOpen()) throw new IOException("Stream closed");      
   }

   public int available() throws IOException {
      synchronized (lock) {
         checkStream();
         try {
            return _is.available();
         } catch (IOException ex) {
            _is=null;
            raise(STREAMCLOSED).run();
            throw ex;
         }
      }
   }

   public void close() throws IOException {
      synchronized (lock) {
         checkStream();
         try {
            _is.close();
         } catch (IOException ex) {
            throw ex;
         } finally {
            _is=null;
            raise(STREAMCLOSED).run();
         }
      }
   }

   public int read() throws IOException {
      synchronized (lock) {
         checkStream();
         try {
            return _is.read();
         } catch (IOException ex) {
            _is=null;
            raise(STREAMCLOSED).run();
            throw ex;
         }
      }
   }

   public byte[] read(int max) throws IOException {
      synchronized (lock) {
         checkStream();
         try {
            byte[] b=new byte[max<=_is.available()?max:_is.available()];
            _is.read(b);
            return b;
         } catch (IOException ex) {
            _is=null;
            raise(STREAMCLOSED).run();
            throw ex;
         }
      }
   }

   public long skip(long n) throws IOException {
      synchronized (lock) {
         checkStream();
         try {
            return _is.skip(n);
         } catch (IOException ex) {
            _is=null;
            raise(STREAMCLOSED).run();
            throw ex;
         }
      }
   }
}
