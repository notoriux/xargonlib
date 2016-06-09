package it.xargon.xrpc.messages;

import java.io.*;
import java.util.*;

import it.xargon.streams.Printable;

public class MsgPubObjsListAnswer implements Serializable, Printable {
   public HashMap<String, String[]> pubObjList=null;

   public void printout(String indent, PrintWriter out) {
      out.print(indent); out.println("BXMP Answering public objects list");
      for (Map.Entry<String, String[]> entry: pubObjList.entrySet()) {
         out.print(indent + "  "); 
         out.print(entry.getKey());
         out.print(":");
         for(String iface:entry.getValue()) {
            out.print(" ");
            out.print(iface);
         }
         out.println();
      }
      
   }
}
