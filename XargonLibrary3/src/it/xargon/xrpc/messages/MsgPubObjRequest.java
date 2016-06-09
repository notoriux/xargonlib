package it.xargon.xrpc.messages;

import it.xargon.streams.Printable;

import java.io.*;

public class MsgPubObjRequest implements Serializable, Printable {
   public String pubname=null;
   
   public void printout(String indent, PrintWriter out) {
      out.print(indent); out.println("BXMP Requesting public object: " + pubname);
   }
}
