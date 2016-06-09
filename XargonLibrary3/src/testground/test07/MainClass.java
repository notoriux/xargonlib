package testground.test07;

import it.xargon.util.*;
import java.io.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import it.xargon.jvcon.*;
import java.util.*;

public class MainClass {
   public static BufferedReader stdin=Debug.stdin;
   public static PrintWriter stdout=Debug.stdout;
   public static PrintWriter stderr=Debug.stderr;
      
   public static void main(String[] args) {
      try {
         new MainClass().go(args);
         stdout.flush();
      } catch (Throwable tr) {
         tr.printStackTrace();
      }
   }
   
   public void go(String[] args) throws Throwable {
      ConsoleFactory.setSystemLookAndFeel();
      VirtualConsole vcon=ConsoleFactory.createConsole("Virtual console test - 07", "test>");
      
      BufferedReader vin=new BufferedReader(new InputStreamReader(vcon.getStdin()));
      
      vcon.getStdout().println("This is a virtual console.");
      vcon.getStdout().println("Type \"quit\" to exit");
      vcon.getStdout().println();
      
      UIDefaults uidefs = UIManager.getLookAndFeelDefaults();
      TreeMap<String,Object> uimap=new TreeMap<String, Object>();
      for(Map.Entry<Object,Object> entry:uidefs.entrySet()) {
         String key=entry.getKey().toString();
         if (key.contains("font")) uimap.put(key, entry.getValue());
      }
      
      for(Map.Entry<String,Object> entry:uimap.entrySet()) {
         vcon.getStdout().println(entry.getKey() + " (" + entry.getValue().getClass().getName() + ")");
         vcon.getStdout().println("\t" + entry.getValue().toString());
      }
      
      Macro macro1=new Macro("List DataFlavor[]") {
         public void run(VirtualConsole target) {
            Clipboard clipboard=Toolkit.getDefaultToolkit().getSystemClipboard();
            DataFlavor[] dfs=clipboard.getAvailableDataFlavors();
            for(DataFlavor df:dfs) {
               target.getStdout().println(df.toString());
            }
         }
      };
      vcon.setMacros(new Macro[] {macro1});

      String text=null;
      
      do {
         text=vin.readLine();
         if (text!=null) {
            if (text.equals("quit")) vcon.getStdout().println("Bye!");
            else vcon.getStdout().println(text);
         }
      } while (!"quit".equals(text));
      
      vcon.close(true);
   }   
}
