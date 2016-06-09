package it.xargon.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import it.xargon.events.*;

public class StreamFilter extends EventsSourceImpl {
   public static interface FilterContents {
      public byte[] getOriginal();
      public void setSubstitute(byte[] subst);
   }
   
   @FunctionalInterface @Event
   public interface Matched {public void matched(FilterContents subst);}
   public static Class<Matched> MATCHED=Matched.class;
   
   public final static byte[] NODATA=new byte[0];
   private ByteArrayOutputStream buf=null;
   private byte[] pat=null;
   private int matched=0;
   private FilterContentsImpl subst=null;
   
   private class MonoFilter implements Matched {
      private byte[] res=null;

      public MonoFilter(String result) {
         if (result==null) res=new byte[0]; else res=result.getBytes();
      }

      public MonoFilter(byte[] result) {
         if (result==null) res=new byte[0]; else {
            res=new byte[result.length];
            System.arraycopy(result,0,res,0,result.length);
         }
      }

      public void matched(FilterContents isubst) {isubst.setSubstitute(res);}
   }
   
   private class FilterContentsImpl implements FilterContents {
      private byte[] orig=null;
      private byte[] sub=null;

      public FilterContentsImpl(byte[] original) {
         setOriginal(original);
      }

      public void setOriginal(byte[] original) {
         orig=new byte[original.length];
         sub=new byte[original.length];
         System.arraycopy(original,0,orig,0,original.length);
         System.arraycopy(original,0,sub,0,original.length);
      }

      public byte[] getOriginal() {
         byte[] res=new byte[orig.length];
         System.arraycopy(orig,0,res,0,orig.length);
         return res;
      }

      public void setSubstitute(byte[] subst) {
         if (subst==null) sub=new byte[0]; else {
            if (subst.length!=sub.length) sub=new byte[subst.length];
            System.arraycopy(subst,0,sub,0,subst.length);
         }
      }

      public byte[] getSubstitute() {
         byte[] res=new byte[sub.length];
         System.arraycopy(sub,0,res,0,sub.length);
         return res;
      }
   }

   public static StreamFilter newStringReplacer(String original, String replace) {
      if ((original==null) || (original.length()==0))
         throw new IllegalArgumentException("Original string must have at least one character");
      StreamFilter res=new StreamFilter(original.getBytes());
      res.onEvent(MATCHED, res.new MonoFilter(replace));
      return res;
   }

   public static StreamFilter newStreamReplacer(byte[] original, byte[] replace) {
      if ((original==null) || (original.length==0))
         throw new IllegalArgumentException("Original string must have at least one character");
      StreamFilter res=new StreamFilter(original);
      res.onEvent(MATCHED, res.new MonoFilter(replace));
      return res;
   }

   public StreamFilter(byte[] pattern) {
      if (pattern==null) throw new NullPointerException("Pattern must not be null");
      buf=new ByteArrayOutputStream();
      pat=pattern;
      subst=new FilterContentsImpl(pat);
   }

   public byte[] getPattern() {
      byte[] res=new byte[pat.length];
      System.arraycopy(pat,0,res,0,pat.length);
      return res;
   }

   public byte[] flush() {
      byte[] res=new byte[matched];

      if (matched==0) return NODATA;
      System.arraycopy(pat,0,res,0,matched);
      matched=0;
      return res;
   }

   public String feed(String source) {
      ByteArrayOutputStream ibuf=new ByteArrayOutputStream();
      if ((source==null) || (source.length()==0)) return "";
      flush();
      try {
         ibuf.write(feed(source.getBytes()));
         ibuf.write(flush());
         ibuf.flush();
         ibuf.close();
      } catch(IOException ex) {}
      return new String(ibuf.toByteArray());
   }

   public byte[] feed(byte[] source) {
      buf.reset();
      try {
         for(int i=0; i<source.length; i++)
            buf.write(feed(source[i]));
      } catch (IOException ex) {
         ex.printStackTrace();
      }
      return buf.toByteArray();
   }

   public byte[] feed(byte source) {
      byte[] res={source};
      if (source!=pat[matched]) {
         if (matched==0) return res; else {
            res=new byte[matched+1];
            System.arraycopy(pat,0,res,0,matched);
            res[matched]=source;
            matched=0;
            return res;
         }
      }
      matched++;
      if (matched==pat.length) {
         matched=0;
         raise(MATCHED).matched(subst);
         return (byte[])(subst.getSubstitute()).clone();
      }
      return NODATA;
   }
}
