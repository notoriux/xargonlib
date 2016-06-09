package it.xargon.streams;

import java.io.IOException;

import it.xargon.events.*;
import it.xargon.xrpc.XRpcImmediate;

public interface IRemoteOutputStream extends EventsSource {
   @FunctionalInterface @Event @XRpcImmediate
   public interface StreamClosed extends Runnable {}
   public static Class<StreamClosed> STREAMCLOSED=StreamClosed.class;
   
   public boolean isOpen();
   public void close() throws IOException;
   public void flush() throws IOException; 
   public void write(byte[] b) throws IOException;
   public void write(int b) throws IOException;
}
