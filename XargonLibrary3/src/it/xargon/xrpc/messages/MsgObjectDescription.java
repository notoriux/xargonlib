package it.xargon.xrpc.messages;

import java.io.*;
import java.lang.reflect.Array;
import java.util.EnumSet;

import it.xargon.streams.Printable;
import it.xargon.util.Bitwise;
import it.xargon.util.Identifier;

public class MsgObjectDescription implements Serializable, Printable {
   public static enum Flavor {
      VOID(0),
      SOURCE_REF(1),
      DEST_REF(2),
      SERIALIZED(3);
      
      private byte typeId;
      private Flavor(int itypeId) {typeId=(byte)(itypeId & 0x00FF);}
      public byte id() {return typeId;}
      public static Flavor getById(byte itypeId) {
         for(Flavor tp:EnumSet.allOf(Flavor.class)) if (itypeId==tp.id()) return tp;
         return null;
      }

   }
      
   public Flavor flavor=null;
   public byte[] sercontents=null;    //se flavor=SERIALIZED
   public Identifier objid=null;       //se flavor=SOURCE_REF o DEST_REF
   public String[] interfaces=null;   //se flavor=SOURCE_REF

   public void printout(String indent, PrintWriter out) {
      out.print(indent);out.print("BXMP Object description: ");
      switch (flavor) {
         case VOID:
            out.println("(void)");
            break;
         case SERIALIZED:
            out.println("serialized content: " + sercontents.length + " byte(s)");
            try {
               Object obj=Bitwise.deserializeObject(sercontents);
               Class<?> cl=obj.getClass();
               
               if (cl.isArray()) {
                  Class<?> ccl=cl.getComponentType();
                  out.println(indent + "  Class: array of " + ccl.getName());
                  int cnt=Array.getLength(obj);
                  out.println(indent + "  Contents: " + cnt + " element(s)" );
                  for(int i=0;i<cnt;i++) {
                     Object elem=Array.get(obj, i);
                     out.println(indent + "  [" + i + "] " + (elem==null?"(null)":elem.toString()));
                  }
               } else {
                  out.println(indent + "  Class: " + cl.getName());
                  out.println(indent + "  Contents: " + obj.toString());
               }               
            } catch (Exception ex) {
               out.println(indent + "  (unable to deserialize: " + ex.getMessage() + ")");
            }
            break;
         case DEST_REF:
            out.println("reference in destination (" + objid.toString() + ")");
            break;
         case SOURCE_REF:
            out.print("reference in source (" + objid.toString() + ") - interfaces ");
            if (interfaces==null) {
               out.println("deferred");
            } else {
               out.println("following:");
               for(String iface:interfaces) out.println(indent + "  " + iface);
            }
            break;
      }
      
   }
}
