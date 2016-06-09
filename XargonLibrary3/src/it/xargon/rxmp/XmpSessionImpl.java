package it.xargon.rxmp;

import java.io.IOException;

import it.xargon.streams.DuplexChannel;

class XmpSessionImpl implements XmpSession {
   private DuplexChannel channel;

   @Override
   public boolean isOpen() {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public void close() {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void sendEvent(byte[] outgoing) throws IOException {
      // TODO Auto-generated method stub
      
   }

   @Override
   public byte[] sendRequest(byte[] outgoing) throws IOException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public byte[] sendRequest(byte[] outgoing, long timeout) throws IOException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void sendRequest(byte[] outgoing, AsyncAnswer receiver)
         throws IOException {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void sendRequest(byte[] outgoing, AsyncAnswer receiver, long timeout)
         throws IOException {
      // TODO Auto-generated method stub
      
   }
   
}
