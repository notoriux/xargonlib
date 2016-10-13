package testground.events;

public class TestEventsMainClass {
   public static void main(String[] args) throws Exception {
      Owner owner=new Owner();
      owner.adopt("Charlie");
      owner.adopt("Jack");
      owner.adopt("Zeus");
      
      owner.doSomething();
   }
}
