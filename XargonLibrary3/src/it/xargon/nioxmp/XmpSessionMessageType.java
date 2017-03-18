package it.xargon.nioxmp;

import java.util.EnumSet;

public enum XmpSessionMessageType {
   INIT((byte)0),
   INIT_ACK((byte)1),
   RESUME((byte)2),
   RESUME_OK((byte)3),
   RESUME_KO((byte)4),
   CLOSE((byte)5),
   CLOSE_ACK((byte)6);
   
   private byte typeId;
   private XmpSessionMessageType(byte itypeId) {typeId=itypeId;}
   public byte id() {return typeId;}
   public static XmpSessionMessageType getById(byte itypeId) {
      for(XmpSessionMessageType tp:EnumSet.allOf(XmpSessionMessageType.class)) if (itypeId==tp.id()) return tp;
      return null;
   }
}
