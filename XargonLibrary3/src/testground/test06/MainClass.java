package testground.test06;

import it.xargon.smartnet.*;
import it.xargon.util.*;
import it.xargon.xmp.*;
import it.xargon.xrpc.*;
import testground.test05.*;

import java.util.concurrent.*;
import java.io.*;
import java.net.*;

public class MainClass {
   public static BufferedReader stdin=Debug.stdin;
   public static PrintWriter stdout=Debug.stdout;
   public static PrintWriter stderr=Debug.stderr;

   private ExecutorService threadPool=null;
   private SmartnetFactory s_fact=null;
   private XmpFactory x_fact=null;
   
   private XmpConnection conn=null;
   private XRpcEndpoint oxremote=null;
   private IChatRoom chatroom=null;
   
   public static void main(String[] args) {
      try {
         new MainClass().go(args);
      } catch (Throwable ex) {
         ex.printStackTrace();
      }
   }
   
   public MainClass() {
      threadPool=Executors.newCachedThreadPool();
      s_fact=new SmartnetFactory(threadPool);
      x_fact=new XmpFactory(threadPool);
   }
   
   private XRpcEndpoint.Events xrpc_object_interceptor=new XRpcEndpoint.Events.Adapter() {
      public void remoteObjectUnavailable(XRpcEndpoint endPoint, Object remObj) {
         if (remObj==chatroom) {
            stdout.println("* Chat room chiusa forzatamente da remoto");
            try {conn.close();} catch (Exception ignored) {}
            oxremote.unregister(xrpc_object_interceptor);
            threadPool.shutdown();
            System.exit(0);
         }
      }
   };
   
   private XmpServer.Events xmp_status_interceptor=new XmpServer.Events.Adapter() {
      public void started(XmpServer serv) {
         stdout.println("Server " + serv.toString() + " avviato");         
      }

      public void stopped(XmpServer serv) {
         stdout.println("Server " + serv.toString() + " arrestato");
      }

      public void accepted(XmpConnection conn) {
         conn.registerProbe(XmpFactory.getDefaultMessageProbe());
      }

      public void removed(XmpConnection conn) {
         conn.unregisterProbe(XmpFactory.getDefaultMessageProbe());
      }
   };

   public void go(String[] args) throws Throwable {
      XmpConnection conn=null;

      stdout.println("Preparazione rewire server...");
      
      int rewirePort=2700;
      XmpServer xmpserv=null;
      XRpcServer rpcserv=null;
      
      do {
         try {
            xmpserv=x_fact.newXmpServer(s_fact.newTcpServer(new InetSocketAddress(rewirePort)));
            xmpserv.register(xmp_status_interceptor);
            xmpserv.start();
         } catch (IOException ex) {
            stdout.println("Porta " + rewirePort + " già occupata");
            xmpserv=null;
            rewirePort++;
         }
      } while ((xmpserv==null) && (rewirePort<2700));
      
      if (xmpserv==null) {
         stdout.println("Nessuna porta disponibile per il rewire");
      } else {
         rpcserv=XRpcFactory.getDefaultFactory().newServer(xmpserv);
         stdout.println("Rewire server avviato sulla porta " + rewirePort);
      }
      
      stdout.println("Connessione diretta in corso...");
      conn=x_fact.newXmpConnection(s_fact.newTcpConnection(new InetSocketAddress("localhost", 2601)));
      
      conn.registerProbe(XmpFactory.getDefaultMessageProbe());
      oxremote=XRpcFactory.getDefaultFactory().newEndpoint(conn);
      oxremote.register(xrpc_object_interceptor);
      conn.start();
      
      if (conn.getIdentity()!=null) {
         stdout.println("Connessione protetta, il server si è identificato con:");
         Debug.dumpBytes(conn.getIdentity().getPublicRemoteKey(), stdout);
         stdout.println();
      }
      
      chatroom=oxremote.getRemoteInstance(IChatRoom.class, "CHATROOM");
      
      stdout.println("La chatroom si chiama \"" + chatroom.getName() + "\"");
      stdout.println("Inserisci il tuo nickname:");
      String nickname=stdin.readLine();
      stdout.println("Inserisci una tua descrizione:");
      String descr=stdin.readLine();
      
      ChatClient client=new ChatClient(nickname, descr);
      IChatSession session=chatroom.enter(client);
      client.processClient(session);
      
      oxremote.unregister(xrpc_object_interceptor);
      if (rpcserv!=null) rpcserv.getXmpServer().shutdown();
      conn.close();
      conn.unregisterProbe(XmpFactory.getDefaultMessageProbe());
      threadPool.shutdown();
   }
}
