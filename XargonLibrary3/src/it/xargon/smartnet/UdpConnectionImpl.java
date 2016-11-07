package it.xargon.smartnet;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

import it.xargon.events.*;
import it.xargon.util.*;

class UdpConnectionImpl extends EventsSourceImpl implements UdpConnection {
   private InetSocketAddress locAddr=null;
   private InetSocketAddress remAddr=null;
   private DatagramSocket udpsock=null;
   private int ibufsize=0;
   private ExecutorService ithreadPool=null;
   private Future<?> connectionTask=null;
   private volatile boolean timeToClose=false;
   private volatile BooleanLatch startlock=null;
   private Thread runner=null;

   public UdpConnectionImpl(ExecutorService threadPool, InetSocketAddress localaddr, InetSocketAddress remoteaddr, int bufsize) {
      ithreadPool=threadPool;
      locAddr=localaddr;
      remAddr=remoteaddr;
      ibufsize=bufsize;
   }

   @Override
   protected ExecutorService getThreadPool() {return ithreadPool;}
   
   public synchronized void start() throws IOException {
      timeToClose=false;
      startlock=new BooleanLatch();

      udpsock=new DatagramSocket(locAddr);
      if (ibufsize==0) ibufsize=udpsock.getReceiveBufferSize();
      else {
         udpsock.setReceiveBufferSize(ibufsize);
         udpsock.setSendBufferSize(ibufsize);
      }
      if (remAddr!=null) udpsock.connect(remAddr);
      udpsock.setSoTimeout(10);

      if (ithreadPool==null) {
         FutureTask<Object> task=new FutureTask<Object>(new Runnable() {public void run() {internalRun();}}, null);
         connectionTask=task;
         new Thread(task).start();
      } else {
         connectionTask=ithreadPool.submit(new Runnable() {public void run() {internalRun();}});
      }

      try {startlock.await();} catch (InterruptedException ex) {}
      startlock=null;
      raise(STARTED).raise(this);      
   }

   public synchronized void close() throws IOException {
      if (!isRunning()) return;
      timeToClose=true;
      udpsock.close();
      connectionTask.cancel(false);
      
      if (Thread.currentThread()!=runner) {
         try {connectionTask.get();} catch (Exception ignored) {}         
      }
   }

   public void forceClose() {
      try {close();}
      catch (IOException ignored) {}
   }

   public InetSocketAddress getLocalSocketAddress() {return locAddr;}

   public InetSocketAddress getRemoteSocketAddress() {return remAddr;}

   public void setBroadcast(boolean on) throws SocketException {
      if (!isRunning()) return;
      udpsock.setBroadcast(on);
   }
   
   public boolean isBroadcast() throws SocketException {
      if (!isRunning()) return false;
      return udpsock.getBroadcast();
   }

   public boolean isRunning() {
      return ((connectionTask!=null) && (!connectionTask.isDone()));
   }

   public void send(byte[] data) throws IOException {
      if (data==null) throw new NullPointerException();
      DatagramPacket packet=new DatagramPacket(data, data.length, remAddr);
      udpsock.send(packet);
   }

   public void send(InetSocketAddress dest, byte[] data) throws IOException {
      if (data==null) throw new NullPointerException();
      DatagramPacket packet=new DatagramPacket(data, data.length, dest);
      udpsock.send(packet);
   }
  
   private void internalRun() {
      DatagramPacket packet=new DatagramPacket(new byte[ibufsize], ibufsize);
      
      runner=Thread.currentThread();
      runner.setName("UdpConnection");
      if (startlock!=null) startlock.open();
      do {
         try {
            udpsock.receive(packet);
            SocketAddress source=packet.getSocketAddress();
            if (source instanceof InetSocketAddress)
               raise(DATAARRIVED).raise(this, (InetSocketAddress)source, packet.getData());
         } catch (SocketTimeoutException ignored) {
         } catch (IOException ex) {
            forceClose();
         }
      } while (!timeToClose);
      
      raise(STOPPED).raise(this);
   }
}
