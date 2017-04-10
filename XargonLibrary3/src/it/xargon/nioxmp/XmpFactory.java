package it.xargon.nioxmp;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.xargon.channels.SelectorWorker;
import it.xargon.events.EventsSource;
import it.xargon.events.OnEvent;
import it.xargon.niomarshal.DataBridge;
import it.xargon.nioxmp.msg.MarXmpContentRequest;
import it.xargon.util.ByteBufferAllocator;
import it.xargon.util.Identifier;

public class XmpFactory implements Closeable {
   private DataBridge dataBridge=null;
   private SelectorWorker selectorWorker=null;
   private ArrayList<ChannelSupplier> allSuppliers=new ArrayList<ChannelSupplier>();
   private Object supplSync=new Object();
   private ExecutorService threadPool=null;
   private ByteBufferAllocator allocator=ByteBufferAllocator.INDIRECT;
   
   public XmpFactory() {
      dataBridge=new DataBridge(allocator);
      dataBridge.installMarshallersFromPackage(MarXmpContentRequest.class.getPackage().getName());
      threadPool=Executors.newCachedThreadPool();
      selectorWorker=new SelectorWorker(threadPool);
      selectorWorker.start();
   }
   
   @Override
   public void close() throws IOException {
      ChannelSupplier[] supplsView=null;
      synchronized (supplSync) {
         supplsView=allSuppliers.toArray(new ChannelSupplier[allSuppliers.size()]);
      }
      for(ChannelSupplier suppl:supplsView) suppl.close();
      selectorWorker.stop();
      threadPool.shutdown();
   }
   
   DataBridge getDataBridge() {return dataBridge;}
   
   SelectorWorker getSelectorWorker() {return selectorWorker;}
   
   ExecutorService getThreadPool() {return threadPool;}
   
   ByteBufferAllocator getAllocator() {return allocator;}
   
   @OnEvent(ChannelSupplier.Closed.class)
   private void ChannelSupplierClosed() {
      ChannelSupplier suppl=EventsSource.getCurrentEventSource(ChannelSupplier.class);
      synchronized (supplSync) {allSuppliers.remove(suppl);}
   }
   
   public TcpServerChannelSupplier newTcpServerChannelSupplier(int port, String... interfaces) throws IOException {
      TcpServerChannelSupplier suppl=new TcpServerChannelSupplier(this, port, interfaces);
      synchronized (supplSync) {allSuppliers.add(suppl);}
      suppl.bindEvents(this);
      return suppl;
   }

   public TcpClientChannelSupplier newTcpClientChannelSupplier(String destAddress, int port) {
      TcpClientChannelSupplier suppl=new TcpClientChannelSupplier(this, destAddress, port);
      synchronized (supplSync) {allSuppliers.add(suppl);}      
      suppl.bindEvents(this);
      return suppl;
   }
   
   public XmpServer newXmpServer(ActiveChannelSupplier supplier) {
      return null;
   }
   
   public XmpEndpoint newXmpClient(PassiveChannelSupplier supplier) {
      return null;
   }
}
