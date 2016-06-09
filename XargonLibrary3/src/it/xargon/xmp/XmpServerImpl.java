package it.xargon.xmp;

import java.io.*;
import java.util.*;

import it.xargon.events.*;
import it.xargon.smartnet.*;

class XmpServerImpl extends EventsSourceImpl<XmpServer.Events> implements XmpServer {
   private HashMap<TcpConnection, XmpConnection> xmpconns=null;
   private int maxJobs=0;
   private XmpFactory fact=null;
   
   private TcpServer tcpserv=null;
   private TcpServer.Events tcpeventsink=new TcpServer.Events() {
      public void started(TcpServer serv) {e_started(serv);}
      public void build(TcpServer serv, TcpConnection conn) {e_build(conn);}
      public void removed(TcpServer serv, TcpConnection conn) {e_removed(conn);}
      public void stopped(TcpServer serv) {e_stopped(serv);}
      public void serverSocketException(TcpServer serv, Exception ex) {raiseEvent.exception(new XmpException(ex), XmpServerImpl.this);}
   };
   
   public XmpServerImpl(XmpFactory factory, TcpServer serv, int maxJobsPerConnection) {
      super(factory.getThreadPool());
      fact=factory;
      maxJobs=maxJobsPerConnection;
      xmpconns=new HashMap<TcpConnection, XmpConnection>();
      tcpserv=serv;
      tcpserv.register(tcpeventsink);
   }

   private void e_started(TcpServer serv) {
      Thread.currentThread().setName("XmpServer - " + tcpserv.toString());
      raiseEvent.started(this);
   }
   
   private void e_build(TcpConnection conn) {
      //TODO: supporto per rilevare una riconnessione
      //TODO: non sarebbe meglio effettuare il dialogo sulla crittazione QUI?
      
      XmpConnection xmpconn=new XmpConnectionTcpImpl(fact, conn, maxJobs);
      synchronized (xmpconns) {xmpconns.put(conn, xmpconn);}
      raiseEvent.accepted(xmpconn);
      //Anche qui, come nel TCPServer, la responsabilità di avviare
      //la gestione effettiva della connessione è nelle mani del
      //listener
   }
   
   private void e_removed(TcpConnection conn) {
      XmpConnection xmpconn=null;
      synchronized (xmpconns) {xmpconn=xmpconns.remove(conn);}
      raiseEvent.removed(xmpconn);
   }
      
   private void e_stopped(TcpServer serv) {raiseEvent.stopped(this);}
      
   public Collection<XmpConnection> getConnections() {
      synchronized (xmpconns) {
         return Collections.unmodifiableCollection(new HashSet<XmpConnection>(xmpconns.values()));
      }
   }
   
   public String toString() {return "LXmpServerImpl:" + tcpserv.toString();}
   public boolean isRunning() {return tcpserv.isRunning();}
   public void start() throws IOException {tcpserv.start();}
   public Collection<XmpConnection> stop() throws IOException {
      Collection<XmpConnection> result=getConnections();
      tcpserv.stop();
      return result;
   }
   public void shutdown() throws IOException {tcpserv.shutdown();}
   
   public XmpFactory getFactory() {return fact;}

   public TcpServer getTcpServer() {return tcpserv;}
}
