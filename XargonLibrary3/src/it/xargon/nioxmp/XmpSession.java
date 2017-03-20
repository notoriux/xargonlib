package it.xargon.nioxmp;

import java.nio.ByteBuffer;

import it.xargon.events.Event;
import it.xargon.events.EventsSource;

public interface XmpSession extends EventsSource {
   @FunctionalInterface @Event
   public interface RequestReceived {public ByteBuffer with(ByteBuffer request);}
   
   @FunctionalInterface @Event
   public interface EventReceived {public void with(ByteBuffer event);}
   
   @FunctionalInterface @Event
   public interface Connected extends Runnable {}
   
   @FunctionalInterface @Event
   public interface Reconnected {public void with(int tries);}
   
   @FunctionalInterface @Event
   public interface Closed extends Runnable {}
   
   @FunctionalInterface @Event
   public interface Truncated extends Runnable {}
   
   @FunctionalInterface @Event
   public interface Retrying {public boolean with(int alreadyTried);}
   
   @FunctionalInterface @Event
   public interface Error {public boolean with(Exception ex);}

   public boolean isInitiator();
   
   public ByteBuffer sendRequest(ByteBuffer request);
   
   public void sendEvent(ByteBuffer event);
   
   public boolean isActive();
   
   public void close();
}
