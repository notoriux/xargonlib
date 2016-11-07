package it.xargon.streams;

import java.io.*;
import it.xargon.util.Tools;
import it.xargon.util.Bitwise;
import it.xargon.util.Debug;
import it.xargon.util.Debug.Printable;

public class Identity implements Streamable, Printable {
   private byte[] publicRemoteKey=null;
   private byte[] publicLocalKey=null;
   private byte[] privateLocalKey=null;
   
   public byte[] getPublicRemoteKey() {
      if (publicRemoteKey==null) return null;
      return Tools.copyOf(publicRemoteKey);
   }

   public void setPublicRemoteKey(byte[] pubremkey) {
      if ((pubremkey!=null) && (pubremkey.length==0)) publicRemoteKey=null;
      publicRemoteKey=Tools.copyOf(pubremkey);
   }
   
   public byte[] getPublicLocalKey() {
      if (publicLocalKey==null) return null;
      return Tools.copyOf(publicLocalKey);
   }

   public void setPublicLocalKey(byte[] publockey) {
      if ((publockey!=null) && (publockey.length==0)) publicLocalKey=null;
      else publicLocalKey=Tools.copyOf(publockey);
   }
   
   public byte[] getPrivateLocalKey() {
      if (privateLocalKey==null) return null;
      return Tools.copyOf(privateLocalKey);
   }

   public void setPrivateLocalKey(byte[] prvlockey) {
      if ((prvlockey!=null) && (prvlockey.length==0)) privateLocalKey=null;
      else privateLocalKey=Tools.copyOf(prvlockey);
   }
   
   public static Identity unmarshal(InputStream istream) throws IOException {
      byte[] cache=new byte[4];
      
      istream.read(cache); byte[] pubremkey=new byte[Bitwise.byteArrayToInt(cache)]; istream.read(pubremkey);
      istream.read(cache); byte[] publockey=new byte[Bitwise.byteArrayToInt(cache)]; istream.read(publockey);
      istream.read(cache); byte[] prvlockey=new byte[Bitwise.byteArrayToInt(cache)]; istream.read(prvlockey);
      
      Identity result=new Identity();
      result.setPublicRemoteKey(pubremkey);
      result.setPublicLocalKey(publockey);
      result.setPrivateLocalKey(prvlockey);
      return result;
   }

   public void marshal(OutputStream ostream) throws IOException {
      ostream.write(Bitwise.intToByteArray(publicRemoteKey==null?0:publicRemoteKey.length));
      if (publicRemoteKey!=null) ostream.write(publicRemoteKey);
      ostream.write(Bitwise.intToByteArray(publicLocalKey==null?0:publicLocalKey.length));
      if (publicLocalKey!=null) ostream.write(publicLocalKey);
      ostream.write(Bitwise.intToByteArray(privateLocalKey==null?0:privateLocalKey.length));
      if (privateLocalKey!=null) ostream.write(privateLocalKey);
   }

   public String toString(String indent) {
      StringBuilder sb=new StringBuilder();
      sb.append(indent).append("ENCODED IDENTITY\n");
      sb.append(indent).append("  Public remote key: [").append(Debug.dumpBytes(publicRemoteKey)).append("]\n");
      sb.append(indent).append("  Public local key: [").append(Debug.dumpBytes(publicLocalKey)).append("]\n");
      sb.append(indent).append("  Private local key: [").append(Debug.dumpBytes(privateLocalKey)).append("]\n");
      return sb.toString();
   }
}
