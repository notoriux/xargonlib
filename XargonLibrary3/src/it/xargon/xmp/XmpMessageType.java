package it.xargon.xmp;

import java.util.EnumSet;

public enum XmpMessageType {
   EVENT(1),
   REQUEST(2),
   ANSWER(3),
   CLOSING(4);
   
   private int typeId;
   private XmpMessageType(int itypeId) {typeId=itypeId;}
   public int id() {return typeId;}
   public static XmpMessageType getById(int itypeId) {
      for(XmpMessageType tp:EnumSet.allOf(XmpMessageType.class)) if (itypeId==tp.id()) return tp;
      return null;
   }
}
