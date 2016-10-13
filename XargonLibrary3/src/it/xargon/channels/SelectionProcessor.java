package it.xargon.channels;

import java.nio.channels.SelectionKey;

public interface SelectionProcessor {
   public void processKey(SelectorWorker worker, SelectionKey key);
}

