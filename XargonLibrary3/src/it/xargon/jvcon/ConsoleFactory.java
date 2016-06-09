package it.xargon.jvcon;

import java.util.*;
import java.io.*;
import javax.swing.*;

import java.awt.Color;
import java.lang.reflect.*;
import java.nio.charset.Charset;

import it.xargon.streams.*;
import it.xargon.util.Holder;

/**
 * This class provides factory methods for Swing consoles. All methods return an instance of VirtualConsole,
 * @author Francesco Muccilli
 *
 */

public final class ConsoleFactory {
   private ConsoleFactory() {}
   
   /*
    * This entry point can be used as a facility to launch existing console-based applications
    * into a virtual console.
    * 
    * Mandatory parameters are:
    * -m | --mainClass the.class.name
    * This is the FQN of the class containing the "main()" method
    * -r | --runProcess "executable path"
    * This is the absolute or relative path (to current directory) of the native executable to load. If running
    * in Microsoft Windows, the executable will be assumed to be a console-class program, and the virtual
    * console will emulate the same behavior (fixed-size font, white-on-black color set, and IBM850 as encoding)
    * 
    * -m and -r are mutually exclusive
    * 
    * Optional parameters are:
    * -t | --consoleTitle "Title of the console"
    * If not specified, it will default to the FQN of the main class or the executable absolute path
    * -c | --consolePrompt "prompt>"
    * If not specified, it will default to ">"
    * -l | --lookAndFeel (swing|system|laf.class.name)
    * If not specified, no L&F will be initialized, leaving the option to the application.
    * -p | --param singleWordParameter
    * -p | --param "long string parameter"
    * This syntax allows passing reserved parameter (shown above) as application parameters. Long strings
    * will be passed without quotes (to pass a quote, use \")
    * 
    * Every other unrecognized parameter will be passed to main() method of mainClass, in the same
    * original order, quoted strings will be passed as unquoted whole parameter.
    */
   public static void main(String[] args) {  
      LinkedList<String> params=new LinkedList<String>();
      for(String arg:args) params.addLast(arg);
      LinkedList<String> appParams=new LinkedList<String>();
      
      boolean launchExecutable=false;
      String entryPoint=null;
      String consoleTitle=null;
      String consolePrompt=">";
      String lafClass=null;
      
      String param=null;
      while ((param=getNextParameter(params))!=null) {
         if (param.equals("-m") || param.equals("--mainClass")) {
            launchExecutable=false;
            entryPoint=getNextParameter(params);
         } else if (param.equals("-r") || param.equals("--runProcess")) {
            launchExecutable=true;
            entryPoint=getNextParameter(params);
         } else if (param.equals("-t") || param.equals("--consoleTitle")) {
            consoleTitle=getNextParameter(params);
            if (consoleTitle==null || consoleTitle.isEmpty()) {
               System.out.println("Missing console title");
               return;
            }
         } else if (param.equals("-c") || param.equals("--consolePrompt")) {
            consolePrompt=getNextParameter(params);
            if (consolePrompt==null || consolePrompt.isEmpty()) {
               System.out.println("Missing console prompt");
               return;
            }
         } else if (param.equals("-l") || param.equals("--lookAndFeel")) {
            lafClass=getNextParameter(params);
            if (lafClass==null || lafClass.isEmpty()) {
               System.out.println("Missing lookAndFeel class name");
               return;
            }
         } else if (param.equals("-p") || param.equals("--param")) {
            String ap=getNextParameter(params);
            if (ap==null || ap.isEmpty()) {
               System.out.println("No parameter found after \"" + param + "\" option");
               return;
            }
            appParams.add(ap);
         } else {
            appParams.add(param);
         }
         
         if (entryPoint==null || entryPoint.isEmpty()) {
            System.out.println("Entry point is mandatory!");
            return;
         }
         
         if (lafClass!=null) {
            if (lafClass.equals("system")) setSystemLookAndFeel();
            else if (lafClass.equals("swing")) setSwingLookAndFeel();
            else setLookAndFeel(lafClass);
         }
      }
         
      VirtualConsole vcon=null;
      
      if (launchExecutable) {
         File exePath=new File(entryPoint);
         if (exePath.getParent()==null && !exePath.getAbsoluteFile().exists()) {
            //Se l'eseguibile specificato non possiede un percorso e non è risolvibile
            //nella directory corrente, effettua la ricerca dell'eseguibile nello stesso
            //modo del sistema operativo: provando una ad una le directory specificate
            //nella variabile d'ambiente "PATH"
            String[] libPath=System.getProperty("java.library.path").split(System.getProperty("path.separator"));
            File foundExePath=null;
            for(String path:libPath) {
               File tryExePath=new File(path, exePath.getName());
               if (tryExePath.exists()) {foundExePath=tryExePath;break;}
            }

            if (foundExePath==null) {
               System.out.println(exePath.toString() + " not found in the current directory or in library path");
               return;                  
            }
            exePath=foundExePath;
         } else {
            //Se si arriva a questo punto, significa che è stato specificato un percorso
            //esplicito (relativo o assoluto), oppure che il file esiste nella directory corrente
            exePath=exePath.getAbsoluteFile(); //risolve il percorso in modo assoluto
            if (!exePath.exists()) {
               //se il file effettivamente non esiste, esce con errore
               System.out.println(exePath.toString() + " doesn't exist");
               return;               
            }
         }
         
         appParams.addFirst(exePath.getAbsolutePath());
         
         if (consoleTitle==null) consoleTitle=exePath.getAbsolutePath();
         
         final Holder<Process> procHolder=new Holder<Process>();
         
         Macro killProcess=new Macro("Kill process") {
            public void run(VirtualConsole target) {
               if (procHolder.get()!=null) procHolder.get().destroy();
            }
         };

         PrintStream vStdout=null;
         PrintStream vStderr=null;
         
         if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            vcon=createConsole("850", Color.BLACK.getRGB(), consoleTitle, consolePrompt, killProcess);
            vStdout=vcon.getUserout("Lucida Console", 12, Color.LIGHT_GRAY.getRGB(), Color.BLACK.getRGB(), false, false);
            vStderr=vcon.getUserout("Lucida Console", 12, Color.WHITE.getRGB(), Color.RED.getRGB(), false, false);            
         } else {
            vcon=createConsole(consoleTitle, consolePrompt, killProcess);
            vStdout=vcon.getStdout();
            vStderr=vcon.getStderr();
         }

         vcon.setEcho(false);
         
         ProcessBuilder pb=new ProcessBuilder(appParams);
         try {                        
            Process proc=pb.start();
            procHolder.set(proc);
            StreamLink linkStdout=new StreamLink(proc.getInputStream(), vStdout); linkStdout.start();
            StreamLink linkStderr=new StreamLink(proc.getErrorStream(), vStderr); linkStderr.start();
            StreamLink linkStdin=new StreamLink(vcon.getStdin(), proc.getOutputStream()); linkStdin.start();
                        
            vcon.setMacros(new Macro[] {killProcess});
            
            int exitCode=proc.waitFor();
            
            vStdout.println("Process terminated with exit code " + exitCode);
            
            linkStdin.stop();
            linkStderr.stop();
            linkStdout.stop();
         } catch (Throwable tr) {
            tr.printStackTrace(System.out);
         }
      } else {
         if (consoleTitle==null) consoleTitle=entryPoint;

         vcon=createConsole(consoleTitle, consolePrompt);

         InputStream oldStdin=System.in;
         PrintStream oldStdout=System.out;
         PrintStream oldStderr=System.err;
         
         System.setIn(vcon.getStdin());
         System.setOut(vcon.getStdout());
         System.setErr(vcon.getStderr());
         
         try {
            Class<?> mainClass=Class.forName(entryPoint);
            Class<?>[] mainSignature=new Class<?>[] {args.getClass()};
            Method mainMethod=mainClass.getMethod("main", mainSignature);
            String[] appArgs=appParams.toArray(new String[appParams.size()]);
            mainMethod.invoke(null, new Object[] {appArgs});
         } catch (Throwable tr) {
            tr.printStackTrace(oldStderr);
         } finally {
            System.setIn(oldStdin);
            System.setOut(oldStdout);
            System.setErr(oldStderr);            
         }
      }
      
      vcon.close(true);
   }
   
   private static String getNextParameter(LinkedList<String> params) {
      if (params.size()==0) return null;
      String param=params.pop();
      if (!param.startsWith("\"")) return param;
      while (!param.endsWith("\"") && params.size()>0) param=param + " " + params.pop();
      return param;
   }
   
   /*
    * Shortcut method to set Swing as look-and-feel for current application
    */
   public static void setSwingLookAndFeel() {
      try {
         UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
         commonGuiSetup();
      } catch (Exception ex) {ex.printStackTrace();}
   }
   
   /*
    * Shortcut method to set a system-like look-and-feel for current application
    */
   public static void setSystemLookAndFeel() {
      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         commonGuiSetup();
      } catch (Exception ex) {ex.printStackTrace();}
   }
   
   /*
    * Shortcut method to set a custom look-and-feel
    */
   public static void setLookAndFeel(String lafClassName) {
      try {
         UIManager.setLookAndFeel(lafClassName);
         commonGuiSetup();
      } catch (Exception ex) {ex.printStackTrace();}
   }
   
   
   private static void commonGuiSetup() {
      JFrame.setDefaultLookAndFeelDecorated(true);
      JDialog.setDefaultLookAndFeelDecorated(true);
   }
   /*
    * Creates a virtual console for debugging, copying the calling class fqn into window title.
    * Interactive prompt will be set to ">"
    */
   public static VirtualConsole createDebugConsole() {
      String title=Thread.currentThread().getStackTrace()[2].getClassName();
      VirtualConsole result=createConsole(title, ">", null);
      result.setEcho(true);
      return result;
   }

   /*
    * Creates a virtual console for debugging, copying the calling class fqn into window title.
    * Interactive prompt will be set to the specified prompt
    */   
   public static VirtualConsole createDebugConsole(String prompt) {
      String title=Thread.currentThread().getStackTrace()[2].getClassName();
      VirtualConsole result=createConsole(title, prompt, null);
      result.setEcho(true);
      return result;
   }

   /*
    * Creates a virtual console for debugging, copying the calling class fqn into window title.
    * Interactive prompt will be set to ">", the macro "closeCallBack" will be invoked upon window
    * closing.
    */ 
   public static VirtualConsole createDebugConsole(Macro closeCallBack) {
      String title=Thread.currentThread().getStackTrace()[2].getClassName();
      VirtualConsole result=createConsole(title, ">", closeCallBack);
      result.setEcho(true);
      return result;
   }

   /*
    * Creates a virtual console for debugging, copying the calling class fqn into window title.
    * Interactive prompt will be set to the specified prompt, the macro "closeCallBack" will be
    * invoked upon window closing.
    */
   public static VirtualConsole createDebugConsole(String prompt, Macro closeCallBack) {
      String title=Thread.currentThread().getStackTrace()[2].getClassName();
      return createConsole(title, prompt, closeCallBack);
   }
   
   /*
    * Creates a generic virtual console. Window title will be "SwingConsole", it won't have any
    * prompt and user echo will be disabled, thougth user input via getStdIn will always be
    * available - unless disabled via setInputsEnabled(false)
    */
   public static VirtualConsole createConsole() {
      return createConsole("SwingConsole", "", null);
   }
   
   /*
    * Creates a generic virtual console. Window title can be specified, it won't have any
    * prompt and user echo will be disabled, thougth user input via getStdIn will always be
    * available - unless disabled via setInputsEnabled(false)
    */
   public static VirtualConsole createConsole(String title) {
      return createConsole(title, "", null);
   }
   
   /*
    * Creates a generic virtual console. Window title and prompt can be specified,
    * user echo will be enabled.
    */
   public static VirtualConsole createConsole(String title, String prompt) {
      VirtualConsole result=createConsole(title, prompt, null);
      result.setEcho(true);
      return result;
   }
   
   /*
    * Creates a generic virtual console. Window title, prompt and closing handler can be specified.
    * user echo will be initially disabled.
    */
   public static VirtualConsole createConsole(String title, String prompt, Macro closeCallBack) {
      return createConsole(Charset.defaultCharset().name(), Color.WHITE.getRGB(), title, prompt, closeCallBack);
   }
   
   /*
    * Creates a generic virtual console where the encoding/decoding charset and background color can also be specified
    */
   public static VirtualConsole createConsole(String charset, int background, String title, String prompt, Macro closeCallBack) {
      final VirtualConsoleImpl dcon=new VirtualConsoleImpl(charset, background, title, prompt, closeCallBack);
      
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            dcon.setSize(640, 480);
            dcon.setLocationByPlatform(true);
            dcon.setVisible(true);
         }
      });
      
      return dcon;
   }
}
