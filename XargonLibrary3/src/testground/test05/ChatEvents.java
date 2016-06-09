package testground.test05;

import it.xargon.events.*;

@EventSink public interface ChatEvents {
   @Event public void joins(String nickname);
   @Event public void leaves(String nickname, String text);
   @Event public void message(String nickname, String text);
   @Event public void action(String nickname, String text);
   @Event public void serverClosing();
   @Event public void system(String text);
}
