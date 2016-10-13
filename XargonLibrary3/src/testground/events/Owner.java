package testground.events;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import it.xargon.events.OnEvent;

public class Owner {
   private ArrayList<Dog> myDogs=null;
   
   public Owner() {
      myDogs=new ArrayList<>();
   }
   
   public void howMany() {
      System.out.println("I own " + myDogs.size() + " dog(s)");
   }
   
   public void adopt(String name) {
      Dog dog=new Dog(name);
      myDogs.add(dog);
      System.out.println("Hi " + name + "! Welcome to your new home!");
      dog.bindEvents(this);
   }
   
   public void doSomething() {
      ThreadLocalRandom rnd=ThreadLocalRandom.current();
      Dog dog=myDogs.get(rnd.nextInt(myDogs.size()));
      int action=rnd.nextInt(4);

      switch (action) {
         case 0:
            System.out.println("Calling " + dog.getName() + "...");
            dog.call();
            break;
         case 1:
            System.out.println(dog.getName() + " deserves some cuddles");
            dog.cuddle();
            break;
         case 2:
            System.out.println("It's time to feed " + dog.getName());
            dog.feed();
            break;
         case 3:
            System.out.println("Ok, " + dog.getName() + " now needs some discipline");
            dog.scold();
            break;
      }
   }
   
   @OnEvent(Dog.Bark.class)
   private void dogBarked(Dog dog, int times) {
      System.out.println(dog.getName() + " barked " + times + " time(s)");
   }
   
   @OnEvent(Dog.WagTail.class)
   private void dogWagging(Dog dog) {
      System.out.println("Awww... " + dog.getName() + " is wagging his tail!");
   }
   
   @OnEvent(Dog.Eat.class)
   private void dogEating(Dog dog) {
      System.out.println("Good boy " + dog.getName() + ", you were hungry, uh?");
   }
   
   @OnEvent(Dog.Bite.class)
   private void dogBiting(Dog dog) {
      System.out.println("Ouch! What the hell " + dog.getName() + "?!?");
   }
}
