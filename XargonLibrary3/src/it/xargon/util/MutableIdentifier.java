package it.xargon.util;

import java.io.*;

public class MutableIdentifier extends Identifier {
   public MutableIdentifier() {super();}
   public MutableIdentifier(byte[] data) {super(data);}

   public static MutableIdentifier readIdentifier(InputStream istream) throws IOException {
      int size=istream.read();
      return readIdentifier(size, istream);
   }
   
   public static MutableIdentifier readIdentifier(int size, InputStream istream) throws IOException {
      byte[] cache=new byte[size];
      istream.read(cache);
      return new MutableIdentifier(cache);
   }
   
   public void setData(byte[] data) {_setData(data);}

   public synchronized MutableIdentifier increment() {_increment(); return this;}
      
   public synchronized MutableIdentifier decrement() {_decrement(); return this;}
   
   public synchronized Object clone() {return dup();}
   
   public MutableIdentifier dup() {return new MutableIdentifier(contents);}
}
