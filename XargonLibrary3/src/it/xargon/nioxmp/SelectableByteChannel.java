package it.xargon.nioxmp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;

import it.xargon.channels.SelectionProcessor;
import it.xargon.channels.SelectorWorker;

public interface SelectableByteChannel extends ByteChannel {
   @Override
   public int read(ByteBuffer dst) throws IOException;

   @Override
   public int write(ByteBuffer src) throws IOException;

   @Override
   public boolean isOpen();

   @Override
   public void close() throws IOException;

   //Let's abuse a bit of facade pattern...
   public SelectableByteChannel configureBlocking(boolean block) throws IOException;
   
   public boolean isBlocking();
   
   public boolean isRegistered();

   public SelectionKey register(SelectorWorker worker, int ops, SelectionProcessor proc) throws ClosedChannelException;
   
   public int validOps();
}
