package it.xargon.nioxmp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;

import it.xargon.channels.SelectionProcessor;
import it.xargon.channels.SelectorWorker;
import it.xargon.events.EventsSourceImpl;

public class TcpServerChannelSupplier extends EventsSourceImpl implements ActiveChannelSupplier {
   private XmpFactory xf=null;
   private ArrayList<ServerSocketChannel> serverSockets=null;

   TcpServerChannelSupplier(XmpFactory xmpFactory, int port, String... interfaces) throws IOException {
      this.xf=xmpFactory;
      serverSockets=new ArrayList<>();
      
      //Only if the server is required to bind to a specific set of local interfaces
      ArrayList<InetSocketAddress> selLocalAddresses=new ArrayList<>();
      
      if (interfaces!=null && interfaces.length>0) {
         //Retrieve all local public IP addresses
         ArrayList<String> allLocalAddresses=new ArrayList<>();
         for (Enumeration<NetworkInterface> allNics=NetworkInterface.getNetworkInterfaces(); allNics.hasMoreElements();) {
            NetworkInterface nic=allNics.nextElement();
            for (Enumeration<InetAddress> nicAddresses=nic.getInetAddresses(); nicAddresses.hasMoreElements();)
               allLocalAddresses.add(nicAddresses.nextElement().getHostAddress());
         }
         
         //Check if each requested interfaces belongs to the local system
         for(String ifName:interfaces) {
            InetAddress addr=InetAddress.getByName(ifName);
            String hostAddr=addr.getHostAddress();
            if (!allLocalAddresses.contains(hostAddr))
               throw new IllegalArgumentException("[" + ifName + "] is not a local network interface");
            
            //Let's just store it, we will bind to it later
            selLocalAddresses.add(new InetSocketAddress(addr, port));
         }
      } else {
         //No interfaces specified - we just bind to all local addresses
         selLocalAddresses.add(new InetSocketAddress(port));         
      }
      
      //Create a ServerSocketChannel for each required interface
      //We need also to configure them in non-blocking mode and register
      //them to a local selector processor, in order to process incoming
      //connections via NIO
      for(InetSocketAddress listenAddress:selLocalAddresses) {
         ServerSocketChannel servSocket=ServerSocketChannel.open();
         servSocket.bind(listenAddress);
         servSocket.configureBlocking(false);
         serverSockets.add(servSocket);
         xf.getSelectorWorker().register(servSocket, servSocket.validOps(), processAccept);
      }
   }
   
   //Will be invoked when a connection is ready to be accepted
   private SelectionProcessor processAccept=new SelectionProcessor() {
      @Override
      public Runnable processKey(SelectorWorker worker, SelectionKey key) throws Exception {
         ServerSocketChannel serverSocket=(ServerSocketChannel) key.channel();
         SocketChannel client = serverSocket.accept();
         client.configureBlocking(false);
         SolidByteChannel managedChannel=new SolidByteChannel(client);
         
         //We need to notify the new channel to interested clients (XmpClient)
         //but on another thread. We are leaving the task to the selector worker
         //that will create a thread for us.
         
         return new Notifier(managedChannel);
      }
   };
   
   private class Notifier implements Runnable {
      private SelectableByteChannel newChannel=null;
            
      public Notifier(SelectableByteChannel newChannel) {
         this.newChannel=newChannel;
      }

      @Override
      public void run() {
         raise(ActiveChannelSupplier.ChannelAvailable.class).with(newChannel);
      }
   }
   
   @Override
   protected ExecutorService getThreadPool() {
      return xf.getThreadPool();
   }

   @Override
   public void close() throws IOException {
      for(ServerSocketChannel srvChan:serverSockets) {
         SelectionKey regKey=xf.getSelectorWorker().getRegistrationKey(srvChan);
         regKey.cancel();
         srvChan.close();
      }
      raise(ChannelSupplier.Closed.class);
      unregisterAll();
   }

}
