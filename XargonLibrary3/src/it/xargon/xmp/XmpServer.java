package it.xargon.xmp;

import java.util.Collection;
import java.io.IOException;

import it.xargon.events.*;
import it.xargon.smartnet.TcpServer;

public interface XmpServer extends EventsSourceImpl<XmpServer.Events> {
   @EventSink public interface Events {
      public abstract class Adapter implements XmpServer.Events {
         public void accepted(XmpConnection conn) {}
         public void exception(XmpException ex, XmpServer serv) {}
         public void removed(XmpConnection conn) {}
         public void started(XmpServer serv) {}
         public void stopped(XmpServer serv) {}
      }
      @Event public void started(XmpServer serv);
      @Event public void accepted(XmpConnection conn);
      @Event public void removed(XmpConnection conn);
      @Event public void stopped(XmpServer serv);
      @Event public void exception(XmpException ex, XmpServer serv);
   }
   
   public void start() throws IOException;
   public void shutdown() throws IOException;
   public Collection<XmpConnection> stop() throws IOException;
   public boolean isRunning();
   public Collection<XmpConnection> getConnections();
   public XmpFactory getFactory();
   public TcpServer getTcpServer();
}
