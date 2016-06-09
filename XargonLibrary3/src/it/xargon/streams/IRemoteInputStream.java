package it.xargon.streams;

import java.io.*;

import it.xargon.events.*;
import it.xargon.xrpc.XRpcImmediate;

public interface IRemoteInputStream extends EventsSource {
   @FunctionalInterface @Event @XRpcImmediate
   public interface StreamClosed extends Runnable {}
   public static Class<StreamClosed> STREAMCLOSED=StreamClosed.class;
   
   public boolean isOpen();
   public int available() throws IOException;
   public void close() throws IOException;
   public int read() throws IOException; 
   public byte[] read(int max) throws IOException; 
   public long skip(long n)throws IOException;
}
