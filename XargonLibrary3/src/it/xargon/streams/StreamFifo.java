package it.xargon.streams;

public class StreamFifo {
   private byte[] buffer=null;
   private int head=0; //dove andrà scritto il prossimo byte
   private int tail=0; //da dove verrà letto il prossimo byte
   private int freespace=0;
   private volatile boolean fullbuffer=false;
   private volatile boolean closed=false;
   
   public StreamFifo(int size) {
      buffer=new byte[size];
      freespace=size;
   }
   
   public boolean isOpen() {synchronized (buffer) {return !closed;}}
   
   public boolean isReadable() {
      checkClosed();
      synchronized (buffer) {
         if (buffer.length-freespace>0) return true;
         return !closed;
      }
   }
   
   public void close() {synchronized (buffer) {closed=true;buffer.notify();}}
   
   private void checkClosed() {if (closed) throw new IllegalStateException();}
   
   public void write(int i) throws InterruptedException {
      synchronized (buffer) {
         checkClosed();
         if (fullbuffer) {buffer.wait();checkClosed();}
         buffer[head]=(byte)(i & 0x00FF);
         freespace--;
         
         //incrementa il puntatore di scrittura o torna indietro al raggiungimendo del confine
         head=(head==buffer.length-1)?0:head+1;
         
         if (head==tail) fullbuffer=true; //il buffer ora è pieno, un'ulteriore scrittura sovrapporrebbe la coda
         buffer.notify();
      }
   }
   
   public void write(byte[] data) throws InterruptedException {write(data, 0, data.length);}
   
   public void write(byte[] data, int off, int len) throws InterruptedException {
      synchronized (buffer) {
         for(int cnt=0;cnt<len;cnt++) write(data[cnt+off]);
      }
   }
   
   public boolean writeNoWait(int i) {
      synchronized (buffer) {
         checkClosed();
         if (freespace==0) return false;
         try {write(i);} catch (InterruptedException ex) {return false;}
         return true;
      }
   }
   
   public int read() throws InterruptedException {
      byte result=0;
      synchronized (buffer) {      
         if ((tail==head) && (!fullbuffer)) {
            checkClosed();
            buffer.wait();
            checkClosed();
         }
         result=buffer[tail];
         freespace++;
         //incrementa il puntatore di lettura o torna indietro al raggiungimendo del confine
         tail=(tail==buffer.length-1)?0:tail+1;
         fullbuffer=false; //in ogni caso, una lettura svuota il buffer di un byte
         buffer.notify();
      }
      return result;
   }
   
   public int read(byte[] data) throws InterruptedException {return read(data, 0, data.length);}
   
   public int read(byte[] data, int off, int len) throws InterruptedException {
      synchronized (buffer) {
         checkClosed();
         for(int cnt=0;cnt<len;cnt++) {
            try {
               data[off+cnt]=(byte)(read() & 0x00FF);
            } catch (IllegalStateException ex) {
               return cnt-1;
            }
         }
      }
      return len;
   }
   
   public int readNoWait() {
      int rd=0;
      synchronized (buffer) {
         if (buffer.length-freespace==0) {
            checkClosed();return -1;
         }
         try {rd=read();} catch (InterruptedException ex) {return -1;}
      }
      return rd;
   }
   
   public int fill(byte[] data) {
      //copia il contenuto di "data" nel buffer fino al riempimento
      //o fino al limite di "data". Non bloccante.
      //restituisce il numero di byte scritti
      int cnt=0;
      synchronized (buffer) {
         checkClosed();
         cnt=(freespace<data.length)?freespace:data.length;
         for(int wr=0;wr<cnt;wr++) try {write(data[wr]);} catch (InterruptedException ignored) {}
      }
      return cnt;
   }
   
   public int drain(byte[] data) {return drain(data, 0, data.length);}
   
   public int drain(byte[] data, int off, int len) {
      //svuota il buffer fino al riempimento di "data" o finchè il buffer non sia vuoto. Non bloccante
      //restituisce il numero di byte letti
      int cnt=0;
      synchronized (buffer) {
         int readable=buffer.length-freespace;
         if ((readable==0) && (closed)) throw new IllegalStateException();
         cnt=((readable)<len)?(readable):len;
         for(int rd=0;rd<cnt;rd++) try {data[off + rd]=(byte)(read() & 0x00FF);} catch (InterruptedException ignored) {}
      }
      return cnt;
   }
   
   public int skip(int n) throws InterruptedException {
      synchronized (buffer) {
         checkClosed();
         for(int cnt=0;cnt<n;cnt++) {
            try {
               read();
            } catch (IllegalStateException ex) {
               return cnt-1;
            }
         }
      }
      return n;
   }
   
   public int peek(byte[] data) {
      int cnt=0;
      synchronized (buffer) {
         int readable=buffer.length-freespace;
         if ((readable==0) && (closed)) throw new IllegalStateException();
         cnt=((readable)<data.length)?(readable):data.length;
         int curpos=tail;
         for(int rd=0;rd<cnt;rd++) {
            data[rd]=buffer[curpos];
            curpos=(curpos==buffer.length-1)?0:curpos+1;
         }
      }
      return cnt;
   }
   
   public int peek() {
      synchronized (buffer) {
         if ((buffer.length==freespace) && (closed)) throw new IllegalStateException();
         return buffer[tail];
      }
   }
   
   public int getSize() {return buffer.length;}
   
   public void waitForRead() throws InterruptedException {
      synchronized (buffer) {
         if ((tail==head) && (!fullbuffer)) {checkClosed();buffer.wait();checkClosed();}
      }
   }
   
   public void waitForWrite() throws InterruptedException {
      synchronized (buffer) {
         checkClosed();
         if (fullbuffer) {buffer.wait();checkClosed();}
      }
   }

   public int getReadableData() {
      int result=0;
      synchronized (buffer) {
         result=buffer.length-freespace;
         if ((result==0) && (closed)) throw new IllegalStateException();
      }
      return result;
   }
   
   public int getWritableData() {
      synchronized (buffer) {
         if (closed) throw new IllegalStateException();
         return freespace;
      }
   }
}
