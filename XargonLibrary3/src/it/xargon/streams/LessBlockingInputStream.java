package it.xargon.streams;

import java.io.IOException;
import java.io.InputStream;

/**
 * Questa implementazione di InputStream devia leggermente dal contratto originario.
 * In particolare consente di leggere da un InputStream tutti i byte disponibili senza
 * bloccare, nel caso in cui il buffer di lettura utilizzato sia più grande del
 * buffer interno.
 * Questo InputStream in pratica è bloccante solo se l'InputStream sottostante non ha
 * più byte disponibili.
 * @author Francesco Muccilli
 */

public class LessBlockingInputStream extends InputStream {
   private InputStream is=null;
   
   public LessBlockingInputStream(InputStream original) {is=original;}
   public InputStream getInputStream() {return is;}
   public void setinputStream(InputStream newstream) {is=newstream;}
   public int available() throws IOException {return is.available();}
   public void close() throws IOException {is.close();}
   public boolean equals(Object obj) {return is.equals(obj);}
   public int hashCode() {return is.hashCode();}
   public boolean markSupported() {return is.markSupported();}
   public synchronized void mark(int readlimit) {is.mark(readlimit);}
   public synchronized void reset() throws IOException {is.reset();}
   public long skip(long n) throws IOException {return is.skip(n);}
   public String toString() {return is.toString();}
   public int read() throws IOException {return is.read();}
   public int read(byte[] b) throws IOException {return read(b, 0, b.length);}
   public int read(byte[] b, int off, int len) throws IOException {
      int cnt=0;
      int scan=0;
      int ioff=off;
      int ilen=len;
      int res=0;
      boolean wasBlocked=false;
      
      cnt=is.available();
      
      if (cnt==0) { //Un'eventuale chiamata di read() bloccherebbe il thread chiamante
         scan=is.read(); //blocca per il primo byte, o finchè non c'è almeno un byte disponibile
         wasBlocked=true;
         if (scan==-1) return -1; //Fine dello stream
         b[ioff]=(byte)(scan & 0xFF);
         ioff++;
         ilen--;
         cnt=is.available();
         if (cnt==0) return 1; //Il buffer nativo è di nuovo vuoto, ma abbiamo letto almeno un byte: usciamo
      }
      
      //C'è ancora qualcosa nel buffer nativo. Leggiamo fin che possiamo
      if (ilen<cnt) { //Il numero di byte voluti dal richiedente è inferiore al numero di byte disponibili
         res=is.read(b,ioff,ilen) + (wasBlocked?1:0); //Diamogli quello che vuole (aggiungendo 1 per il byte di blocco eventualmente già letto)
      } else { //Il numero di byte voluti dal richiedente è SUPERIORE al numero di byte disponibili
         //Questa è la ragion d'essere di questa funzione:
         //Dalla documentazione, il contratto generale di InputStream cita che il metodo deve bloccare
         //almeno finchè non ha letto tutti i byte richiesti. Questa implementazione invece legge finchè
         //può (svuotando il buffer nativo) e restituisce il controllo senza bloccare
         res=is.read(b,ioff,cnt) + (wasBlocked?1:0);
      }
            
      return res;
   }   
}
