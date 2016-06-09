package it.xargon.streams;

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import it.xargon.util.SimpleChecksum;

public class ChecksumOutputStream extends FilterOutputStream {
   private SimpleChecksum chk=null;

   public ChecksumOutputStream(OutputStream out) {
      super(out);
      chk=new SimpleChecksum();
   }
   
   public int getChecksum() {return chk.get();}
   
   public void writeChecksum() throws IOException {
      out.write(chk.get());
   }
   
   public void resetChecksum() {chk.reset();}

   public void write(int b) throws IOException {
      chk.feed(b);
      out.write(b);
   }
   
   public void write(byte[] b, int off, int len) throws IOException {
      chk.feed(b, off, len);
      out.write(b, off, len);
   }
}
