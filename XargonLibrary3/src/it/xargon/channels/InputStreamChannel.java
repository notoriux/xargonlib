package it.xargon.channels;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayDeque;
import java.util.Objects;

import it.xargon.util.Bitwise;

public class InputStreamChannel {
   private final static boolean BUFFER_NATIVE=true;
   
   private SelectorWorker worker=null;
   private InputStream source=null;
   private Pipe pipe=null;
   private SelectionKey writerKey=null;
   
   private ArrayDeque<ByteBuffer> queue=null;
   private Object queueLock=null;

   public InputStreamChannel(InputStream source, SelectorWorker worker) throws IOException {
      this.source=Objects.requireNonNull(source);
      this.worker=Objects.requireNonNull(worker);
      queue=new ArrayDeque<>();
      queueLock=new Object();
      this.pipe=Pipe.open();
      this.pipe.sink().configureBlocking(false);
      writerKey=worker.register(pipe.sink(), SelectionKey.OP_WRITE, writerProcessor);
   }
   
   private SelectionProcessor writerProcessor=new SelectionProcessor() {
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
         //Sink pronto per la scrittura!
         ByteBuffer nioBuffer=null;
         
         //Ci sono vecchi dati in attesa di essere scritti?
         synchronized (queueLock) {
            while ((nioBuffer=queue.peekLast())!=null) {//sì, procediamo un buffer per volta
               //tentiamo di riversarlo sul sink
               pipe.sink().write(nioBuffer);
               if (nioBuffer.remaining()==0) {//buffer riversato completamente, rimuovere dalla coda
                  queue.remove(nioBuffer);
               } else { //buffer riversato parzialmente, indica che il buffer nativo del sink è pieno
                  return null; //appuntamento al prossimo giro, i dati in entrata aspetteranno
               }
            }
         }
         
         //A questo punto la coda di dati in attesa è vuota. Lavoriamo un po'
         //con i dati presenti in stream (se ce ne sono)
         int avail=source.available();
         if (avail==0) {
            //non ci sono nemmeno dati pronti in stream, dobbiamo
            //affidare una lettura bloccante ad un thread separato
            //chiedendo di non consegnare più eventi di "pronto per la scrittura"
            //finché non saranno memorizzati un po' di dati in coda
            writerKey.cancel();
            return blockingRead;
         }
         
         //Ci sono anche un po' di dati in coda: preleviamoli tutti senza bloccare
         //e tentiamo di scriverli immediatamente sul sink
         byte[] ioBuffer=new byte[avail];
         source.read(ioBuffer);
         nioBuffer=wrap(ioBuffer, 0, avail);
         pipe.sink().write(nioBuffer);
         //Se è riuscito a riversare tutto sul sink, non c'è più niente da fare
         //altrimenti provvede a mettere in coda il buffer parzialmente "smangiato"
         //in attesa del prossimo evento di "pronto per la scrittura"
         if (nioBuffer.remaining()!=0) {
            synchronized (queueLock) {queue.addFirst(nioBuffer);}
         }
         
         return null;
      }
   };
   
   private Runnable blockingRead=new Runnable() {      
      @Override
      public void run() {
         //Se si è giunti qui, significa che il sink era pronto per la
         //scrittura ma non c'erano dati disponibili sullo stream. La
         //ricezione di eventi sul selettore era stata già fermata.
         //Questo codice è in esecuzione su un thread a parte, quindi
         //abbiamo relativa libertà di movimento.
         
         //Provvediamo a metterci in lettura sullo stream, che sarà quindi
         //una chiamata bloccante. Nel momento in cui la chiamata si conclude
         //correttamente, terminiamo di leggere tutto il contenuto disponibile,
         //memorizziamo il nuovo buffer in coda e ri-registriamo
         //il selector del sink per ricevere il prossimo evento di "pronto per la scrittura"
         byte[] ioBuffer=null;
         int input=0;
         boolean mustClose=false;
         try {
            input=source.read(); //bloccante
            if (input<0) {
               mustClose=true; //stream terminato
            } else {
               //contiamo quanti byte sono disponibili nel buffer nativo
               int avail=source.available();
               ioBuffer=new byte[avail+1];
               ioBuffer[0]=Bitwise.asByte(input); //inseriamo il primo byte letto
               if (avail>0) {
                  //leggiamo il resto dello stream
                  int read=source.read(ioBuffer, 1, avail);
                  if (read<0) mustClose=true; //stream terminato
               }
            }
         } catch (IOException ex) {mustClose=true;}
         
         if (mustClose) {
            //lo stream è stato chiuso, questo strumento non ha più ragione di esistere
            close(); //chiude tutte le altre risorse
            return; //termina il thread
         }
         
         ByteBuffer nioBuffer=wrap(ioBuffer, 0, ioBuffer.length);
         synchronized (queueLock) {queue.addFirst(nioBuffer);}
         writerKey=worker.register(pipe.sink(), SelectionKey.OP_WRITE, writerProcessor);
      }
   };
   
   private ByteBuffer wrap(byte[] buffer, int offset, int length) {
      if (BUFFER_NATIVE) {
         ByteBuffer bb=ByteBuffer.allocateDirect(buffer.length).put(buffer, offset, length);
         bb.flip();
         return bb;
      }
      
      return ByteBuffer.wrap(buffer, offset, length);
   }

   public Pipe.SourceChannel getSourceChannel() {
      return pipe.source();
   }
   
   public void close() {
      writerKey.cancel();
      try {source.close();} catch (IOException ignored) {}
      try {pipe.sink().close();} catch (IOException ignored) {}
      try {pipe.source().close();} catch (IOException ignored) {}
   }
}
