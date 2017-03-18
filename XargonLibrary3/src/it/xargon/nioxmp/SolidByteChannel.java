package it.xargon.nioxmp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Objects;

import it.xargon.channels.SelectionProcessor;
import it.xargon.channels.SelectorWorker;

class SolidByteChannel implements SelectableByteChannel {
   private ByteChannel asByteChannel=null;
   private SelectableChannel asSelChannel=null;
   
   public SolidByteChannel(ByteChannel channel) {
      if (!(Objects.requireNonNull(channel) instanceof SelectableChannel))
         throw new IllegalArgumentException("Provided channel must also extend SelectableChannel");
      asByteChannel=channel;
      asSelChannel=(SelectableChannel)channel;
   }

   @Override
   public int read(ByteBuffer dst) throws IOException {return asByteChannel.read(dst);}

   @Override
   public int write(ByteBuffer src) throws IOException {return asByteChannel.write(src);}

   @Override
   public boolean isOpen() {return asByteChannel.isOpen();}

   @Override
   public void close() throws IOException {asByteChannel.close();}

   @Override
   public SolidByteChannel configureBlocking(boolean block) throws IOException {
      asSelChannel.configureBlocking(block); 
      return this;
   }
   
   @Override
   public boolean isBlocking() {return asSelChannel.isBlocking();}
   
   @Override
   public boolean isRegistered() {return asSelChannel.isRegistered();}

   @Override
   public SelectionKey register(SelectorWorker worker, int ops, SelectionProcessor proc) throws ClosedChannelException {
      return worker.register(asSelChannel, ops, proc);
   }
   
   @Override
   public int validOps() {return asSelChannel.validOps();}
}
