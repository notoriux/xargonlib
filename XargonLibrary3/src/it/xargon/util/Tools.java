package it.xargon.util;

import java.io.*;
import java.lang.reflect.Array;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormatSymbols;
import java.util.*;

import javax.swing.SwingUtilities;

public class Tools {
   private Tools() {}
   
   public static int getArrayDimCount(Object array) {
      if (array==null) throw new NullPointerException();      
      int dim=0;
      Class<?> c = array.getClass();
      while (c.isArray()) {c = c.getComponentType();dim++;}
      return dim;
   }
   
   public static int[] getArrayDimensions(Object array) {
      int[] dims=new int[getArrayDimCount(array)];
      
      Object arobj=array;
      for(int cnt=0;cnt<dims.length;cnt++) {
         dims[cnt]=Array.getLength(arobj);
         if (dims[cnt]>0) arobj=Array.get(arobj, 0);
      }
      
      return dims;
   }
   
   public static Class<?> getBaseComponentType(Object array) {
      if (array==null) throw new NullPointerException();
      Class<?> c = array.getClass();
      if (!c.isArray()) return null;
      while (c.isArray()) c = c.getComponentType();
      return c;
   }
   
   public static int[] nextArrayIndex(int[] indexes, int[] limits) {
      if ((indexes==null) || (limits==null)) throw new NullPointerException();
      if (indexes.length!=limits.length) throw new IllegalArgumentException();
      
      int[] result=Arrays.copyOf(indexes, limits.length);
      int sel=result.length-1;

      while (sel>=0) {
         if (result[sel]<(limits[sel]-1)) {result[sel]++;return result;}
         result[sel]=0;
         sel--;
      }
      
      return null;
   }
   
   public static Object getArrayValue(Object array, int[] indexes) {
      if ((array==null) || (indexes==null)) throw new NullPointerException();
      Object result=array;
      for(int cnt=0;cnt<indexes.length;cnt++) result=Array.get(result, indexes[cnt]);
      return result;
   }
   
   public static void setArrayValue(Object array, int[] indexes, Object value) {
      if ((array==null) || (indexes==null)) throw new NullPointerException();
      Object target=array;
      for(int cnt=0;cnt<indexes.length-1;cnt++) target=Array.get(target, indexes[cnt]);
      Array.set(target, indexes[indexes.length-1], value);
   }
   
   public static void ensureSwingThread(Runnable operation) {
      if (SwingUtilities.isEventDispatchThread()) operation.run();
      else SwingUtilities.invokeLater(operation);
   }
   
   public static byte[] hash(char[] password) {
      MessageDigest md=null;
      try {md = MessageDigest.getInstance("MD5");}
      catch (NoSuchAlgorithmException e) {return null;}

      for(char ch:password) {
         byte b1=(byte)((ch >>8) & 0x00FF);
         byte b2=(byte)(ch & 0x00FF);
         md.update(b1);
         md.update(b2);
      }
      return md.digest();
   }
   
   public static int forceRead(InputStream in, byte[] buffer, boolean tolerant) throws IOException {
      if (buffer.length==0) return 0;
      
      int off=0;
      int len=buffer.length;
      int stored=0;
      
      do {
         stored=in.read(buffer, off, len);
         if (stored!=-1) {off+=stored;len-=stored;}
      } while (off<buffer.length && stored!=-1);
      
      //se tolerant è falso, nel caso vengano letti meno byte di quelli attesi
      //lanciamo un'eccezione
      if (off!=buffer.length && !tolerant) throw new IOException("Unexpected end of stream: " + buffer.length + " bytes requested, " + off + " read");

      if (off==0) off=-1;
      //se non ha letto nulla (perchè stored=-1) siamo giunti alla fine dello stream

      return off;
   }
   
   public static byte[] copyOf(byte[] original) {
      if (original==null) throw new NullPointerException();
      byte[] result=new byte[original.length];
      System.arraycopy(original, 0, result, 0, original.length);
      return result;
   }
   
   public static byte[] copyOfRange(byte[] original, int from, int to) {
      int newLength = to - from;
      if (newLength < 0) throw new IllegalArgumentException(from + " > " + to);
      byte[] copy = new byte[newLength];
      System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
      return copy;
   }
   
   public static int waitProcessTimeout(Process proc, long timeout) throws InterruptedException {
      long desttime=System.currentTimeMillis() + timeout;
      int exitcode=-1;
      boolean gotExitCode=false;
      
      do {
         try {
            exitcode=proc.exitValue();
            gotExitCode=true;
         } catch (IllegalThreadStateException ex) {
            gotExitCode=false;
         }
         Thread.sleep(100);
      } while ((!gotExitCode) && (System.currentTimeMillis()<desttime));
      
      if (!gotExitCode) throw new IllegalStateException("timeout while waiting for process");
      
      return exitcode;
   }
   
   public static Class<?> getTypeForName(String className) throws ClassNotFoundException {
      if (className.equals(java.lang.Void.TYPE.getName())) return java.lang.Void.TYPE;
      else if (className.equals(java.lang.Boolean.TYPE.getName())) return java.lang.Boolean.TYPE;
      else if (className.equals(java.lang.Byte.TYPE.getName())) return java.lang.Byte.TYPE;
      else if (className.equals(java.lang.Short.TYPE.getName())) return java.lang.Short.TYPE;
      else if (className.equals(java.lang.Character.TYPE.getName())) return java.lang.Character.TYPE;
      else if (className.equals(java.lang.Integer.TYPE.getName())) return java.lang.Integer.TYPE;
      else if (className.equals(java.lang.Float.TYPE.getName())) return java.lang.Float.TYPE;
      else if (className.equals(java.lang.Long.TYPE.getName())) return java.lang.Long.TYPE;
      else if (className.equals(java.lang.Double.TYPE.getName())) return java.lang.Double.TYPE;
      return Class.forName(className);
   }
   
   public static Class<?> getBoxingClass(Class<?> original) {
      if (original.equals(java.lang.Void.TYPE)) return java.lang.Void.class;
      else if (original.equals(java.lang.Boolean.TYPE)) return java.lang.Boolean.class;
      else if (original.equals(java.lang.Byte.TYPE)) return java.lang.Byte.class;
      else if (original.equals(java.lang.Short.TYPE)) return java.lang.Short.class;
      else if (original.equals(java.lang.Character.TYPE)) return java.lang.Character.class;
      else if (original.equals(java.lang.Integer.TYPE)) return java.lang.Integer.class;
      else if (original.equals(java.lang.Float.TYPE)) return java.lang.Float.class;
      else if (original.equals(java.lang.Long.TYPE)) return java.lang.Long.class;
      else if (original.equals(java.lang.Double.TYPE)) return java.lang.Double.class;
      return original;      
   }
   
   public static interface Transformer<T,V> {public V transform(T obj);}
   
   public static <K,T,V> K inverseLookup(Map<K,T> map, Transformer<T,V> transformer, V value) {
      synchronized (map) {
         for(Map.Entry<K,T> entry: map.entrySet()) {
            if (transformer.transform(entry.getValue()).equals(value)) return entry.getKey();
         }
      }
      return null;
   }
   
   public static <K,V> K inverseLookup(Map<K,V> map, V value) {
      synchronized (map) {
         for(Map.Entry<K,V> entry: map.entrySet()) {
            if (entry.getValue().equals(value)) return entry.getKey();
         }
      }
      return null;
   }
   
   public static String parseFileSize(long size) {
      return parseMetric(size, 1024, new String[] {"byte|bytes", "Kbyte|Kbytes", "Mbyte|Mbytes", "Gbyte|Gbytes", "Tbyte|Tbytes"});
   }
   
   public static String parseWeight(long weight) {
      return parseMetric(weight, 10, new String[] {"grammo|grammi",
                                                      "decigrammo|decigrammi",
                                                      "etto|etti",
                                                      "chilo|chili",
                                                      "miriagrammo|miriagrammi",
                                                      "quintale|quintali",
                                                      "tonnellata|tonnellate"});      
   }
   
   public static String parseMetric(long value, int radix, String[] metrics) {
      int mcnt=0;
      double ivalue=value;
      
      while ((ivalue>=radix) && (mcnt<metrics.length-1)) {
         mcnt++;
         ivalue=ivalue/radix;
      }
      
      String partial=String.format("%1$1.3f",ivalue);
      char commaChar=new DecimalFormatSymbols().getDecimalSeparator();
      int comma=partial.indexOf(commaChar)-1;
      int backcnt=partial.length()-1;
      while (((partial.charAt(backcnt)=='0') || (partial.charAt(backcnt)==commaChar)) && (backcnt>comma)) backcnt--;
      partial=partial.substring(0, backcnt+1);
      
      String suffix=metrics[mcnt];
      int suffpipe=suffix.indexOf("|");
      if (suffpipe>=0) {if (ivalue==1) suffix=suffix.substring(0,suffpipe); else suffix=suffix.substring(suffpipe+1);}
      
      return partial + " " + suffix;
   }
   
   public static int waitFor(InputStream is, byte[][] matches) throws IOException {
      return waitFor(is, matches, null);
   }
   
   public static int waitFor(InputStream is, byte[][] matches, OutputStream os) throws IOException {
      int maybematch=-1;
      int mmcount=0;
      int nomorematch=0;
      byte bscan=0;

      if (matches.length==0) return -1;
      
      try {
         for(int scan=is.read();scan>-1;scan=is.read()) {
            //forziamo a byte l'intero appena letto, per confrontarlo correttamente con i byte delle ricerche da fare
            bscan=Bitwise.asByte(scan);
            
            if (maybematch==-1) { //Se non è stata ancora trovata una corrispondenza
               for(int i=0;i<matches.length;i++) {
                  if (matches[i].length>0) { //è già capitato di avere una lista vuota per i confronti
                                             //in tal caso, non ci scandalizziamo troppo
                     if (matches[i][0]==bscan) {
                        //Il primo carattere del confronto "i" è stato verificato
                        //prendiamo nota e aspettiamo i prossimi caratteri
                        maybematch=i;mmcount=1;
                        //Se il pattern di cui è stato verificato il primo byte è lungo esattamente 1 byte
                        if ((maybematch!=-1) && (matches[maybematch].length==1)) {
                           //La ricerca è finita, restituisci il risultato
                           return maybematch;
                        }
                        break;
                     }
                  }
               }
               
               //se non c'è stata nessuna verifica, buttiamo il carattere esaminato dall'altra parte
               if ((maybematch==-1) && (os!=null)) {
                  os.write(scan);
               }
            } else { //...è in corso una verifica per il pattern identificato da "maybematch", sono già stati verificati almeno "mmcount" byte
               if (bscan==matches[maybematch][mmcount]) {
                  mmcount++;
                  //Se il numero di caratteri già controllato supera la lunghezza del pattern
                  //sotto controllo, la verifica è riuscita
                  if (mmcount==matches[maybematch].length) {
                     return maybematch;
                  }
               } else {
                  //l'ultimo carattere letto non corrisponde alla posizione corrispondente
                  //nel pattern sotto controllo, è possibile però che ci siano altri pattern
                  //più lunghi che iniziano allo stesso modo: cerchiamoli
                  nomorematch=maybematch;
                  maybematch=-1;
                  for(int i=0;i<matches.length;i++) {
                     if (matches[i].length>mmcount) {
                        //controlliamo che almeno i primi mmcount caratteri corrispondano
                        boolean rematch=true;
                        for(int j=0;j<mmcount;j++) {if (matches[i][j]!=matches[nomorematch][j]) {rematch=false;break;}}
                        if (rematch) { //nuovo pattern probabile, verifichiamo che il carattere in scan corrisponda alla
                                       //posizione corrente
                           if (bscan==matches[i][mmcount]) { //possiamo proseguire la verifica
                              maybematch=i;
                              if (mmcount==matches[maybematch].length) return maybematch; //già trovata una corrispondenza
                              mmcount++;
                              break; //al prossimo giro (del for esterno) continua verificando contro il nuovo pattern trovato
                           }
                        }
                     }
                  }
                  
                  //Nessuno dei pattern a disposizione inizia allo stesso modo, ma può esserci
                  //un pattern che abbia come primo carattere proprio quello che ha causato l'interruzione
                  //del confronto. Cerchiamo anche quello.
                  for(int i=0;i<matches.length;i++) {
                     if (bscan==matches[i][0]) { //trovato!
                        maybematch=i;
                        mmcount=1;
                        break;
                     }
                  }
                  
                  //All'uscita dei loop qui sopra:
                  //   - maybematch rimane -1 se non ci sono più pattern probabili (e quindi deve proseguire la ricerca)
                  //   - nomorematch contiene l'ultimo tentativo fallito (come indice in matches)
                  //   - mmcount contiene il numero di caratteri corretto fino al fallimento
                  //   - scan contiene il carattere che ha causato il fallimento nella ricerca.
                  //Buttiamo tutto nello stream dall'altra parte
                  if ((maybematch==-1) && (os!=null)) {
                     os.write(matches[nomorematch], 0, mmcount);
                     os.write(scan);
                  }
               }
            }
         }
         
         //Il flusso è terminato, se era in corso una verifica per una
         //possibile corrispondenza va considerata fallita. Riportiamo
         //nello stream dall'altra parte i caratteri finora verificati
         if ((maybematch!=-1) && (os!=null)) {
            os.write(matches[maybematch], 0, mmcount);
         }   
      } catch (java.net.SocketTimeoutException ignored) {}
      return -1;
   }
   
   public static void replaceInStream(InputStream in, HashMap<String,String> subst, OutputStream out) throws IOException {
      ArrayList<byte[]> searchList=new ArrayList<byte[]>();
      
      for(String search:subst.keySet()) {
         byte[] bsearch=search.getBytes();
         searchList.add(bsearch);
      }
      
      byte[][] searchBytes=searchList.toArray(new byte[searchList.size()][]);
      
      int scan=0;
      do {
         scan=Tools.waitFor(in, searchBytes, out);
         if (scan!=-1) {
            String found=new String(searchBytes[scan]);
            String replaceWith=subst.get(found);
            out.write(replaceWith.getBytes());
            out.flush();
         }
      } while (scan!=-1);
      
      out.flush();
   }
}
