package it.xargon.xrpc.messages;

import it.xargon.streams.Printable;

import java.io.*;

public class MsgPubObjStatusChanged implements Serializable, Printable {
   public String objname=null;
   public boolean published=false;
   
   public void printout(String indent, PrintWriter out) {
      out.print(indent);
      out.print("BXMP Public object status: " + objname + " ");
      if (published) out.println("is now published");
      else out.println("isn't published anymore");
   }
}
