package it.xargon.streams;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;

public class ImmediateOutputStream extends OutputStream {
   private OutputStream ios=null;
   public ImmediateOutputStream(OutputStream os) {ios=os;}
   public void close() throws IOException {
      try {ios.close();} catch (SocketException ignored) {}
   }
   public void flush() throws IOException {
      try {ios.flush();} catch (SocketException ignored) {}            
   }
   public void write(int b) throws IOException {
      try {ios.write(b);ios.flush();} catch (SocketException ignored) {}            
   }
   public void write(byte[] b) throws IOException {
      try {ios.write(b);ios.flush();} catch (SocketException ignored) {}
   }
   public void write(byte[] b, int off, int len) throws IOException {
      try {ios.write(b,off,len);ios.flush();} catch (SocketException ignored) {}            
   }
}