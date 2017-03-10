package testground.niosockets.server;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import it.xargon.channels.*;
import static it.xargon.util.Debug.stdout;
import static it.xargon.util.Debug.stdin;

public class ServerMainClass {
   private SelectorWorker selWorker=null;
   
   public static void main(String[] args) throws Exception{
      new ServerMainClass().go();
   }
   
   public void go() throws Exception {
      selWorker=new SelectorWorker();
      
      stdout.println("Press ENTER to start worker:");
      stdin.readLine();
      selWorker.start();
      
      stdout.println("Started");
      ServerSocketChannel serverChannel=ServerSocketChannel.open();
      serverChannel.configureBlocking(false);
      serverChannel.bind(new InetSocketAddress(4567));
      SelectionKey listenerKey=selWorker.register(serverChannel, SelectionKey.OP_ACCEPT, this::processAccept);
      stdout.println("Server socket registered");
      
      stdout.println("Press ENTER to stop worker");
      stdin.readLine();
      listenerKey.cancel();
      serverChannel.close();
      selWorker.stop();
   }
   
   public Runnable processAccept(SelectorWorker worker, SelectionKey key) {
      stdout.println("[processAccept] Accepting...");
      ServerSocketChannel serverChannel=(ServerSocketChannel) key.channel();
      try {
         SocketChannel socketChannel=serverChannel.accept();
         stdout.println("[processAccept] Accepted from " + socketChannel.getRemoteAddress());
         socketChannel.configureBlocking(false);
         socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
         socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, false);
         selWorker.register(socketChannel, SelectionKey.OP_READ, this::processRead);
         //selWorker.register(socketChannel, SelectionKey.OP_WRITE, this::processWrite);
      } catch (IOException e) {
         e.printStackTrace();
      }
      return null;
   }
   
   public Runnable processRead(SelectorWorker worker, SelectionKey key) {
      stdout.println("[processRead] Reading...");
      SocketChannel socketChannel=(SocketChannel) key.channel();
      ByteBuffer buf=ByteBuffer.allocate(4096);
      try {
         int cnt=socketChannel.read(buf);
         if (cnt<0) {
            stdout.println("[processRead] Channel closed! unregistering...");
            key.cancel();
            socketChannel.close();
            return null;
         }
         stdout.println("[processRead] Got " + cnt + " bytes");
      } catch (IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   public Runnable processWrite(SelectorWorker worker, SelectionKey key) {
      stdout.println("[processWrite] Ready to write...");
      return null;
   }
}
