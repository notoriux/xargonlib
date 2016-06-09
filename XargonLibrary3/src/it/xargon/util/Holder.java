package it.xargon.util;

public class Holder<T> {
   private T heldObj=null;
   
   public T get() {return heldObj;}
   
   public void set(T obj) {heldObj=obj;}
}
