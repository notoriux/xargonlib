package testground.test09;

import it.xargon.util.*;

import java.io.*;

import testground.test08.ExampleSource;
import static testground.test08.ExampleSource.*;

public class MainClass {
   public static BufferedReader stdin=Debug.stdin;
   public static PrintWriter stdout=Debug.stdout;
   public static PrintWriter stderr=Debug.stderr;
   
   public static void main(String[] args) {
      try {
         new MainClass().go(args);
      } catch (Throwable tr) {
         tr.printStackTrace();
      }
   }
   
   public void go(String[] args) throws Exception {
      ExampleSource src=new ExampleSource();
      
      src.onEvent(FIRSTEVENT, (a,b) -> {stdout.println("[FIRST SINK] --- a="+a+" b="+b);});
      src.onEvent(FIRSTEVENT, (c,d) -> {stdout.println("[SECOND SINK] --- c="+c+" d="+c);});
      src.onEvent(SECONDEVENT, text -> {stdout.println("[THIRD SINK] --- text="+text);});
      
      src.testForRaisingEvents();
   }   
}
