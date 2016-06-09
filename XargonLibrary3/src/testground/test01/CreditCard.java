package testground.test01;

import it.xargon.util.*;

import java.math.BigInteger;
import java.util.*;
import java.awt.Point;

import testground.test02.*;

public class CreditCard implements ICreditCard {
   private ICCLogger ilogger=null;
   private ICCFactory ifactory=null;
   private String isubscriber=null;
   private long iplafond=0;
   private Identifier nextTransactionId=null;
   private HashMap<Identifier, String> transactionsLog=null;
   
   private void log(String logline) {if (ilogger!=null) ilogger.log(logline);}
   
   public CreditCard(ICCFactory factory, ICCLogger logger, String subscriber, long plafond) {
      ifactory=factory;
      ilogger=logger;
      isubscriber=subscriber;
      iplafond=plafond;
      transactionsLog=new HashMap<Identifier, String>();
      nextTransactionId=new Identifier(BigInteger.valueOf(12345678).toByteArray());
      log(">>> Creata nuova carta di credito a nome di " + subscriber + " con plafond di " + iplafond + " euro");
   }
   
   public boolean isDerivedFrom(ICCFactory factory) {
      Debug.stdout.println("ifactory = " + ifactory.toString() + " proxy=" + java.lang.reflect.Proxy.isProxyClass(ifactory.getClass()));
      Debug.stdout.println("factory = " + factory.toString() + " proxy=" + java.lang.reflect.Proxy.isProxyClass(ifactory.getClass()));
      return factory==ifactory;
   }
   
   public long getPlafond() {
      log(">>> Richiesto plafond disponibile");
      return iplafond;
   }

   public String getSubscriber() {
      log(">>> Richiesto nome del proprietario");
      return isubscriber;
   }

   public Map<Identifier, String> getTransactionsLog() {return transactionsLog;}

   public Identifier makeTransaction(String merchant, long expense) {
      log(">>> Richiesta transazione con " + merchant + " del valore di " + expense + " euro");
      Identifier trx=nextTransactionId;
      nextTransactionId=nextTransactionId.next();
      if (iplafond < expense) {
         log(">>> Transazione fallita, plafond insufficiente");
         transactionsLog.put(trx, "timestamp:" + System.currentTimeMillis() + " merchant:" + merchant + " value:" + expense + " --> FAILED");
         return null;
      }
      iplafond-=expense;
      log(">>> Transazione riuscita");
      transactionsLog.put(trx, "timestamp:" + System.currentTimeMillis() + " merchant:" + merchant + " value:" + expense + " --> OK");
      return trx;
   }
   
   public Point getGeoLocation() {return new Point(20,20);}
}
