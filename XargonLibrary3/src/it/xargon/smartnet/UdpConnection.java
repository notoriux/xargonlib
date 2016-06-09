package it.xargon.smartnet;

import java.io.*;
import java.net.*;

import it.xargon.events.*;

public interface UdpConnection extends EventsSource {
   @FunctionalInterface @Event
   public interface Started {public void raise(UdpConnection conn);}
   public static Class<Started> STARTED=Started.class;

   @FunctionalInterface @Event
   public interface Stopped {public void raise(UdpConnection conn);}
   public static Class<Stopped> STOPPED=Stopped.class;
   
   @FunctionalInterface @Event
   public interface DataArrived {public void raise(UdpConnection conn, InetSocketAddress source, byte[] data);}
   public static Class<DataArrived> DATAARRIVED=DataArrived.class;
   
   public void start() throws IOException;
   public void close() throws IOException;
   public void forceClose();
   
   public void send(byte[] data) throws IOException;
   public void send(InetSocketAddress dest, byte[] data) throws IOException;
   
   public void setBroadcast(boolean on) throws SocketException;
   public boolean isBroadcast() throws SocketException;
   
   public boolean isRunning();
   public InetSocketAddress getLocalSocketAddress();
   public InetSocketAddress getRemoteSocketAddress();
}
