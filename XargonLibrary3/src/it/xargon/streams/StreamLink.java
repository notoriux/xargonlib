package it.xargon.streams;

import java.io.*;
import java.util.concurrent.*;

import it.xargon.events.*;

import static it.xargon.streams.SmartInputStream.*;

public class StreamLink extends EventsSourceImpl {
   @FunctionalInterface @Event
   public interface DataStreamed {public void raise(byte[] data, int length);}
   public static Class<DataStreamed> DATASTREAMED=DataStreamed.class;
   
   @FunctionalInterface @Event
   public interface StreamException {public void raise(StreamErrorReaction ex);}
   public static Class<StreamException> STREAMEXCEPTION=StreamException.class;
   
   @FunctionalInterface @Event
   public interface Started extends Runnable {}
   public static Class<Started> STARTED=Started.class;
   
   @FunctionalInterface @Event
   public interface Stopped extends Runnable {}
   public static Class<Stopped> STOPPED=Stopped.class;
      
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
      smartIn.onEvent(SmartInputStream.STARTED,() -> raise(StreamLink.STARTED).run());
      
      smartIn.onEvent(SmartInputStream.STOPPED, () -> {
         try { ostream.flush();} catch (IOException ex) {}
         raise(StreamLink.STOPPED).run();
      });
      
      smartIn.onEvent(DATAARRIVED, (data, length) -> {
         try {
            ostream.write(data,0,length);
            ostream.flush();
         } catch (IOException ex) {
            StreamErrorReaction ex2=new StreamErrorReaction(ex);
            raise(STREAMEXCEPTION).raise(ex2);
            if (ex2.stopRequested()) try {smartIn.stop().close();} catch (IOException e) {}
         }
         
         raise(DATASTREAMED).raise(data, length);
      });
      
      smartIn.start();
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
