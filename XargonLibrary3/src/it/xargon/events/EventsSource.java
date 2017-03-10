package it.xargon.events;

import java.lang.reflect.Array;
import java.util.*;

import it.xargon.util.Identifier;

public interface EventsSource {
   public enum ExceptionReaction {CONTINUE, STOP, UNREGISTER}
   public Identifier[] bindEvents(Object sink);
   public <T> Identifier onEvent(Class<T> evclass, T sink);
   public <T> Identifier unregister(Class<T> evclass, T sink);
   public boolean unregister(Identifier id);
   public boolean unregister(Identifier[] ids);
   public void unregisterAll(Class<?> evclass);
   public void unregisterAll();
   
   public static boolean isProcessingEvent() {
      return InvocationDispatcher.currentSources.get().size()>0;
   }
   
   public static <T> T getCurrentEventSource(Class<T> expectedType) {
      return expectedType.cast(InvocationDispatcher.currentSources.get().peekFirst());
   }
   
   @SuppressWarnings("unchecked")
   public static <T> T[] getPastResults(Class<T> expectedType) {
      if (!isProcessingEvent()) return null;
      LinkedList<?> res=InvocationDispatcher.eventResults.get().peekFirst();
      return res.toArray((T[]) Array.newInstance(expectedType, res.size()));
   }
}
