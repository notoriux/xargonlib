package it.xargon.streams;

import java.io.*;

public class DuplexChannel {
   private InputStream iis=null;
   private OutputStream ios=null;
   private Identity iident=null;
   
   public DuplexChannel(InputStream is, OutputStream os, Identity ident) {
      iis=is;ios=os;iident=ident;
   }
   
   public InputStream getInputStream() {return iis;}
   public void setInputStream(InputStream is) {iis=is;}
   
   public OutputStream getOutputStream() {return ios;}
   public void setOutputStream(OutputStream os) {ios=os;}
   
   public Identity getIdentity() {return iident;}
   public void setIdentity(Identity ident) {iident=ident;}
   
   public void close() {
      try {iis.close();ios.close();} catch (IOException ex) {}
   }
}
