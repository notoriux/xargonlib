package it.xargon.nioxmp;

import java.util.EnumSet;

public enum XmpMessageType {
   EVENT((byte)1),
   REQUEST((byte)2),
   ANSWER((byte)3),
   CLOSING((byte)4);
   
   private byte typeId;
   private XmpMessageType(byte itypeId) {typeId=itypeId;}
   public byte id() {return typeId;}
   public static XmpMessageType getById(byte itypeId) {
      for(XmpMessageType tp:EnumSet.allOf(XmpMessageType.class)) if (itypeId==tp.id()) return tp;
      return null;
   }
}
