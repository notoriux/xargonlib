package it.xargon.nioxmp;

import java.util.EnumSet;

public enum XmpContentMessageType {
   EVENT((byte)0),
   REQUEST((byte)1),
   ANSWER((byte)3),
   NOSESSION((byte)4);
   
   private byte typeId;
   private XmpContentMessageType(byte itypeId) {typeId=itypeId;}
   public byte id() {return typeId;}
   public static XmpContentMessageType getById(byte itypeId) {
      for(XmpContentMessageType tp:EnumSet.allOf(XmpContentMessageType.class)) if (itypeId==tp.id()) return tp;
      return null;
   }
}
