package it.xargon.smartnet;

import java.util.Collection;
import java.io.IOException;
import java.net.InetSocketAddress;

import it.xargon.events.*;

public interface TcpServer extends EventsSource {   
   @FunctionalInterface @Event
   public interface Started extends Runnable {}
   public static Class<Started> STARTED=Started.class;

   @FunctionalInterface @Event
   public interface Stopped extends Runnable {}
   public static Class<Stopped> STOPPED=Stopped.class;
   
   @FunctionalInterface @Event
   public interface Build {public void with(TcpConnection conn);}
   public static Class<Build> BUILD=Build.class;

   @FunctionalInterface @Event
   public interface Removed {public void with(TcpConnection conn);}
   public static Class<Removed> REMOVED=Removed.class;

   @FunctionalInterface @Event
   public interface ServerException {public void with(Exception ex);}
   public static Class<ServerException> SERVEREXCEPTION=ServerException.class;
   
   public void start() throws IOException;
   public Collection<TcpConnection> stop() throws IOException;
   public void shutdown() throws IOException;
   public boolean isRunning();
   public Collection<TcpConnection> getConnections();
   public InetSocketAddress getListeningAddress();
   public SmartnetFactory getFactory();
}
