package it.xargon.util;

import java.io.*;
import java.util.*;

public class Identifier implements Cloneable, Comparable<Identifier>, Serializable {
   protected final static byte BZERO=Bitwise.asByte(0);
   protected final static Identifier IDZERO=new Identifier();

   protected byte[] contents=null;
   
   public Identifier() {contents=new byte[0];}
   public Identifier(byte[] data) {this();_setData(data);}
   
   public static Identifier readIdentifier(InputStream istream) throws IOException {
      int size=istream.read();
      return readIdentifier(size, istream);
   }

   public static Identifier readIdentifier(int size, InputStream istream) throws IOException {
      byte[] cache=new byte[size];
      istream.read(cache);
      return new Identifier(cache);
   }

   protected void _setData(byte[] data) {
      if (data==null) throw new IllegalArgumentException();
      if (data.length>255) throw new IllegalArgumentException();
      if (data.length>=contents.length) contents=new byte[data.length];
      System.arraycopy(data, 0, contents, contents.length - data.length, data.length);
      shrink();
   }

   public int getSize() {return contents.length;}
   public byte[] getData() {return getData(null);}
   public byte[] getData(byte[] buffer) {
      byte[] ibuf=null;
      if ((buffer==null) || (buffer.length<contents.length)) ibuf=new byte[contents.length]; else ibuf=buffer;
      System.arraycopy(contents, 0, ibuf, 0, ibuf.length);
      return ibuf;
   }
   
   public void writeOn(OutputStream ostream) throws IOException {
      ostream.write(contents.length);
      ostream.write(contents);
   }

   protected synchronized Identifier _increment() {increment(contents.length-1);return this;}
   
   public synchronized Identifier next() {return dup()._increment();}
   
   private void increment(int comporder) {
      if (comporder<0) {
         if (contents.length==255) {
            contents=new byte[0];
         } else {
            byte[] oldcontents=contents;
            contents=new byte[oldcontents.length + 1];
            System.arraycopy(oldcontents, 0, contents, 1, oldcontents.length);
            contents[0]++;
         }
      } else if (Bitwise.asInt(contents[comporder])==0xFF) {
         contents[comporder]=BZERO;
         increment(comporder-1);
      } else {
         contents[comporder]++;
      }
   }
   
   protected synchronized Identifier _decrement() {
      if (compareTo(IDZERO)==0) throw new ArithmeticException();
      decrement(contents.length-1);
      shrink();
      return this;
   }
   
   public synchronized Identifier previous() {return dup()._decrement();}
   
   private void decrement(int comporder) {
      if (comporder<0) {
         throw new ArithmeticException();
      } else if (contents[comporder]==BZERO) {
         decrement(comporder-1);
         contents[comporder]=Bitwise.asByte(0xFF);
      } else {
         contents[comporder]--;
      }
   }
   
   private void shrink() {
      int cnt=0;
      while(cnt<contents.length) {if (contents[cnt]!=0) break; else cnt++;}
      contents=(cnt==contents.length)?new byte[0]:Tools.copyOfRange(contents, cnt, contents.length);
   }

   public int hashCode() {
      return Bitwise.sequenceToInt((contents.length<4)?0:contents[contents.length-4],
                                    (contents.length<3)?0:contents[contents.length-3],
                                    (contents.length<2)?0:contents[contents.length-2],
                                    (contents.length<1)?0:contents[contents.length-1]);
   }

   public int compareTo(Identifier o) {
      int mybyte=BZERO;
      int otherbyte=BZERO;
      
      for(int cnt=1;cnt<=Math.max(contents.length, o.contents.length);cnt++) {
         mybyte=Bitwise.asInt((contents.length - cnt)<0?BZERO:contents[contents.length - cnt]);
         otherbyte=Bitwise.asInt((o.contents.length - cnt)<0?BZERO:o.contents[o.contents.length - cnt]);
         if (mybyte>otherbyte) return 1;
         if (mybyte<otherbyte) return -1;
      }

      return 0;
   }
   
   public boolean equals(Object o) {
      if (!(o instanceof Identifier)) return false;
      return compareTo(Identifier.class.cast(o))==0;
   }
   
   public synchronized Object clone() {return dup();}
   
   public String toString() {
      if (contents.length==0) return "00";
      StringBuilder result=new StringBuilder();
      Formatter fmt=new Formatter(result);
      for(byte b:contents) fmt.format("%1$02X", b);
      fmt.flush();fmt.close();
      return result.toString();
   }
   
   public Identifier dup() {return new Identifier(contents);}
}
