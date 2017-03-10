package it.xargon.smartnet;

import it.xargon.events.*;
import it.xargon.streams.*;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;

class TcpConnectionImpl extends EventsSourceImpl implements TcpConnection {
   private final static int DEFAULT_SOTIMEOUT=10;
   
   private Socket iSock=null;
   private InetSocketAddress localSockAddr=null;
   private InetSocketAddress remoteSockAddr=null;
   private SmartInputStream smartIn=null;
   private OutputStream sockOutputStream=null;
   private ExecutorService ithreadPool=null;
   private SmartnetFactory ifactory=null;
   private TcpServerImpl parentServer=null;
   private int origSoTimeout=0;

   public TcpConnectionImpl(SmartnetFactory factory, InetSocketAddress remoteaddr, InetSocketAddress localaddr) {
      super();
      ifactory=factory;
      if (remoteaddr==null) throw new IllegalArgumentException("Unexpected null argument");
      ithreadPool=factory.getThreadPool();
      remoteSockAddr=remoteaddr;
      if (localaddr==null) return;
      if (!localaddr.getAddress().isSiteLocalAddress()) throw new IllegalArgumentException("The provided local address binding is not local");
      localSockAddr=localaddr;
   }
   
   public TcpConnectionImpl(SmartnetFactory factory, TcpServerImpl parent, Socket sck) throws IOException {
      super();
      ifactory=factory;
      if (sck==null) throw new IllegalArgumentException("Unexpected null argument");
      if (!sck.isConnected()) throw new IllegalArgumentException("The provided socket is not connected");
      ithreadPool=factory.getThreadPool();
      parentServer=parent;
      iSock=sck;
      localSockAddr=(InetSocketAddress)iSock.getLocalSocketAddress();
      remoteSockAddr=(InetSocketAddress)iSock.getRemoteSocketAddress();
   }
   
   @Override
   protected ExecutorService getThreadPool() {return ifactory.getThreadPool();}
   
   private synchronized void commonStart() throws IOException {      
      if (iSock==null)  {
         iSock=new Socket();
         if (localSockAddr!=null) iSock.bind(localSockAddr);
         iSock.connect(remoteSockAddr);
         localSockAddr=InetSocketAddress.class.cast(iSock.getLocalSocketAddress());
      }
      
      iSock.setTcpNoDelay(true);
      iSock.setKeepAlive(true);
      origSoTimeout=iSock.getSoTimeout();
      iSock.setSoTimeout(DEFAULT_SOTIMEOUT);
      
      sockOutputStream=new ImmediateOutputStream(iSock.getOutputStream());
      smartIn=new SmartInputStream(iSock.getInputStream(), ithreadPool);
      smartIn.bindEvents(this);
   }
   
   @OnEvent(SmartInputStream.Started.class)
   private void smartISStarted() {raise(CONNECTED);}
   
   @OnEvent(SmartInputStream.Stopped.class)
   private void smartISStopped() {
      try {iSock.close();} catch (IOException ex) {}
      raise(DISCONNECTED);
      smartIn.unregisterAll();
      if (parentServer!=null) parentServer.connectionCleanup(this);
   }
   
   @OnEvent(SmartInputStream.Suspended.class)
   private void smartISSuspended() {
      try {iSock.setSoTimeout(origSoTimeout);} catch (IOException ignored) {}
   }
   
   @OnEvent(SmartInputStream.Restored.class)
   private void smartISRestored() {
      try {iSock.setSoTimeout(DEFAULT_SOTIMEOUT);} catch (IOException ignored) {}
   }
   
   @OnEvent(SmartInputStream.DataArrived.class)
   private void smartISDataArrived(byte[] data, int length) {
      byte[] sdata=new byte[length];
      System.arraycopy(data,0,sdata,0,length);
      raise(TcpConnection.DATAARRIVED).with(data);
   }

   @OnEvent(SmartInputStream.StreamException.class)
   private Boolean smartISException(Exception ex) {
      if (ex instanceof SocketTimeoutException) return false; //lo stream non deve fermarsi!
      return true;
   }

   public synchronized void start() throws IOException {
      if (isRunning()) return;
      commonStart();
      smartIn.start();
      //Non ritorna finchè la connessione non è effettivamente "viva e attiva"
   }
   
   public synchronized DuplexChannel startSuspended() throws IOException {
      if (isRunning()) return null;
      commonStart();
      InputStream iis=smartIn.startSuspended();
      return new DuplexChannel(iis, sockOutputStream, null);
   }

   public void close() throws IOException {
      if (!isRunning()) return;
      synchronized (this) {
         InputStream is=smartIn.stop(); //aspetta che il flusso si sia fermato prima di ritornare!!!
         if (is!=null) is.close();
      }
   }
   
   public void forceClose() {try {close();} catch (IOException ex) {}}

   public DuplexChannel getDuplexChannel() {
      return new DuplexChannel(smartIn.suspendInputStream(), sockOutputStream, null);
   }
   
   public void setDuplexChannel(DuplexChannel chan) {
      smartIn.restoreInputStream(chan.getInputStream());
      sockOutputStream=chan.getOutputStream();
   }
   
   public OutputStream getOutputStream() {return sockOutputStream;}

   public boolean isConnected() {
      if (iSock==null) return false;
      return (iSock.isConnected() && !iSock.isClosed());
   }
   
   public boolean isRunning() {
      if (smartIn==null) return false;
      return smartIn.isRunning();
   }
   
   public String toString() {
      return "TcpConnectionImpl:" + ((localSockAddr==null)?"":localSockAddr.toString() + ":") + remoteSockAddr.toString();
   }

   public InetSocketAddress getLocalAddress() {return localSockAddr;}

   public InetSocketAddress getRemoteAddress() {return remoteSockAddr;}

   public SmartnetFactory getFactory() {return ifactory;}
}
