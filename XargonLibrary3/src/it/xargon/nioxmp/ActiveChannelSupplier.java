package it.xargon.nioxmp;

import it.xargon.events.Event;

public interface ActiveChannelSupplier extends ChannelSupplier {
   @FunctionalInterface @Event
   public interface ChannelAvailable {public void with(SelectableByteChannel channel);}
}
