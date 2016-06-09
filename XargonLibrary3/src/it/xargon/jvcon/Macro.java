package it.xargon.jvcon;

/**
 * Macros must be derived from this class.
 * You just need to implement the "run" function, where you will be provided with
 * a reference to the calling console.
 * @author Francesco Muccilli
 *
 */

public abstract class Macro {
   private String cbkname=null;
   public Macro(String name) {cbkname=name;}
   public String getName() {return cbkname;}
   public abstract void run(VirtualConsole target);
}
