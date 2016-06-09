package it.xargon.rxmp;


import it.xargon.util.*;

class XmpParser {   
   private enum EXPECT {
		MESSAGETYPE,          //tipo di messaggio
      MSGIDSIZE,            //lunghezza in byte dell'identificativo di messaggio
		MSGID,                //identificativo del messaggio
      PARIDSIZE,            //lunghezza in byte dell'identificativo del messaggio padre (solo se messagetype=ANSWER)
		PARID,                //identificativo del messaggio padre (solo se messagetype=ANSWER)
		CONTENTSLEN,          //attesa della lunghezza del contenuto
		CONTENTS,             //attesa del contenuto
		CHECKSUM,             //attesa del byte di checksum
		DONE;                 //dati ok, ricostruire il messaggio
	}
	
	private EXPECT status=null;
	private SimpleChecksum checksum=null;
	
	private XmpMessageType msgtype=null;
	private byte[] bMessageId=null;
	private Identifier imessageId=null;
	private byte[] bParentId=null;
	private Identifier iparentId=null;
   private byte[] contentsLen=null;
	private byte[] bcontents=null;
	
	private int len=0;
		
	public XmpParser() {
      checksum=new SimpleChecksum();
		reset();
	}
	
	public synchronized void reset() {
		status=EXPECT.MESSAGETYPE;
		msgtype=null;
		bMessageId=null;
		imessageId=null;
		bParentId=null;
		iparentId=null;
		contentsLen=null;
		bcontents=null;
		len=0;
		checksum.reset();
	}
	
   private void emitError(String errstr) throws XmpParserException {
      reset();
      throw new XmpParserException(errstr);
   }
   
   public synchronized XmpMessageImpl feed(byte data) throws XmpParserException {
      if (status!=EXPECT.CHECKSUM) checksum.feed(data);
      
   	switch (status) {
   		case MESSAGETYPE:
   			msgtype=XmpMessageType.getById(data);
   			if (msgtype==null) emitError("Invalid message type");
   			status=EXPECT.MSGIDSIZE;
   			break;
   			
         case MSGIDSIZE:
            len=Bitwise.asInt(data);
            bMessageId=new byte[len];
            if (len==0) {
               imessageId=new Identifier(bMessageId);
               if (msgtype==XmpMessageType.ANSWER) {
                  status=EXPECT.PARIDSIZE;
               } else {
                  bParentId=new byte[0];
                  iparentId=new Identifier(bParentId);
                  len=4;
                  contentsLen=new byte[4];
                  status=EXPECT.CONTENTSLEN;
               }
            } else status=EXPECT.MSGID;
            break;
   		case MSGID:
   		   bMessageId[bMessageId.length - len] = data;
   		   len--;
   		   if (len==0) {
               imessageId=new Identifier(bMessageId);
               if (msgtype==XmpMessageType.ANSWER) {
   		         status=EXPECT.PARIDSIZE;
   		      } else {
   		         bParentId=new byte[0];
                  iparentId=new Identifier(bParentId);
                  len=4;
                  contentsLen=new byte[4];
                  status=EXPECT.CONTENTSLEN;
   		      }
   		   }
   		   break;
   		case PARIDSIZE:
            len=Bitwise.asInt(data);
            bParentId=new byte[len];
            if (len==0) {
               iparentId=new Identifier(bParentId);
               len=4;
               contentsLen=new byte[4];
               status=EXPECT.CONTENTSLEN;
            } else status=EXPECT.PARID;
            break;   		   
   		case PARID:
   		   bParentId[bParentId.length - len] = data;
            len--;
            if (len==0) {
               iparentId=new Identifier(bParentId);
               len=4;
               contentsLen=new byte[4];
               status=EXPECT.CONTENTSLEN;
            }
            break;
 
   		case CONTENTSLEN:
   		   contentsLen[4-len]=data;
   		   len--;
   		   if (len==0) {
   		      len=Bitwise.byteArrayToInt(contentsLen);
   		      if (len==0) {
   		         status=EXPECT.CHECKSUM;
   		      } else {
   		         bcontents=new byte[len];
   		         status=EXPECT.CONTENTS;
   		      }
   		   }
   		   break;
   		case CONTENTS:
   		   bcontents[bcontents.length-len]=data;
   		   len--;
   		   if (len==0) status=EXPECT.CHECKSUM;
   		   break;
   		case CHECKSUM:
   		   if (checksum.get()!=Bitwise.asInt(data)) emitError("Checksum error");
   		   status=EXPECT.DONE;
   		   break;
   		case DONE:
   		   break;
   	}
   	
   	XmpMessageImpl msg=null;
   	
   	if (status==EXPECT.DONE) {
   	   msg=new XmpMessageImpl(msgtype, bcontents);
   	   msg.setMessageId(imessageId);
   	   msg.setParentId(iparentId);
   	   reset();
   	}
   	
   	return msg;
   }
}
