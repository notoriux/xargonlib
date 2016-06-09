package it.xargon.xrpc.messages;

import java.io.*;

import it.xargon.streams.Printable;
import it.xargon.util.Identifier;

public class MsgRemoteProxyFinalized implements Serializable, Printable {
   public Identifier target=null;
   
   public void printout(String indent, PrintWriter out) {
      out.print(indent); out.println("BXMP Remote proxy finalized: " + target.toString());
   }
}
