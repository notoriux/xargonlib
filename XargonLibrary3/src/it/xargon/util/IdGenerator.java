package it.xargon.util;

public class IdGenerator {
   private MutableIdentifier nextVal=null;
   
   public IdGenerator() {this(null);}
   
   public IdGenerator(Identifier nextId) {
      nextVal=(nextId==null)?new MutableIdentifier():new MutableIdentifier(nextId.getData());
   }
   
   public Identifier peek() {
      synchronized (nextVal) {
         return new Identifier(nextVal.getData());
      }
   }
   
   public Identifier next() {
      synchronized (nextVal) {
         Identifier result=new Identifier(nextVal.getData());
         nextVal.increment();
         return result;
      }      
   }
}
