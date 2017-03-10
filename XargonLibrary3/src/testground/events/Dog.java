package testground.events;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

import it.xargon.events.Event;
import it.xargon.events.EventsSourceImpl;

public class Dog extends EventsSourceImpl {
   @FunctionalInterface @Event
   public interface Bark {public void with(int times);}
   
   @FunctionalInterface @Event
   public interface Eat extends Runnable {}
   
   @FunctionalInterface @Event
   public interface WagTail extends Runnable {}
   
   @FunctionalInterface @Event
   public interface Bite extends Runnable {}
   
   private String name=null;
   
   public Dog(String name) {
      super();
      this.name=name;
   }
   
   @Override
   protected ExecutorService getThreadPool() {return null;}

   public String getName() {return name;}
   
   public void call() {
      int barks=ThreadLocalRandom.current().nextInt(2,6);
      raise(Bark.class).with(barks);
   }
   
   public void feed() {
      raise(Eat.class);
   }
   
   public void cuddle() {
      raise(WagTail.class);
   }
   
   public void scold() {
      raise(Bite.class);
   }
}
