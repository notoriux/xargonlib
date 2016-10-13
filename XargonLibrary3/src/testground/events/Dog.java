package testground.events;

import java.util.concurrent.ThreadLocalRandom;

import it.xargon.events.Event;
import it.xargon.events.EventsSourceImpl;

public class Dog extends EventsSourceImpl {
   @FunctionalInterface @Event
   public interface Bark {public void with(Dog dog, int times);}
   
   @FunctionalInterface @Event
   public interface Eat {public void with(Dog dog);}
   
   @FunctionalInterface @Event
   public interface WagTail {public void with(Dog dog);}
   
   @FunctionalInterface @Event
   public interface Bite {public void with(Dog dog);}
   
   private String name=null;
   
   public Dog(String name) {
      super();
      this.name=name;
   }

   public String getName() {return name;}
   
   public void call() {
      int barks=ThreadLocalRandom.current().nextInt(2,6);
      raise(Bark.class).with(this, barks);
   }
   
   public void feed() {
      raise(Eat.class).with(this);
   }
   
   public void cuddle() {
      raise(WagTail.class).with(this);
   }
   
   public void scold() {
      raise(Bite.class).with(this);
   }
}
