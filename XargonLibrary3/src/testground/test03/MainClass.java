package testground.test03;

import it.xargon.smartnet.SmartnetFactory;
import it.xargon.util.*;
import it.xargon.xmp.*;
import it.xargon.marshal.*;

import java.io.*;
import java.util.concurrent.*;

public class MainClass {
   public static BufferedReader stdin=Debug.stdin;
   public static PrintWriter stdout=Debug.stdout;
   public static PrintWriter stderr=Debug.stderr;
   
   public static void main(String[] args) {
      try {
         new MainClass().go(args);
         stdout.flush();
      } catch (Throwable tr) {
         tr.printStackTrace(stderr);
      }
   }

   public void go(String[] args) throws Throwable {
      ExecutorService ithreadPool=Executors.newCachedThreadPool();
      SmartnetFactory s_fact=new SmartnetFactory(ithreadPool);
      XmpFactory x_fact=new XmpFactory(ithreadPool);
      
      final DataBridge dbr=new DataBridge();
            
      final XmpConnection.Events conn_interceptor=new XmpConnection.Events.Adapter() {
         public void connected(XmpConnection conn) {
            stderr.println("connected!");
         }

         public void disconnected(XmpConnection conn) {
            stderr.println("disconnected!");
         }

         public void exception(XmpConnection conn, XmpException ex) {
            stderr.print("exception -> ");
            ex.printStackTrace(stderr);
         }

         public void processEvent(XmpConnection conn, byte[] contents) {
            stdout.println("*** EVENT ***");
            
            Debug.dumpBytesFormatted("", contents, stdout);

            Object obj =dbr.unmarshal(contents);
            
            stdout.println(obj.toString());
            
            stdout.println();
         }

         public byte[] processRequest(XmpConnection conn, byte[] request) {
            stdout.println("*** REQUEST ***");
            
            Debug.dumpBytesFormatted("", request, stdout);

            Object obj =dbr.unmarshal(request);
            
            stdout.println(obj.toString());
            
            stdout.println();
            
            return dbr.marshal("OK");
         }
      };
      
      final XmpServer.Events xmpServerEventsInterceptor=new XmpServer.Events() {
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
            conn.register(conn_interceptor);
            try {conn.start();} catch (IOException ignored) {}
         }

         public void removed(XmpConnection conn) {conn.unregister(conn_interceptor);}
         
      };
      
      XmpServer bxmpserv=x_fact.newXmpServer(s_fact.newTcpServer(new java.net.InetSocketAddress(2800)));
      bxmpserv.register(xmpServerEventsInterceptor);
      bxmpserv.start();
      
      stdout.println("Premere INVIO per arrestare il server");
      stdin.readLine();
      bxmpserv.shutdown();
      ithreadPool.shutdown();
   }
}
