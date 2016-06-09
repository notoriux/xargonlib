package testground.test04;

import it.xargon.smartnet.SmartnetFactory;
import it.xargon.util.*;
import it.xargon.xmp.*;
import it.xargon.marshal.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.*;

public class MainClass {
   public static BufferedReader stdin=Debug.stdin;
   public static PrintWriter stdout=Debug.stdout;
   public static PrintWriter stderr=Debug.stderr;

   public static void main(String[] args) {
      try {
         new MainClass().go(args);
      } catch (Throwable tr) {
         tr.printStackTrace(stderr);
      }
   }
   
   private XmpConnection.Events conn_interceptor=new XmpConnection.Events.Adapter() {
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

      public void processEvent(XmpConnection conn, byte[] contents) {}

      public byte[] processRequest(XmpConnection conn, byte[] request) {return request;}
   };
      
   public void go(String[] args) throws Exception {
      ExecutorService ithreadPool=null;//Executors.newCachedThreadPool();
      SmartnetFactory nfactory=new SmartnetFactory(ithreadPool);
      XmpFactory xfactory=new XmpFactory(ithreadPool);
      
      XmpConnection conn=xfactory.newXmpConnection(nfactory.newTcpConnection(new InetSocketAddress("127.0.0.1", 2800)));
      conn.register(conn_interceptor);
      conn.start();
      
      stdout.println("Connessione creata, premere invio per iniziare i test");
      stdin.readLine();
      
      DataBridge dbr=new DataBridge();
      
      conn.sendRequest(dbr.marshal(new Boolean(true)));
      conn.sendRequest(dbr.marshal(new Byte((byte)10)));
      conn.sendRequest(dbr.marshal(new Character('c')));
      conn.sendRequest(dbr.marshal(new Double(120d)));
      conn.sendRequest(dbr.marshal(new Float(400f)));
      conn.sendRequest(dbr.marshal(new Identifier().next().next()));
      conn.sendRequest(dbr.marshal(new Integer(300)));
      conn.sendRequest(dbr.marshal(new Long(900l)));
      conn.sendRequest(dbr.marshal(new Short((short)200)));
      conn.sendRequest(dbr.marshal(false));
      conn.sendRequest(dbr.marshal((byte)45));
      conn.sendRequest(dbr.marshal(new byte[] {10,20,30}));
      conn.sendRequest(dbr.marshal('p'));
      conn.sendRequest(dbr.marshal(125d));
      conn.sendRequest(dbr.marshal(405f));
      conn.sendRequest(dbr.marshal(305));
      conn.sendRequest(dbr.marshal(905l));
      conn.sendRequest(dbr.marshal((short)205));
      conn.sendRequest(dbr.marshal("Prova di stringa"));
      
      stdout.println("Test conclusi, premere invio per chiudere tutto");
      stdin.readLine();
      
      conn.close();
      //ithreadPool.shutdown();
   }
}
