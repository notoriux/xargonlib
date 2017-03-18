package it.xargon.nioxmp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

import it.xargon.channels.SelectionProcessor;
import it.xargon.channels.SelectorWorker;

class SplitByteChannel implements SelectableByteChannel {
   private ReadableByteChannel asSourceByteChannel=null;
   private WritableByteChannel asSinkByteChannel=null;
   private SelectableChannel asSourceSelChannel=null;
   private SelectableChannel asSinkSelChannel=null;
   
   public SplitByteChannel(ReadableByteChannel sourceChannel, WritableByteChannel sinkChannel) {
      if (!(Objects.requireNonNull(sourceChannel) instanceof SelectableChannel)
       || !(Objects.requireNonNull(sinkChannel) instanceof SelectableChannel))
         throw new IllegalArgumentException("Both channels must also extend SelectableChannel");
      asSourceByteChannel=sourceChannel;
      asSinkByteChannel=sinkChannel;
      asSourceSelChannel=(SelectableChannel)sourceChannel;
      asSinkSelChannel=(SelectableChannel)sinkChannel;
   }

   @Override
   public int read(ByteBuffer dst) throws IOException {return asSourceByteChannel.read(dst);}

   @Override
   public int write(ByteBuffer src) throws IOException {return asSinkByteChannel.write(src);}

   @Override
   public boolean isOpen() {
      if (asSourceByteChannel.isOpen()!=asSinkByteChannel.isOpen())
         throw new IllegalStateException("Compound channel open state not aligned "
               + "(source=" + (asSourceByteChannel.isOpen()?"open":"closed") + " "
               + " sink=" + (asSinkByteChannel.isOpen()?"open":"closed") + ")");
      return asSourceByteChannel.isOpen();
   }

   @Override
   public void close() throws IOException {
      asSourceByteChannel.close();
      asSinkByteChannel.close();
   }

   @Override
   public SplitByteChannel configureBlocking(boolean block) throws IOException {
      asSourceSelChannel.configureBlocking(block);
      asSinkSelChannel.configureBlocking(block); 
      return this;
   }
   
   @Override
   public boolean isBlocking() {
      if (asSourceSelChannel.isBlocking()!=asSinkSelChannel.isBlocking())
         throw new IllegalStateException("Compound channel blocking state not aligned "
               + "(source=" + (asSourceSelChannel.isBlocking()?"blocking":"nonblocking") + " "
               + " sink=" + (asSinkSelChannel.isBlocking()?"blocking":"nonblocking") + ")");
      return asSourceSelChannel.isBlocking();
   }
   
   @Override
   public boolean isRegistered() {
      if (asSourceSelChannel.isRegistered()!=asSinkSelChannel.isRegistered())
         throw new IllegalStateException("Compound channel registered state not aligned "
               + "(source=" + (asSourceSelChannel.isRegistered()?"registerd":"not registered") + " "
               + " sink=" + (asSinkSelChannel.isRegistered()?"registerd":"not registered") + ")");
      return asSourceSelChannel.isRegistered();
   }

   @Override
   public SelectionKey register(SelectorWorker worker, int ops, SelectionProcessor proc) throws ClosedChannelException {
      if ((ops & asSourceSelChannel.validOps())==asSourceSelChannel.validOps()
            && (ops & asSinkSelChannel.validOps())==asSinkSelChannel.validOps())
              throw new IllegalArgumentException("Specified operations set are valid for both source and sink channel. Please register each operation by itself");

      if ((ops & asSourceSelChannel.validOps())==asSourceSelChannel.validOps()) {
         if (asSinkSelChannel.isRegistered() && worker.getRegistrationKey(asSinkSelChannel)==null)
            throw new IllegalArgumentException("Source operations and sink operations must be registered on the same selector");
         
         return worker.register(asSourceSelChannel, ops, proc);
      }
      
      if ((ops & asSinkSelChannel.validOps())==asSinkSelChannel.validOps()) {
         if (asSourceSelChannel.isRegistered() && worker.getRegistrationKey(asSourceSelChannel)==null)
            throw new IllegalArgumentException("Source operations and sink operations must be registered on the same selector");
         
         return worker.register(asSinkSelChannel, ops, proc);
      }

      return null;
   }
   
   @Override
   public int validOps() {return asSourceSelChannel.validOps() & asSinkSelChannel.validOps();}
}
