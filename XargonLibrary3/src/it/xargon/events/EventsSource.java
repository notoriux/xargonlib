package it.xargon.events;

import it.xargon.util.Identifier;

public interface EventsSource {
   public enum ExceptionReaction {CONTINUE, STOP, UNREGISTER}
   public <T> Identifier onEvent(Class<T> evclass, T sink);
   public <T> Identifier unregister(Class<T> evclass, T sink);
   public boolean unregister(Identifier id);
   public boolean unregister(Identifier[] ids);
   public void unregisterAll(Class<?> evclass);
   public void unregisterAll();
}
