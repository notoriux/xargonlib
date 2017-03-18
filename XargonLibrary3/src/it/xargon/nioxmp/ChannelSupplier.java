package it.xargon.nioxmp;

import java.io.Closeable;

import it.xargon.events.Event;
import it.xargon.events.EventsSource;

public interface ChannelSupplier extends EventsSource, Closeable {
   @FunctionalInterface @Event
   public interface Closed extends Runnable {}
}
