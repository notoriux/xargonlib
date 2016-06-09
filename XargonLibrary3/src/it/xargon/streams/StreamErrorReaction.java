package it.xargon.streams;

import java.io.*;

public class StreamErrorReaction {
   private boolean mayStop=true;
   private IOException ioex=null;
   public StreamErrorReaction(IOException cause) {ioex=cause;}
   public IOException getException() {return ioex;}
   public void dontStop() {mayStop=false;}
   public boolean stopRequested() {return mayStop;}
}