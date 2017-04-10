package it.xargon.nioxmp;

import it.xargon.events.EventsSource;

import it.xargon.events.Event;

public interface XmpServer extends EventsSource {
   @FunctionalInterface @Event
   public interface XmpClientAvailable {public void with(XmpEndpoint xmpClient);}
   
   @FunctionalInterface @Event
   public interface XmpServerStarted extends Runnable {}

   @FunctionalInterface @Event
   public interface XmpServerStopped extends Runnable {}
}
