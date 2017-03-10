package it.xargon.nioxmp;

import java.util.concurrent.ExecutorService;

import it.xargon.events.Event;
import it.xargon.events.EventsSourceImpl;

class XmpConnection extends EventsSourceImpl{
   @FunctionalInterface @Event
   public interface MessageArrived {public void with(XmpMessage message);}
   
   
   public XmpConnection() {
      // TODO Auto-generated constructor stub
   }

   @Override
   protected ExecutorService getThreadPool() {
      // TODO Auto-generated method stub
      return null;
   }

}
