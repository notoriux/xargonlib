package it.xargon.nioxmp;

import java.util.EnumSet;

public enum XmpMessageType {
   NEWSESSION((byte)0),
   RESTORE((byte)1),
   ENDSESSION((byte)2),
   EVENT((byte)3),
   REQUEST((byte)4),
   ANSWER((byte)5);
   
   private byte typeId;
   private XmpMessageType(byte itypeId) {typeId=itypeId;}
   public byte id() {return typeId;}
   public static XmpMessageType getById(byte itypeId) {
      for(XmpMessageType tp:EnumSet.allOf(XmpMessageType.class)) if (itypeId==tp.id()) return tp;
      return null;
   }
}
