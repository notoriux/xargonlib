package testground.test08;

import java.util.concurrent.ExecutorService;

import it.xargon.events.*;

public class ExampleSource extends EventsSourceImpl {
   @FunctionalInterface @Event
   public interface FirstEvent {public void with(int arg1, int arg2);}
   public final static Class<FirstEvent> FIRSTEVENT=FirstEvent.class;
   
   @FunctionalInterface @Event
   public interface SecondEvent {public void with(String arg1);}
   public final static Class<SecondEvent> SECONDEVENT=SecondEvent.class;
   
   @FunctionalInterface @Event
   public interface ThirdEvent {public void with(long arg1);}
   public final static Class<ThirdEvent> THIRDEVENT=ThirdEvent.class;
   
   public ExampleSource() {
      super();
   }
   
   @Override
   protected ExecutorService getThreadPool() {return null;}
   
   public void testForRaisingEvents() {
      raise(FIRSTEVENT).with(43,34);
      raise(SECONDEVENT).with("test");
   }
}
