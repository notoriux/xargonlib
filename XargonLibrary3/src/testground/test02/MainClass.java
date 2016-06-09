package testground.test02;

import java.io.*;
import java.util.*;
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
      MainClass test=new MainClass();
      try {
         test.go(args);
      } catch (Throwable ex) {
         ex.printStackTrace();
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
   
   public void go(String[] args) throws Throwable {
      ExecutorService ithreadPool=Executors.newCachedThreadPool();
      SmartnetFactory s_fact=new SmartnetFactory(ithreadPool);
      XmpFactory x_fact=new XmpFactory(ithreadPool);

      XmpConnection conn=x_fact.newXmpConnection(s_fact.newTcpConnection(new java.net.InetSocketAddress("127.0.0.1",2800)));
      conn.registerProbe(xmp_message_probe);
      XRpcEndpoint oxremote=XRpcFactory.getDefaultFactory().newEndpoint(conn);
      conn.setRequestingEncryption(true);
      conn.start();
      
      if (conn.getIdentity()!=null) {
         stdout.println("Connessione protetta, il server si è identificato con:");
         Debug.dumpBytes(conn.getIdentity().getPublicRemoteKey(), stdout);
         stdout.println();
      }
      stdout.println("Connessione creata, premere invio per iniziare i test");stdin.readLine();
      
      HashMap<String, Class<?>[]> remInstances=oxremote.getAllRemoteInstances();
      
      for(String instname:remInstances.keySet()) {
         stdout.print("(");
         for(Class<?> iface:remInstances.get(instname)) stdout.print(iface.getName() + " ");
         stdout.print(") ");
         stdout.println(instname);
      }
      
      ICCFactory ccfactory=oxremote.getRemoteInstance(ICCFactory.class, "CCFACTORY-PREMIUM");
      
      ICCLogger logger=new ICCLogger() {public void log(String logline) {stdout.println(logline);}};

      ICreditCard myCreditCard=ccfactory.createCCMgr(logger, "Francesco Muccilli");
      stdout.println(myCreditCard.getPlafond());
      stdout.println(myCreditCard.getSubscriber());
      stdout.println(myCreditCard.makeTransaction("TiburCC", 2000));
      stdout.println(myCreditCard.makeTransaction("MediaWorld", 1200));
      stdout.println(myCreditCard.makeTransaction("ComputerCityHW", 700));
      stdout.println(myCreditCard.getPlafond());
      stdout.println(myCreditCard.getGeoLocation());
      
      Map<Identifier, String> trxlog=myCreditCard.getTransactionsLog();
      for(Identifier id:trxlog.keySet()) {
         stdout.println(id.toString() + " | " + trxlog.get(id));
      }

      myCreditCard=null;
      stdout.println("Premere invio per eseguire il garbage collection");stdin.readLine();
      System.gc();
      
      stdout.println("Test eseguito, premere invio per chiudere tutto");stdin.readLine();
      
      conn.close();
      conn.unregisterProbe(xmp_message_probe);
      ithreadPool.shutdown();
   }

}
