package it.xargon.nioxmp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.ArrayDeque;
import java.util.concurrent.ExecutorService;

import it.xargon.channels.SelectionProcessor;
import it.xargon.channels.SelectorWorker;
import it.xargon.events.Event;
import it.xargon.niomarshal.Parser;
import it.xargon.nioxmp.msg.XmpMessage;

class XmpWire extends it.xargon.events.EventsSourceImpl {
   private final static int BUFFER_LEN=4096;
   
   @FunctionalInterface @Event
   public interface MessageReceived {public void with(XmpMessage msg);}
   
   @FunctionalInterface @Event
   public interface UnknownObjectOnWire {public void with(Object obj);}
      
   @FunctionalInterface @Event
   public interface ConnectionClosed {public void with(ByteBuffer[] unsentBuffer);}

   private XmpFactory xf=null;
   private SelectableByteChannel channel=null;
   
   private SelectionKey readRegKey=null;
   private ByteBuffer readBuffer=null;
   
   private Object writeLock=new Object();
   private SelectionKey writeRegKey=null;
   private ArrayDeque<ByteBuffer> writeBuffer=null;
   
   private Parser parser=null;
   
   public enum WorkMode {SYNC, ASYNC}
   private WorkMode workMode=WorkMode.SYNC;
   
   public XmpWire(XmpFactory xf, SelectableByteChannel channel) throws IOException {
      this.xf=xf;
      this.channel=channel;
      
      readBuffer=xf.getAllocator().alloc(BUFFER_LEN);
      writeBuffer=new ArrayDeque<ByteBuffer>();
      parser=new Parser(xf.getAllocator());
      
      channel.configureBlocking(true);
   }
   
   private void doAfterChannelClosed() {
      ByteBuffer[] unsentBuffer=null;
      synchronized (writeLock) {
         if (readRegKey!=null) readRegKey.cancel();readRegKey=null;
         if (writeRegKey!=null) writeRegKey.cancel();writeRegKey=null;
         if (writeBuffer.size()!=0)
            unsentBuffer=writeBuffer.toArray(new ByteBuffer[writeBuffer.size()]);
      }
      XmpWire.this.raise(ConnectionClosed.class).with(unsentBuffer);
   }
   
   private SelectionProcessor readProc=new SelectionProcessor() {      
      @Override
      public Runnable processKey(SelectorWorker worker, SelectionKey key) throws Exception {
         if (key.isReadable()) {
            int len=channel.read(readBuffer);
            if (len<0) {
               doAfterChannelClosed();
               return null;
            }
            
            ByteBuffer[] parsedBlocks=parser.feed(readBuffer);
            readBuffer.clear();
            if (parsedBlocks!=null && parsedBlocks.length>0) return new Notifier(parsedBlocks);
         }
         
         return null;
      }
   };
   
   private SelectionProcessor writeProc=new SelectionProcessor() {      
      @Override
      public Runnable processKey(SelectorWorker worker, SelectionKey key) throws Exception {
         if (key.isWritable()) {
            ByteBuffer outgoing=null;
            
            synchronized (writeLock) {
               if (writeBuffer.size()==0) {
                  writeRegKey.cancel();
                  writeRegKey=null;
                  return null;
               }
               outgoing=writeBuffer.getFirst();
            }
            
            try {
               channel.write(outgoing);
            } catch (IOException ex) {
               doAfterChannelClosed();
               return null;
            }
            
            if (!outgoing.hasRemaining()) {
               synchronized (writeLock) {
                  writeBuffer.remove(outgoing);
                  if (writeBuffer.size()==0) {
                     writeRegKey.cancel();
                     writeRegKey=null;
                  }
               }
            }
         }

         return null;
      }
   };
   
   private class Notifier implements Runnable {
      private ByteBuffer[] parsedBlocks=null;
      
      public Notifier(ByteBuffer[] parsedBlocks) {this.parsedBlocks=parsedBlocks;}
      
      @Override
      public void run() {
         for(ByteBuffer block:parsedBlocks) {
            Object obj=xf.getDataBridge().unmarshal(block);
            if (obj instanceof XmpMessage) XmpWire.this.raise(MessageReceived.class).with((XmpMessage)obj);
            else XmpWire.this.raise(UnknownObjectOnWire.class).with(obj);
         }
      }
      
   }
   
   public WorkMode getCurrentWorkMode() {return workMode;}
   
   public void setWorkModeAsync(ByteBuffer[] unsentBuffer) throws IOException {
      if (workMode.equals(WorkMode.ASYNC)) return;
      
      synchronized (writeLock) {
         channel.configureBlocking(false);
         readRegKey=channel.register(xf.getSelectorWorker(), SelectionKey.OP_READ, readProc);
         if (unsentBuffer!=null && unsentBuffer.length>0)
            for(ByteBuffer buf:unsentBuffer) writeBuffer.addLast(buf);
         writeRegKey=channel.register(xf.getSelectorWorker(), SelectionKey.OP_WRITE, writeProc);
         workMode=WorkMode.ASYNC;
      }
   }
   
   public ByteBuffer[] setWorkModeSync() throws IOException {
      if (workMode.equals(WorkMode.SYNC)) return null;
      
      ByteBuffer[] result=null;
      
      synchronized (writeLock) {
         readRegKey.cancel(); readRegKey=null;
         writeRegKey.cancel(); writeRegKey=null;
         channel.configureBlocking(true);
         if (writeBuffer.size()!=0)
            result=writeBuffer.toArray(new ByteBuffer[writeBuffer.size()]);
         workMode=WorkMode.SYNC;
      }
      
      return result;
   }
   

   public void close() throws IOException {
      channel.close();
      if (workMode.equals(WorkMode.SYNC)) doAfterChannelClosed();
   }
   
   public XmpMessage receive() throws IOException {
      if (!channel.isOpen()) throw new ClosedChannelException();
      if (workMode.equals(WorkMode.ASYNC)) throw new IllegalStateException("Wire is in ASYNC mode");
      
      ByteBuffer parsedBlock=null;
      ByteBuffer ibuf=xf.getAllocator().alloc(1);
      XmpMessage result=null;
      
      while (parsedBlock==null) {
         int len=channel.read(ibuf);
         if (len<0) {
            doAfterChannelClosed();
            return null;
         }
         
         parsedBlock=parser.feed(ibuf)[0]; //By feeding one byte at a time, we will get a single bytebuffer
         readBuffer.clear();
      }
      
      Object obj=xf.getDataBridge().unmarshal(parsedBlock);
      if (obj instanceof XmpMessage) result=(XmpMessage)obj;
      else XmpWire.this.raise(UnknownObjectOnWire.class).with(obj);
      
      return result;
   }
   
   public void send(XmpMessage msg) throws IOException {
      if (!channel.isOpen()) throw new ClosedChannelException();
      
      ByteBuffer bytemsg=xf.getDataBridge().marshal(msg);
      switch (workMode) {
         case ASYNC:
            synchronized (writeLock) {
               writeBuffer.addLast(bytemsg);
               if (writeRegKey==null)
                  writeRegKey=channel.register(xf.getSelectorWorker(), SelectionKey.OP_WRITE, writeProc);
            }
            break;
         case SYNC:
            channel.write(bytemsg);
            break;
      }
   }
   
   public boolean isOpen() {
      return channel.isOpen();
   }
   
   @Override
   protected ExecutorService getThreadPool() {
      return xf.getThreadPool();
   }
}
