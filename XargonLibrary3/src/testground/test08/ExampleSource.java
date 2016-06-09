package testground.test08;

import it.xargon.events.*;

public class ExampleSource extends EventsSourceImpl {
   @FunctionalInterface @Event
   public interface FirstEvent {public void raise(int arg1, int arg2);}
   public static Class<FirstEvent> FIRSTEVENT=FirstEvent.class;
   
   @FunctionalInterface @Event
   public interface SecondEvent {public void raise(String arg1);}
   public static Class<SecondEvent> SECONDEVENT=SecondEvent.class;
   
   @FunctionalInterface @Event
   public interface ThirdEvent {public void raise(long arg1);}
   public static Class<ThirdEvent> THIRDEVENT=ThirdEvent.class;
   
   public ExampleSource() {
      super();
   }
   
   public void testForRaisingEvents() {
      raise(FIRSTEVENT).raise(43,34);
      raise(SECONDEVENT).raise("test");
   }
}
