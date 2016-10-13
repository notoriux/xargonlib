package it.xargon.streams;

import java.io.*;
import java.util.concurrent.*;

import it.xargon.events.*;

public class StreamLink extends EventsSourceImpl {
   @FunctionalInterface @Event
   public interface DataStreamed {public void with(byte[] data, int length);}
   public final static Class<DataStreamed> DATASTREAMED=DataStreamed.class;
   
   @FunctionalInterface @Event
   public interface StreamException {public void with(StreamErrorReaction ex);}
   public final static Class<StreamException> STREAMEXCEPTION=StreamException.class;
   
   @FunctionalInterface @Event
   public interface Started extends Runnable {}
   public final static Class<Started> STARTED=Started.class;
   
   @FunctionalInterface @Event
   public interface Stopped extends Runnable {}
   public final static Class<Stopped> STOPPED=Stopped.class;
      
	private OutputStream ostream=null;
	private SmartInputStream smartIn=null;
      
   public StreamLink(InputStream in, OutputStream out) {this(in,out,null);}

   public StreamLink(InputStream in, OutputStream out, ExecutorService threadPool) {
      super(threadPool);
      if (out==null) throw new IllegalArgumentException("OutputStream expected");
      ostream=out;
      smartIn=new SmartInputStream(in, threadPool);
   }

   public void start() {
      /*smartIn.onEvent(SmartInputStream.STARTED, this::processStarted);
      
      smartIn.onEvent(SmartInputStream.STOPPED, this::processStopped);
      
      smartIn.onEvent(SmartInputStream.DATAARRIVED, this::processDataArrived);*/
      
      smartIn.bindEvents(this);
      
      smartIn.start();
   }
   
   @OnEvent(SmartInputStream.Started.class)
   private void processStarted() {raise(StreamLink.STARTED);}
   
   @OnEvent(SmartInputStream.Stopped.class)
   private void processStopped() {
      try {ostream.flush();} catch (IOException ex) {}
      raise(StreamLink.STOPPED);
   }
   
   @OnEvent(SmartInputStream.DataArrived.class)
   private void processDataArrived(byte[] data, int length) {
      try {
         ostream.write(data,0,length);
         ostream.flush();
      } catch (IOException ex) {
         StreamErrorReaction ex2=new StreamErrorReaction(ex);
         raise(STREAMEXCEPTION).with(ex2);
         if (ex2.stopRequested()) try {smartIn.stop().close();} catch (IOException e) {}
      }
      
      raise(DATASTREAMED).with(data, length);
   }
   
   public void stop() {
      try {
         InputStream is=smartIn.stop();
         if (is!=null) is.close();
      } catch (IOException e) {}
      
      smartIn.unregisterAll();
   }
   
   public boolean isRunning() {return smartIn.isRunning();}
}
