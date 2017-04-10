package it.xargon.nioxmp;

import java.io.IOException;

import it.xargon.events.Event;

public interface ActiveChannelSupplier extends ChannelSupplier {
   @FunctionalInterface @Event
   public interface ChannelAvailable {public void with(SelectableByteChannel channel);}
   
   public void start() throws IOException;
}
