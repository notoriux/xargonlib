package it.xargon.channels;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.util.Objects;

public class OutputStreamChannel {
   private final static int BUFFER_SIZE=4096;
   private final static boolean BUFFER_NATIVE=true;
   
   private OutputStream dest=null;
   private Pipe pipe=null;
   private SelectionKey readerKey=null;
   private ByteBuffer nioBuffer=BUFFER_NATIVE?ByteBuffer.allocateDirect(BUFFER_SIZE):ByteBuffer.allocate(BUFFER_SIZE);
   
   public OutputStreamChannel(OutputStream dest, SelectorWorker worker) throws IOException {
      this.dest=Objects.requireNonNull(dest);
      Objects.requireNonNull(worker);
      this.pipe=Pipe.open();
      this.pipe.source().configureBlocking(false);
      readerKey=worker.register(pipe.source(), SelectionKey.OP_READ, readerProcessor);
   }

   private SelectionProcessor readerProcessor=new SelectionProcessor() {   
      @Override
      public Runnable processKey(SelectorWorker worker, SelectionKey key) {
         try {
            return _processKey(worker, key);
         } catch (IOException ex) {
            close();
            return null;
         } catch (Exception ex) {
            throw new RuntimeException(ex);
         }
      }
      
      private Runnable _processKey(SelectorWorker worker, SelectionKey key) throws IOException {
         //Pronto per la lettura!
         int read=pipe.source().read(nioBuffer);
         if (read<0) close();
         if (read<=0) return null;
         nioBuffer.flip();
         byte[] ioBuffer=new byte[read];
         nioBuffer.get(ioBuffer);
         nioBuffer.position(0);
         nioBuffer.limit(nioBuffer.capacity());
         dest.write(ioBuffer);
         return null;
      }
   };
   
   public Pipe.SinkChannel getSinkChannel() {
      return pipe.sink();
   }
   
   public void close() {
      readerKey.cancel();
      try {dest.close();} catch (IOException ignored) {}
      try {pipe.sink().close();} catch (IOException ignored) {}
      try {pipe.source().close();} catch (IOException ignored) {}
   }
}
