package it.xargon.events;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Event {
   public enum Kind {SIMPLE, SERIAL, PARALLEL, SWING, CHAIN_FIRST, CHAIN_LAST}
   Kind value() default Kind.SIMPLE;
}
