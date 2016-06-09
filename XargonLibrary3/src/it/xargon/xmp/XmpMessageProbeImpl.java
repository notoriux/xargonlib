package it.xargon.xmp;

import java.util.concurrent.*;

import it.xargon.events.*;

class XmpMessageProbeImpl extends EventsSourceImpl<XmpMessageProbe.Events> implements XmpMessageProbe {
   public XmpMessageProbeImpl(ExecutorService threadPool) {
      super(threadPool);
   }
   
   public void p_discardedAnswer(XmpConnection conn, XmpMessage discarded) {
      raiseEvent.discardedAnswer(conn, discarded);
   }
   
   public void p_requestSent(XmpConnection conn, XmpMessage outgoing) {
      raiseEvent.requestSent(conn, outgoing);
   }
   
   public void p_eventSent(XmpConnection conn, XmpMessage outgoing) {
      raiseEvent.eventSent(conn, outgoing);
   }
   
   public void p_answerReceived(XmpConnection conn, XmpMessage incoming) {
      raiseEvent.answerReceived(conn, incoming);
   }
   
   public void p_preProcessMessage(XmpConnection conn, XmpMessage incoming) {
      raiseEvent.preProcessMessage(conn, incoming);
   }
   
   public void p_postProcessMessage(XmpConnection conn, XmpMessage incoming, XmpMessage outgoing) {
      raiseEvent.postProcessMessage(conn, incoming, outgoing);
   }
}
