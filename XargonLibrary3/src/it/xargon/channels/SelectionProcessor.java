package it.xargon.channels;

import java.nio.channels.SelectionKey;

@FunctionalInterface
public interface SelectionProcessor {
   public Runnable processKey(SelectorWorker worker, SelectionKey key) throws Exception;
}

