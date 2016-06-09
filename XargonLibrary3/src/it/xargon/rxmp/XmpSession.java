package it.xargon.rxmp;

import java.io.IOException;

public interface XmpSession {
   public static interface AsyncAnswer {public void receive(byte[] answer);}
   
   public boolean isOpen();
   public void close();
   
   public void sendEvent(byte[] outgoing) throws IOException;   
   public byte[] sendRequest(byte[] outgoing) throws IOException;
   public byte[] sendRequest(byte[] outgoing, long timeout) throws IOException;
   public void sendRequest(byte[] outgoing, AsyncAnswer receiver) throws IOException;
   public void sendRequest(byte[] outgoing, AsyncAnswer receiver, long timeout) throws IOException;
}
