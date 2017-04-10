package it.xargon.nioxmp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import it.xargon.events.EventsSourceImpl;
import it.xargon.events.OnEvent;
import it.xargon.nioxmp.XmpWire.WorkMode;
import it.xargon.nioxmp.msg.XmpContentAnswer;
import it.xargon.nioxmp.msg.XmpContentEvent;
import it.xargon.nioxmp.msg.XmpContentMessage;
import it.xargon.nioxmp.msg.XmpContentRequest;
import it.xargon.nioxmp.msg.XmpMessage;
import it.xargon.nioxmp.msg.XmpSessionClose;
import it.xargon.nioxmp.msg.XmpSessionInit;
import it.xargon.nioxmp.msg.XmpSessionKO;
import it.xargon.nioxmp.msg.XmpSessionMessage;
import it.xargon.nioxmp.msg.XmpSessionOK;
import it.xargon.util.Identifier;

abstract class XmpAbstractEndpointImpl extends EventsSourceImpl implements XmpEndpoint {
   protected Identifier sessionId=null;
   protected Identifier lastMessageId=Identifier.IDZERO;
   protected Object messageIdLock=new Object();
   protected XmpWire wire=null;
   protected XmpFactory xf=null;

   protected class ResponseCollector {
      private Thread waitingThread=null;
      private XmpContentAnswer answer=null;
      private Exception exc=null;
      public ResponseCollector(Thread waitingThread) {this.waitingThread=waitingThread;}
      public Thread getWaitingThread() {return waitingThread;}
      public ResponseCollector setAnswer(XmpContentAnswer answer) {this.answer=answer;return this;}
      public XmpContentAnswer getAnswer() {return answer;}
      public ResponseCollector setException(Exception exc) {this.exc=exc;return this;}
      public Exception getException() {return exc;}
   }
   
   protected Map<Identifier, ResponseCollector> waitingRequests=new HashMap<>();
   protected Object respLock=new Object();

   public XmpAbstractEndpointImpl(XmpFactory xf) {
      this.xf=xf;
   }
   
   protected void registerWireHandlers() {
      
   }
   
   protected void unregisterWireHandlers() {
      
   }
   
   @OnEvent(XmpWire.MessageReceived.class)
   protected void messageReceived(XmpMessage msg) {
      try {
         if (msg instanceof XmpSessionMessage) {
            if (msg instanceof XmpSessionClose) {
               //TODO: inizia una chiusura pulita
            } else {
               wire.send(new XmpSessionKO(sessionId, "Unexpected session message"));
               raise(XmpEndpoint.Error.class).with(new XmpException("Unexpected session message").setAttachedInfo(msg));
               return;
            }
         } else if (msg instanceof XmpContentMessage) {
            if (msg instanceof XmpContentEvent) {
               //TODO: gestire Future per poter interrompere l'elaborazione se necessario
               Future<?> eventFuture=xf.getThreadPool().submit(new XmpContentEventTask((XmpContentEvent) msg));
               return;
            } else if (msg instanceof XmpContentRequest) {
               //TODO: gestire Future per poter interrompere l'elaborazione se necessario
               Future<?> requestFuture=xf.getThreadPool().submit(new XmpContentRequestTask((XmpContentRequest) msg));
               return;
            } else if (msg instanceof XmpContentAnswer) {
               XmpContentAnswer answer=(XmpContentAnswer)msg;
               ResponseCollector coll=waitingRequests.get(answer.getParentId());
               if (coll==null) {
                  raise(XmpEndpoint.Error.class).with(new XmpException("Got a response for an unknown request").setAttachedInfo(answer));
                  return;
               }
               
               synchronized (coll) {
                  coll.setAnswer(answer);
                  coll.getWaitingThread().notify();
               }
               
               return;
            } else {
               raise(XmpEndpoint.Error.class).with(new XmpException("Unknown content message").setAttachedInfo(msg));
            }
         } else {
            raise(XmpEndpoint.Error.class).with(new XmpException("Unknown XMP message").setAttachedInfo(msg));
         }
      } catch (IOException ex) {
         raise(XmpEndpoint.Error.class).with(new XmpException("IOException while sending a message down the wire", ex));
      }
   }
   
   private abstract class XmpTaskRunner<T extends XmpContentMessage> implements Runnable {
      protected T contentMsg=null;
      
      public XmpTaskRunner(T contentMsg) {this.contentMsg=contentMsg;}
      
      public void run() {
         try {protectedRun();}
         catch (Exception ex) {
            raise(XmpEndpoint.Error.class)
               .with(new XmpException("Exception while processing a parallel task", ex).setAttachedInfo(contentMsg));
         }
      }
      
      protected abstract void protectedRun() throws Exception;
   }
   
   private class XmpContentEventTask extends XmpTaskRunner<XmpContentEvent> {
      public XmpContentEventTask(XmpContentEvent eventMsg) {super(eventMsg);}
      
      protected void protectedRun() throws Exception {
         raise(XmpEndpoint.EventReceived.class).with(contentMsg.getContents());
      }
   }
   
   private class XmpContentRequestTask extends XmpTaskRunner<XmpContentRequest> {
      public XmpContentRequestTask(XmpContentRequest requestMsg) {super(requestMsg);}
      
      protected void protectedRun() throws Exception {
         ByteBuffer answerContents=raise(XmpEndpoint.RequestReceived.class).with(contentMsg.getContents());
         XmpContentAnswer answer=new XmpContentAnswer(sessionId, nextMessageID(), contentMsg.getMessageId(), answerContents);
         wire.send(answer);
      }
   }
   
   @OnEvent(XmpWire.UnknownObjectOnWire.class)
   protected void unknownObject(Object obj) {
      raise(XmpEndpoint.Error.class).with(new XmpException("Unknown object on wire!").setAttachedInfo(obj));
   }
   
   @OnEvent(XmpWire.ConnectionClosed.class)
   protected void connectionClosed(ByteBuffer[] unsentBuffer) {
      //TODO: gestire riapertura
   }
   
   protected Identifier nextMessageID() {
      Identifier messageId=null;
      synchronized (messageIdLock) {
         messageId=lastMessageId.next();
         lastMessageId=messageId;
      }
      return messageId;
   }

   @Override
   public Identifier getSessionId() {return sessionId;}

   @Override
   public ByteBuffer sendRequest(ByteBuffer contents) throws IOException {
      Identifier msgId=nextMessageID();
      ResponseCollector respcoll=new ResponseCollector(Thread.currentThread());
      synchronized (respLock) {waitingRequests.put(msgId, respcoll);}
      
      XmpContentRequest req=new XmpContentRequest(sessionId, msgId, contents);
      synchronized (respcoll) {
         wire.send(req);
         try {
            respcoll.wait();
         } catch (InterruptedException ex) {
            throw new IOException("Interrupted while waiting for a response");
         }
         
         //Sanity check
         ResponseCollector respcheck=null;
         synchronized (respLock) {respcheck=waitingRequests.remove(msgId);}         
         assert respcheck==respcoll: "Retrieved response collector is different from original";
         
         if (respcoll.getException()!=null)
            throw new IOException("Exception while waiting for a response", respcoll.getException());
      }
      
      return respcoll.answer.getContents();
   }

   @Override
   public void sendEvent(ByteBuffer contents) throws IOException {
      XmpContentEvent event=new XmpContentEvent(nextMessageID(), sessionId, contents);
      wire.send(event);
   }

   @Override
   public boolean isActive() {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public void close() {
      // TODO Auto-generated method stub

   }

   @Override
   protected ExecutorService getThreadPool() {
      return xf.getThreadPool();
   }

}
