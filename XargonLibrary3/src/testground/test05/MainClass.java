package testground.test05;

import it.xargon.util.*;
import it.xargon.xmp.*;
import it.xargon.xrpc.*;
import it.xargon.smartnet.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.*;

import testground.test06.ChatClient;

public class MainClass {
   public static BufferedReader stdin=Debug.stdin;
   public static PrintWriter stdout=Debug.stdout;
   public static PrintWriter stderr=new PrintWriter(System.err, true);

   //private String vsurl="http://127.0.0.1:8080/VirtualStreamServer/vss";
   //private String vsurl="http://free.hostingjava.it/-notoriux/vss";
   private ExecutorService threadPool=null;
   private SmartnetFactory s_fact=null;
   private XmpFactory x_fact=null;
   
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
   
   private ChatEvents chat_events_interceptor=new ChatEvents() {
      public void action(String nickname, String text) {stdout.println("ACTION # " + nickname + " " + text);}
      public void joins(String nickname) {stdout.println("JOINS  # " + nickname);}
      public void leaves(String nickname, String text) {stdout.println("LEAVES # " + nickname + "(" + text + ")");}
      public void message(String nickname, String text) {stdout.println("MSG    # " + nickname + "> " + text);}
      public void serverClosing() {}
      public void system(String text) {}
      
   };

      
   public void go(String[] args) throws Throwable {
      XmpServer xmpserv1=x_fact.newXmpServer(s_fact.newTcpServer(new InetSocketAddress(2601)));
      xmpserv1.register(xmp_status_interceptor);
      XRpcServer rpcserv1=XRpcFactory.getDefaultFactory().newServer(xmpserv1);
      
      ChatRoom chatroom=new ChatRoom("La Stanza delle Necessità");
      
      rpcserv1.publish("CHATROOM", chatroom, true);
      xmpserv1.start();
      
      String choice=null;
      boolean endLoop=false;
      boolean spyMode=false;
      do {
         stdout.println("Scegliere una delle seguenti opzioni e premere invio:");
         stdout.println("1 - Inviare un messaggio in broadcast a tutti i client connessi");
         stdout.println("2 - Iniziare/fermare il tracciamento dei messaggi pubblici");
         stdout.println("3 - Entrare nella chat in veste di client");
         stdout.println("x - Chiudere la stanza e arrestare i server");
         stdout.print(">");stdout.flush();
         
         choice=stdin.readLine();
         
         if (choice.equals("1")) {
            stdout.println("Inserire il messaggio da inviare in broadcast e premere invio");
            chatroom.sysMessage(stdin.readLine());
         } else if (choice.equals("2")) {
            if (spyMode) {
               stdout.println("*** TRACCIAMENTO DISATTIVATO");
               chatroom.unregister(chat_events_interceptor);
               spyMode=false;
            } else {
               stdout.println("*** TRACCIAMENTO ATTIVATO");
               chatroom.register(chat_events_interceptor);
               spyMode=true;
            }
         } else if (choice.equals("3")) {
            if (spyMode) {
               stdout.println("*** ABBANDONARE IL TRACCIAMENTO PRIMA DI ENTRARE ATTIVAMENTE IN CHAT");
            } else {
               stdout.println("Scegli un nickname:");
               String nickname=stdin.readLine();
               stdout.println("Scegli una tua descrizione:");
               String descr=stdin.readLine();
               stdout.println("Collegamento in corso...");
               ChatClient client=new ChatClient(nickname, descr);
               
               IChatSession session=chatroom.enter(client);
               client.processClient(session);
            }
         } else if (choice.equals("x")) {
            endLoop=true;
         } else {
            stdout.println("Scelta non corretta");
         }
      } while (!endLoop);
      
      chatroom.close();
      
      rpcserv1.unpublish("CHATROOM");      
      xmpserv1.shutdown();      
      xmpserv1.unregister(xmp_status_interceptor);      

      threadPool.shutdown();
   }
}