package it.xargon.jvcon;

import java.io.*;

/***
 * This interface allows management and use of a Swing Commandline Console. This console can be
 * allocated via the factory methods in {@linkplain ConsoleFactory}
 * @author Francesco Muccilli
 *
 */

public interface VirtualConsole {
   /**
    * Sets the string that will be shown on the title bar
    * @param title
    */
   public void setConsoleTitle(String title);
   /**
    * @return Current title in the title bar
    */
   public String getConsoleTitle();
   
   /**
    * Sets the prompt shown at the bottom of the console window
    * @param prompt
    */
   public void setPrompt(String prompt);
   /**
    * @return Current prompt
    */
   public String getPrompt();
   
   /**
    * Shows a toolbar of macro functions immediately under the title bar, or substitutes the current one.
    * @param macros An array of {@linkplain Macro} that will be rendered as a list of buttons on
    * the toolbar. Button text will be copied from {@linkplain Macro#getName()}, pressing a button will
    * call {@linkplain Macro#run(VirtualConsole) Macro.run} on a dedicated thread. No more than one
    * macro function will be executing at a time.
    */
   public void setMacros(Macro[] macros);
   
   /**
    * Allows disabling/enabling every mean of sending input to this console. Disabling inputs means that
    * sending commands or activating macro functions won't be possible.
    * @param enabled {@code true} if inputs must be enabled, {@code false} otherwise.
    */
   public void setInputsEnabled(boolean enabled);
   /**
    * @return Current status of inputs
    */
   public boolean isInputEnabled();
   /**
    * @return {@code true} if the console is no more visible, {@code false} if it is still visible and active.
    */
   public boolean isClosed();
   
   /**
    * Closes the console and all the derived streams (output streams will be fushed).
    * If a macro was specified as "closeCallBack" when the console was build, it will be run first
    * (the console and all needed resources will be valid and available in the meantime).
    * Upon returning from the macro, all the cleanup procedures will be performed.
    * This method blocks until the console window has been effectively disposed.
    * @param pause if <code>true</code>, after every cleanup action has been executed, the
    * window will remain visible, allowing inspection and/or copying of the output text (inputs will
    * be obviously disabled). In the meantime the calling thread will stay blocked until the window
    * will be closed via native OS command.
    */
   public void close(boolean pause);
   
   /**
    * @return an InputStream representing the pure standard input
    */
   public InputStream getStdin();
   
   /**
    * Emulates an user-provided input. Useful when implementing "shortcut" macros. 
    * @param buffer block of bytes that will appear as directly inputted by the user
    */
   public void pipeToStdin(byte[] buffer) throws IOException;
   
   /**
    * @return a PrintWriter that will show application output in normal font (black on white)
    */
   public PrintStream getStdout();
   
   /**
    * @return a PrintWriter that will show application errors in a different format (white on red)
    */
   public PrintStream getStderr();
   
   /**
    * This is the only method that will allow some customization of console's graphic aspect. It creates
    * a PrintStream that will be rendered with the following attributes:
    * 
    * @param fontface the font name that will be used for rendering
    * @param size font size in points
    * @param foreground RGB color for the foreground (last 24 bit of an integer)
    * @param background RGB color for the background (last 24 bit of an integer)
    * @param bold "bold" attribute of the font
    * @param italic "italic" attribute of the font
    * @return the custom PrintWriter that will render output text with the specified attributer. Closing the writer
    * will release all resources used (font and style).
    */
   public PrintStream getUserout(String fontface, int size, int foreground, int background, boolean bold, boolean italic);
   
   /**
    * Sets the echo behavior
    * @param enabled {@code true} if user-provided input must be copied on the output (with a different font - usually
    * blue on white), {@code false} otherwise.
    */
   public void setEcho(boolean enabled);
   /**
    * @return the current echo setting
    */
   public boolean hasEcho();
   
   /**
    * @return the current contents of the console window (including prompts and user input)
    */
   public char[] getConsoleBuffer();
   
   /**
    * Forces clearing of the current contents, like pressing "Clear output" button.
    */
   public void clearOutputBuffer();
}
