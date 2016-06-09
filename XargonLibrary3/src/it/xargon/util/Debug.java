package it.xargon.util;

import java.io.*;
import java.util.*;

public class Debug {
   public static BufferedReader stdin=null;
   public static PrintWriter stdout=null;
   public static PrintWriter stderr=null;
   private static HashMap<Thread, LinkedList<Long>> debugTimers=null;
   
   private Debug() {}
   
   static {
      stdin=new BufferedReader(new InputStreamReader(System.in));
      stdout=new PrintWriter(System.out, true);
      stderr=new PrintWriter(System.err, true);
      debugTimers=new HashMap<Thread, LinkedList<Long>>();
   }
   
   public abstract static class CheckedRunnable implements Runnable {
      private PrintWriter dbgout=null;
      
      public CheckedRunnable() {this(Debug.stdout);}
      public CheckedRunnable(PrintWriter out) {dbgout=out;}
      public void run() {
         enterMethod(dbgout);
         try {checkedRun();} catch (Throwable tr) {trace(dbgout, Debug.exceptionToString(tr));}
         exitMethod(dbgout);
      }
      public abstract void checkedRun();
   }
   
   public static void enterMethod() {enterMethod(Debug.stdout, null);}

   public static void enterMethod(String message) {enterMethod(Debug.stdout, message);}
   
   public static void enterMethod(PrintWriter out) {enterMethod(out, null);}
   
   private static void enterMethod(PrintWriter out, String message) {
      StackTraceElement call=Thread.currentThread().getStackTrace()[3];
      out.printf("--> %s.%s (%s:%d)", call.getClassName(), call.getMethodName(), call.getFileName(), call.getLineNumber());
      if (message!=null) {out.println();out.printf("    %s", message);}
      out.println();
      LinkedList<Long> timerStack=null;
      if (!debugTimers.containsKey(Thread.currentThread())) {
         timerStack=new LinkedList<Long>();
         debugTimers.put(Thread.currentThread(), timerStack);
      } else {
         timerStack=debugTimers.get(Thread.currentThread());
      }
      timerStack.addLast(System.currentTimeMillis());
   }
   
   public static void trace() {trace(Debug.stdout, null);}
   
   public static void trace(String message) {trace(Debug.stdout, message);}
   
   public static void trace(PrintWriter out) {trace(out, null);}
   
   public static void trace(PrintWriter out, String message) {
      long measure=System.currentTimeMillis();         
      LinkedList<Long> timerStack=debugTimers.get(Thread.currentThread());
      if ((timerStack!=null) & (timerStack.size()>0)) measure-=timerStack.peek(); else measure=0;
      StackTraceElement call=Thread.currentThread().getStackTrace()[3];
      out.printf("  | %s.%s (%s:%d)", call.getClassName(), call.getMethodName(), call.getFileName(), call.getLineNumber());
      if (measure>0) out.printf(" %d msec", measure);
      if (message!=null) {out.println();out.printf("    %s", message);}
      out.println();
   }

   public static void exitMethod() {exitMethod(Debug.stdout, null);}

   public static void exitMethod(String message) {exitMethod(Debug.stdout, message);}

   public static void exitMethod(PrintWriter out) {exitMethod(out, null);}
      
   public static void exitMethod(PrintWriter out, String message) {
      long measure=System.currentTimeMillis();         
      LinkedList<Long> timerStack=debugTimers.get(Thread.currentThread());
      if ((timerStack!=null) & (timerStack.size()>0)) measure-=timerStack.removeLast(); else measure=0;
      StackTraceElement call=Thread.currentThread().getStackTrace()[3];
      out.printf("<-- %s.%s (%s:%d)", call.getClassName(), call.getMethodName(), call.getFileName(), call.getLineNumber());
      if (measure>0) out.printf(" %d msec", measure);
      if (message!=null) {out.println();out.printf("    %s", message);}
      out.println();
   }
      
   public static void dumpBytes(byte[] data, PrintWriter out) {
      if (data==null) return;
      dumpBytes(data, 0, data.length, out);
   }

   public static void dumpBytes(byte[] data, int offset, int length, PrintWriter out) {
      if (data==null) return;
      for(int cnt=offset;cnt<offset+length;cnt++) {
         out.printf("%1$02X ", data[cnt]);
      }
      out.flush();
   }
   
   public static void dumpBytesFormatted(String indent, byte[] data, PrintWriter out) {
      dumpBytesFormatted(indent, data, 0, data.length, out);
   }

   public static void dumpBytesFormatted(String indent, byte[] data, int offset, int length, PrintWriter out) {
      int column=0;
      char[] disp=new char[16];
      
      for(int cnt=offset;cnt<offset+length;cnt++) {
         disp[column]=Bitwise.asChar(data[cnt]);
         if (disp[column]<32 || disp[column]>126) disp[column]='.';
         if (column==0) {out.print(indent);out.printf("%1$08X - ", cnt);}
         out.printf("%1$02X ", data[cnt]);
         if (column==7) out.print(": ");
         if (column==15) {out.print(" - ");dumpDisp(disp, column, out);out.println();}
         column=(column==15)?0:column+1;
      }
      
      if (column>0) {
         for(int i=column;i<16;i++) {
            out.print("   ");
            if (i==7) out.print(": ");
         }
         out.print(" - ");
         dumpDisp(disp, column-1, out);
         out.println();
      }
   }
   
   private static void dumpDisp(char[] disp, int cnt, PrintWriter out) {
      for(int i=0;i<=cnt;i++) out.print(disp[i]);
   }
   
   public static void dumpAllTreadStacks(PrintStream out) {
      out.println("*** Dumping StackTrace for all threads ***");
      Map<Thread,StackTraceElement[]> allDumps=Thread.getAllStackTraces();
      for(Map.Entry<Thread, StackTraceElement[]> dump:allDumps.entrySet()) {
         Thread thr=dump.getKey();
         out.println("******** THREAD " + thr.getId() + " \"" + thr.getName() + "\" is " + thr.getState().toString());
         StackTraceElement[] stackTrace=dump.getValue();
         for(StackTraceElement stackElem:stackTrace) out.println("* " + stackElem.toString());
         out.println();
      }
   }
   
   public static String exceptionToString(Throwable ex) {
      StringBuilder bld=new StringBuilder();
      
      bld.append(ex.toString());bld.append('\n');
      StackTraceElement[] trace=ex.getStackTrace();
      for(StackTraceElement elem:trace) {
         bld.append("   at ");
         bld.append(elem.toString());
         bld.append('\n');
      }
      
      Throwable ourCause = ex.getCause();
      if (ourCause != null) appendCause(bld, trace, ourCause);      

      return bld.toString();
   }
   
   private static void appendCause(StringBuilder bld, StackTraceElement[] causedTrace, Throwable cause) {
      StackTraceElement[] trace = cause.getStackTrace();
      int cnt1 = trace.length-1, cnt2 = causedTrace.length-1;
      while (cnt1 >= 0 && cnt2 >=0 && trace[cnt1].equals(causedTrace[cnt2])) {cnt1--; cnt2--;}
      int framesInCommon = trace.length - 1 - cnt1;

      bld.append("Caused by ");
      bld.append(cause.toString());
      bld.append('\n');
      for (int i=0; i <= cnt1; i++) {
         bld.append("   at ");
         bld.append(trace[i].toString());
         bld.append('\n');
      }
      if (framesInCommon != 0) {
         bld.append("   ... ");
         bld.append(framesInCommon);
         bld.append(" more\n");}

      Throwable ourCause = cause.getCause();
      if (ourCause != null) appendCause(bld, trace, ourCause);      
   }   
}
