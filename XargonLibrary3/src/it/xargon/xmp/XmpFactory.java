package it.xargon.xmp;

import java.io.*;
import it.xargon.smartnet.*;
import it.xargon.util.Debug;

import java.net.*;
import java.util.concurrent.*;

public class XmpFactory {
   private static XmpFactory defaultFactory=null;
   private static WriterProbe defaultProbe=null;
   private ExecutorService ithreadPool=null;
   
   private XmpFactory() {
      this(null);
   }
   
   public XmpFactory(ExecutorService threadPool) {
      ithreadPool=threadPool;
   }
   
   public static XmpFactory defaultLXmpFactory() {
      if (defaultFactory==null) defaultFactory=new XmpFactory();
      return defaultFactory;
   }
      
   public ExecutorService getThreadPool() {return ithreadPool;}
            
   public void simpleEvent(String host, int port, byte[] contents) throws IOException {
      InetSocketAddress remoteaddr=new InetSocketAddress(host, port);
      XmpConnection conn=newXmpLightConnection(remoteaddr);
      conn.start();
      conn.sendEvent(contents);
      conn.close();
   }
   
   public byte[] simpleRequest(String host, int port, byte[] contents) throws IOException {
      InetSocketAddress remoteaddr=new InetSocketAddress(host, port);
      XmpConnection conn=newXmpLightConnection(remoteaddr);
      conn.start();
      byte[] incoming=conn.sendRequest(contents);
      conn.close();
      return incoming;
   }
   
   public XmpServer newXmpServer(TcpServer tcpserv) {
      return new XmpServerImpl(this, tcpserv, 0);
   }
   
   public XmpServer newXmpServer(TcpServer tcpserv, int maxjobs) {
      return new XmpServerImpl(this, tcpserv, maxjobs);
   }
   
   public XmpConnection newXmpConnection(TcpConnection tcpconn) throws IOException {
      return new XmpConnectionTcpImpl(this, tcpconn, 0);
   }   
   
   public XmpConnection newXmpConnection(TcpConnection tcpconn, int maxjobs) throws IOException {
      return new XmpConnectionTcpImpl(this, tcpconn, maxjobs);
   }
   
   public XmpConnection newXmpConnection(InputStream inStream, OutputStream outStream) throws IOException {
      return new XmpConnectionStreamsImpl(this, inStream, outStream, 0);      
   }

   public XmpConnection newXmpConnection(InputStream inStream, OutputStream outStream, int maxjobs) throws IOException {
      return new XmpConnectionStreamsImpl(this, inStream, outStream, maxjobs);
   }
   
   public XmpConnection newXmpLightConnection(InetSocketAddress remoteaddr) throws IOException {
      Socket sock=new Socket(remoteaddr.getAddress(), remoteaddr.getPort());
      return newXmpLightConnection(sock);
   }

   public XmpConnection newXmpLightConnection(InetSocketAddress remoteaddr, InetSocketAddress localaddr) throws IOException {
      Socket sock=new Socket();
      sock.bind(localaddr);
      sock.connect(remoteaddr);
      return newXmpLightConnection(sock);
   }

   public XmpConnection newXmpLightConnection(Socket sck) throws IOException {
      return new XmpConnectionLightImpl(sck, this);
   }
   
   public static XmpMessageProbe.Events getDefaultMessageProbe() {
      if (defaultProbe==null) defaultProbe=new WriterProbe(Debug.stderr);
      return defaultProbe;
   }
   
   private static class WriterProbe implements XmpMessageProbe.Events {
      private PrintWriter out=null;
      
      public WriterProbe(PrintWriter _out) {out=_out;}
      
      @Override public void discardedAnswer(XmpConnection conn, XmpMessage discarded) {
         if (discarded!=null) {
            discarded.printout("L.... ", out);
            out.println();
         }
      }

      @Override public void eventSent(XmpConnection conn, XmpMessage outgoing) {
         if (outgoing!=null) {
            outgoing.printout("L-->R ", out);
            out.println();
         }
      }

      @Override public void requestSent(XmpConnection conn, XmpMessage outgoing) {
         if (outgoing!=null) {
            outgoing.printout("L-->R ", out);
            out.println();
         }
      }
      
      @Override public void answerReceived(XmpConnection conn, XmpMessage incoming) {
         if (incoming!=null) {
            incoming.printout("L<--R ", out);
            out.println();
         }
      }
      
      @Override public void preProcessMessage(XmpConnection conn, XmpMessage incoming) {
         if (incoming!=null) {
            incoming.printout("R-->L ", out);
            out.println();
         }
      }
      
      @Override public void postProcessMessage(XmpConnection conn, XmpMessage incoming, XmpMessage outgoing) {
         if (outgoing!=null) {
            outgoing.printout("R<--L ", out);
            out.println();
         }
      }
   }
}
