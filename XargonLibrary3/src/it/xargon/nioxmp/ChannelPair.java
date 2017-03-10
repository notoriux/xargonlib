package it.xargon.nioxmp;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

class ChannelPair {
   private ReadableByteChannel inputChannel=null;
   private WritableByteChannel outputChannel=null;
   
   public ChannelPair(ReadableByteChannel inputChannel, WritableByteChannel outputChannel) {
      this.inputChannel=inputChannel;
      this.outputChannel=outputChannel;
   }
   
   public ReadableByteChannel getInputChannel() {return inputChannel;}
   public WritableByteChannel getOutputChannel() {return outputChannel;}
}
