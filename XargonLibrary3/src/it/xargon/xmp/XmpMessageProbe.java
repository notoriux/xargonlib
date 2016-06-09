package it.xargon.xmp;

import it.xargon.events.*;

public interface XmpMessageProbe extends EventsSourceImpl<XmpMessageProbe.Events> {
   @EventSink public interface Events {
      @Event public void discardedAnswer(XmpConnection conn, XmpMessage discarded);
      @Event public void requestSent(XmpConnection conn, XmpMessage outgoing);
      @Event public void eventSent(XmpConnection conn, XmpMessage outgoing);
      @Event public void answerReceived(XmpConnection conn, XmpMessage incoming);
      @Event public void preProcessMessage(XmpConnection conn, XmpMessage incoming);
      @Event public void postProcessMessage(XmpConnection conn, XmpMessage incoming, XmpMessage outgoing);
   }
}
