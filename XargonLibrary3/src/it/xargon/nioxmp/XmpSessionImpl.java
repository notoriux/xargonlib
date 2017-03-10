package it.xargon.nioxmp;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import it.xargon.events.EventsSourceImpl;
import it.xargon.util.Identifier;

class XmpSessionImpl extends EventsSourceImpl implements XmpSession {
   private XmpFactory xmpFactory=null;
   private Supplier<ChannelPair> channelInitiator=null;
   private ChannelPair channels=null;
   private Identifier sessionId=null;
   
   public XmpSessionImpl(XmpFactory xmpFactory, Supplier<ChannelPair> channelInitiator) {
      // TODO Auto-generated constructor stub
   }
   
   public void start() {
      
   }

   @Override
   public ByteBuffer sendRequest(ByteBuffer request) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void sendEvent(ByteBuffer event) {
      // TODO Auto-generated method stub

   }

   @Override
   public boolean isActive() {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public void close() {
      // TODO Auto-generated method stub

   }

   @Override
   protected ExecutorService getThreadPool() {
      // TODO Auto-generated method stub
      return null;
   }

}
