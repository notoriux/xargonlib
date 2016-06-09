package it.xargon.xrpc.messages;

import it.xargon.streams.Printable;

import java.io.PrintWriter;

public class MsgPubObjsListRequest implements java.io.Serializable, Printable {
   public void printout(String indent, PrintWriter out) {
      out.print(indent); out.println("BXMP Requesting public object list");
   }
}
