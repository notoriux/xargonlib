package testground.test01;

import java.io.*;
import java.util.concurrent.*;

import it.xargon.smartnet.SmartnetFactory;
import it.xargon.util.*;
import it.xargon.xmp.*;
import it.xargon.xrpc.*;

public class MainClass {
   public static BufferedReader stdin=Debug.stdin;
   public static PrintWriter stdout=Debug.stdout;
   public static PrintWriter stderr=Debug.stderr;

   public static void main(String[] args) {
      try {
         new MainClass().go(args);
      } catch (Exception ex) {
         String text=Debug.exceptionToString(ex);
         stdout.println(text);
      }
   }

   private XmpMessageProbe.Events xmp_message_probe=new XmpMessageProbe.Events() {      
      @Override public void discardedAnswer(XmpConnection conn, XmpMessage discarded) {
         discarded.printout("L.... ", stderr);
         stderr.println();   
      }

      @Override public void eventSent(XmpConnection conn, XmpMessage outgoing) {
         outgoing.printout("L-->R ", stderr);
         stderr.println();                  
      }

      @Override public void requestSent(XmpConnection conn, XmpMessage outgoing) {
         outgoing.printout("L-->R ", stderr);
         stderr.println();                  
      }
      
      @Override public void answerReceived(XmpConnection conn, XmpMessage incoming) {
         incoming.printout("L<--R ", stderr);
         stderr.println();                           
      }
      
      @Override public void preProcessMessage(XmpConnection conn, XmpMessage incoming) {
         incoming.printout("R-->L ", stderr);
         stderr.println();         
      }
      
      @Override public void postProcessMessage(XmpConnection conn, XmpMessage incoming, XmpMessage outgoing) {
         if (outgoing!=null) {
            outgoing.printout("R<--L ", stderr);
            stderr.println();
         }
      }      
   };
   
   public void go(String[] args) throws Exception {      
      ExecutorService ithreadPool=Executors.newCachedThreadPool();
      SmartnetFactory s_fact=new SmartnetFactory(ithreadPool);
      XmpFactory x_fact=new XmpFactory(ithreadPool);
      
      XmpServer.Events xmpServerEventsInterceptor=new XmpServer.Events() {
         public void exception(XmpException ex, XmpServer serv) {
            stdout.println("*** Eccezione ***");
            ex.printStackTrace(stdout);
         }

         public void started(XmpServer serv) {
            stdout.println("Server avviato");
            stdout.println("In attesa di una connessione entrante...");            
         }

         public void stopped(XmpServer serv) {
            stdout.println("Server arrestato");            
         }

         public void accepted(XmpConnection conn) {
            conn.registerProbe(xmp_message_probe);
         }

         public void removed(XmpConnection conn) {
            conn.unregisterProbe(xmp_message_probe);
         }
         
      };
      
      XRpcServer.Events xrpcServerEventsinterceptor=new XRpcServer.Events() {
         public void published(XRpcServer server, String pubName) {
            stdout.println("Pubblicato un nuovo oggetto sul server: " + pubName);
         }
         
         public void unpublished(XRpcServer server, String pubName) {
            stdout.println("L'oggetto " + pubName + " non è più pubblicato");
         }

         public void accepted(XRpcServer server, XRpcEndpoint endpoint) {
            stdout.println("Connessione in arrivo: " + endpoint.getXmpConnection().toString());            
         }
         
         public void removed(XRpcServer server, XRpcEndpoint endpoint) {
            stdout.println("Connessione terminata");
         }
         public void exception(XRpcServer server, Exception ex) {
            stdout.println("Eccezione!!!");
            ex.printStackTrace(stdout);
         }
      };
      
      XmpServer xmpserv=x_fact.newXmpServer(s_fact.newTcpServer(new java.net.InetSocketAddress(2800)));
      xmpserv.register(xmpServerEventsInterceptor);
      XRpcServer rpcserv=XRpcFactory.getDefaultFactory().newServer(xmpserv);
      rpcserv.register(xrpcServerEventsinterceptor);
      rpcserv.publish("CCFACTORY-BASIC", new BasicCCFactory());
      rpcserv.publish("CCFACTORY-PREMIUM", new PremiumCCFactory());
      xmpserv.start();
      
      stdout.println("Premere INVIO per arrestare il server");
      stdin.readLine();
      xmpserv.shutdown();
      ithreadPool.shutdown();
   }
}
