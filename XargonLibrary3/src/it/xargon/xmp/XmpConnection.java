package it.xargon.xmp;

import it.xargon.events.*;
import java.io.*;

import it.xargon.streams.*;

public interface XmpConnection extends EventsSourceImpl<XmpConnection.Events> {
   @EventSink public interface Events {
      public abstract class Adapter implements XmpConnection.Events {
         public void connected(XmpConnection conn) {}
         public void disconnected(XmpConnection conn) {}
         public void exception(XmpConnection conn, XmpException ex) {}
         public void processEvent(XmpConnection conn, byte[] contents) {}
         public byte[] processRequest(XmpConnection conn, byte[] request) {return request;}
      }
      
      @Event public void connected(XmpConnection conn);
      @Event public void processEvent(XmpConnection conn, byte[] contents);
      @Event(EventType.CHAINED) public byte[] processRequest(XmpConnection conn, byte[] request);
      @Event public void disconnected(XmpConnection conn);
      @Event public void exception(XmpConnection conn, XmpException ex);
   }

   public final static byte VER_MAJOR = 0x07;
   public final static byte VER_MINOR = 0x00;
   public final static byte[] SIGNATURE={'X', 'M', 'P', VER_MAJOR, VER_MINOR};
   public static interface AsyncAnswer {public void receive(byte[] answer);}
   public static enum SecurityModel {PLAIN_ONLY, CRYPTED_ONLY, BOTH} 

   public XmpFactory getFactory();
   
   public boolean isRequestingEncryption();
   public void setRequestingEncryption(boolean req);
   
   public SecurityModel getSecurityModel();
   public void setSecurityModel(SecurityModel sec);
   
   public EncryptionModel getEncryptionModel();
   public void setEncryptionModel(EncryptionModel enc);
   
   public Identity getIdentity();
   public void setIdentity(Identity ident);
   
   public void registerProbe(XmpMessageProbe.Events sink);
   public void unregisterProbe(XmpMessageProbe.Events sink);
   
   public void start() throws IOException;
   public void close() throws IOException;

   public void sendEvent(byte[] outgoing) throws IOException;
   
   public byte[] sendRequest(byte[] outgoing) throws IOException;
   public byte[] sendRequest(byte[] outgoing, long timeout) throws IOException;
   public void sendRequest(byte[] outgoing, AsyncAnswer receiver) throws IOException;
   public void sendRequest(byte[] outgoing, AsyncAnswer receiver, long timeout) throws IOException;
   
   public boolean isRunning();
   public boolean isConnected();
}
