package it.xargon.rxmp;

import java.io.*;

import it.xargon.streams.ChecksumOutputStream;
import it.xargon.util.*;

class XmpMessageImpl implements XmpMessage {
   private XmpMessageType itype=null;
   private Identifier messageId=null;
   private Identifier parentId=null;
   private byte[] contents=null;
   private long timestamp=0;
   
   public XmpMessageImpl(XmpMessageType type) {this(type, null);}
   public XmpMessageImpl(XmpMessageType type, byte[] rcontents) {
      itype=type;
      setContents(rcontents);
      messageId=new Identifier();
      parentId=new Identifier();
   }
   
   public void setContents(byte[] cont) {contents=(cont==null)?new byte[0]:cont;}
   public byte[] getContents() {return contents;}
   
   public void setMessageId(Identifier msgid) {messageId=msgid;timestamp=System.currentTimeMillis();}
   public Identifier getMessageId() {return messageId;}
   
   public long getTimestamp() {return timestamp;}

   public void setParentId(Identifier prnid) {parentId=prnid;}
   public Identifier getParentId() {return parentId;}

   public void setType(XmpMessageType type) {itype=type;}
   public XmpMessageType getType() { return itype;}
   
   public XmpMessageImpl createAnswer() {
      if (itype!=XmpMessageType.REQUEST) return null;
      XmpMessageImpl answ=new XmpMessageImpl(XmpMessageType.ANSWER);
      answ.setParentId(messageId);
      return answ;
   }

   public boolean isChildOf(XmpMessage request) {
      if (!(request instanceof XmpMessageImpl)) return false;
      XmpMessageImpl irequest=XmpMessageImpl.class.cast(request);
      if ((itype!=XmpMessageType.ANSWER) || (request.getType()!=XmpMessageType.REQUEST)) return false;
      if (!parentId.equals(irequest.messageId)) return false;      
      return true;
   }

   public void marshal(OutputStream ostream) throws IOException {
      @SuppressWarnings("resource")
      ChecksumOutputStream costream=new ChecksumOutputStream(ostream);
      costream.write(itype.id());
      costream.write(messageId.getSize());
      costream.write(messageId.getData());
      if (itype==XmpMessageType.ANSWER) {
         costream.write(parentId.getSize());
         costream.write(parentId.getData());
      }
      costream.write(Bitwise.intToByteArray(contents.length));
      costream.write(contents);
      costream.writeChecksum();
      costream.flush();
   }

   public void printout(String indent, PrintWriter out) {
      out.print(indent);
      switch (itype) {
         case EVENT:
            out.print("EVENT ");
            out.print(messageId.toString());
            break;
         case REQUEST:
            out.print("REQUEST ");
            out.print(messageId.toString());
            break;
         case ANSWER:
            out.print("ANSWER ");
            out.print(messageId.toString());
            out.print(" FOR REQUEST ");
            out.print(parentId.toString());
            break;
         case CLOSING:
            out.print("CLOSING ");
            out.print(messageId.toString());
            break;
         default:
            out.print("(unknown) ");
            out.print(messageId.toString());
            break;
      }
      out.println();
      out.println(indent + "  " + contents.length + " byte(s)");
      Debug.dumpBytesFormatted(indent + "  ", contents, out);
   }
}