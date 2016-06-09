package it.xargon.smartnet;

import java.io.*;
import java.net.InetSocketAddress;

import it.xargon.events.*;
import it.xargon.streams.*;

public interface TcpConnection extends EventsSource {
   @FunctionalInterface @Event
   public interface Connected {public void raise(TcpConnection conn);}
   public static Class<Connected> CONNECTED=Connected.class;
   
   @FunctionalInterface @Event
   public interface Disconnected {public void raise(TcpConnection conn);}
   public static Class<Disconnected> DISCONNECTED=Disconnected.class;
   
   @FunctionalInterface @Event
   public interface DataArrived {public void raise(TcpConnection conn, byte[] data);}
   public static Class<DataArrived> DATAARRIVED=DataArrived.class;
   
   public InetSocketAddress getLocalAddress();
   public InetSocketAddress getRemoteAddress();
   public SmartnetFactory getFactory();
   
   public void start() throws IOException;
   public void close() throws IOException;
   public void forceClose();

   public DuplexChannel startSuspended() throws IOException;
   public DuplexChannel getDuplexChannel();
   public void setDuplexChannel(DuplexChannel chan);
   
   public OutputStream getOutputStream();

   public boolean isRunning();
   public boolean isConnected();
}
