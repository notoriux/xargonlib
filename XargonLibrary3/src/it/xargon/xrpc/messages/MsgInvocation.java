package it.xargon.xrpc.messages;

import java.io.*;

import it.xargon.streams.Printable;
import it.xargon.util.Identifier;

public class MsgInvocation implements Serializable, Printable {   
   public Identifier targetObject=null;
   public String targetClass=null;
   public String methodName=null;
   public String[] signature=null;
   public MsgObjectDescription[] arguments=null; 

   public void printout(String indent, PrintWriter out) {
      out.print(indent);out.println("BXMP Method invocation");
      out.print(indent);out.println("  target: " + targetObject.toString());
      out.print(indent);out.println("  target class: " + targetClass);
      out.print(indent);out.println("  method: " + methodName);
      if ((signature==null) || (signature.length==0)) {
         out.print(indent);out.println("  (no arguments)");
      } else {
         out.print(indent);out.print("  signature:");
         for(String sc:signature) out.print(" " + sc);
         out.println();

         out.print(indent);out.println("  " + arguments.length + " arguments:");
         for(MsgObjectDescription arg:arguments) arg.printout(indent + "    ", out);
      }
   }
}
