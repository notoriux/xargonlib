package it.xargon.streams;

import java.io.*;

import it.xargon.events.*;

public class RemotableOutputStream extends EventsSourceImpl implements IRemoteOutputStream {
   private Object lock=null;
   private OutputStream _os=null;
   
   public RemotableOutputStream(OutputStream os) {
      if (os==null) throw new NullPointerException();
      lock=new Object();
      _os=os;
   }
   
   public boolean isOpen() {
      synchronized (lock) {
         return _os!=null;         
      }
   }
   
   private void checkStream() throws IOException {
      if (!isOpen()) throw new IOException("Stream closed");      
   }

   public void close() throws IOException {
      synchronized (lock) {
         checkStream();
         try {
            _os.close();
         } catch (IOException ex) {
            throw ex;
         } finally {
            _os=null;
            raise(STREAMCLOSED).run();
         }
      }
   }

   public void flush() throws IOException {
      synchronized (lock) {
         checkStream();
         try {
            _os.flush();
         } catch (IOException ex) {
            _os=null;
            raise(STREAMCLOSED).run();
            throw ex;
         }
      }
   }

   public void write(byte[] b) throws IOException {
      synchronized (lock) {
         checkStream();
         try {
            _os.write(b);
         } catch (IOException ex) {
            _os=null;
            raise(STREAMCLOSED).run();
            throw ex;
         }
      }
   }

   public void write(int b) throws IOException {
      synchronized (lock) {
         checkStream();
         try {
            _os.write(b);
         } catch (IOException ex) {
            _os=null;
            raise(STREAMCLOSED).run();
            throw ex;
         }
      }
   }

}
