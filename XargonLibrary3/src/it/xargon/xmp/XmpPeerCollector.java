package it.xargon.xmp;

import it.xargon.util.*;
import java.io.IOException;

class XmpPeerCollector extends XmpServer.Events.Adapter {
   private XmpConnection iconn=null;
   private BooleanLatch lock=null;
   
   public XmpPeerCollector() {lock=new BooleanLatch();}
   public void accepted(XmpConnection conn) {iconn=conn;lock.open();}
   
   public void exception(XmpException ex, XmpServer serv) {
      try {serv.shutdown();} catch (IOException e) {}
      lock.open();
   }
         
   public XmpConnection getConnection(long timeout) {
      try {
         if (timeout==0) lock.await();
         else lock.await(timeout);
      } catch (InterruptedException ex) {}
      return iconn;
   }
}
