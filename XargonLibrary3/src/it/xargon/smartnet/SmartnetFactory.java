package it.xargon.smartnet;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;

/**
 * This class contains factory methods that allows the creation of needed TCP object sharing common
 * resources (in this case, just the thread pool)
 * 
 * @author Francesco Muccilli
 */
public class SmartnetFactory {
   private static SmartnetFactory ifactory=null;
   private ExecutorService ithreadPool=null;

   /**
    * Standard constructor: no thread pool will be used
    */
   public SmartnetFactory() {
      this(null);
   }
   
   /**
    * Using this constructor allows all the objects to use a common shared thread pool manager.
    * Remembert that the passed thread pool must be explicitly shut down before program termination,
    * otherwise the JVM won't terminate by itself
    * @param threadPool the thread pool manager that will be used to execute parallel tasks
    */
   public SmartnetFactory(ExecutorService threadPool) {
      ithreadPool=threadPool;
   }
   
   /**
    * This allows the immediate use of a singleton instance of the factory, shared by the whole JVM
    * @return JVM-level factory for SmartNet objects.
    */
   public static SmartnetFactory defaultFactory() {
      if (ifactory==null) ifactory=new SmartnetFactory();
      return ifactory;
   }
   
   /**
    * Any class interested in the currently used thread pool manager could obtain a reference via
    * this method.
    * @return currently used thread pool manager, or null if no one installed.
    */
   public ExecutorService getThreadPool() {return ithreadPool;}

   /**
    * Creates a SmartNet TCP Server that will listen for incoming connection on the specified socket address
    * @param endPoint a TCP socket address (IP:PORT) specifing where the server will listen
    * @return a SmartNet TCP Server ready to be used by registering sinks. Server won't be active until start()
    * will be called
    */
   public TcpServer newTcpServer(InetSocketAddress endPoint) {
      return new TcpServerImpl(this, endPoint);
   }

   public TcpConnection newTcpConnection(InetSocketAddress remoteaddr) {
      return new TcpConnectionImpl(this, remoteaddr, null);
   }

   public TcpConnection newTcpConnection(InetSocketAddress remoteaddr, InetSocketAddress localaddr) {
      return new TcpConnectionImpl(this, remoteaddr, localaddr);
   }

   public TcpConnection newTcpConnection(Socket sck) throws IOException {
      return new TcpConnectionImpl(this, null, sck);
   }
   
   public UdpConnection newUdpConnection(InetSocketAddress localaddr) {
      return new UdpConnectionImpl(ithreadPool, localaddr, null, 0);
   }

   public UdpConnection newUdpConnection(InetSocketAddress localaddr, int bufsize) {
      return new UdpConnectionImpl(ithreadPool, localaddr, null, bufsize);
   }

   public UdpConnection newUdpConnection(InetSocketAddress localaddr, InetSocketAddress remoteaddr, int bufsize) {
      return new UdpConnectionImpl(ithreadPool, localaddr, remoteaddr, bufsize);
   }
}
