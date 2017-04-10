package it.xargon.nioxmp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

import it.xargon.events.EventsSourceImpl;

public class TcpClientChannelSupplier extends EventsSourceImpl implements PassiveChannelSupplier {
   private XmpFactory xf=null;
   private InetSocketAddress destSocketAddress=null;
   
   TcpClientChannelSupplier(XmpFactory xmpFactory, String destAddress, int port) {
      this.xf=xmpFactory;
      destSocketAddress=new InetSocketAddress(destAddress, port);
   }

   @Override
   public SelectableByteChannel get() throws IOException {
      SocketChannel sockChannel=SocketChannel.open();
      sockChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
      sockChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
      sockChannel.connect(destSocketAddress);
      SolidByteChannel result=new SolidByteChannel(sockChannel);
      return result;
   }

   @Override
   public void close() throws IOException {
      raise(ChannelSupplier.Closed.class);
      unregisterAll();
   }

   @Override
   protected ExecutorService getThreadPool() {
      return xf.getThreadPool();
   }
}
