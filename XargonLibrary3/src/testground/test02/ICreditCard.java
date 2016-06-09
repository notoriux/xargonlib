package testground.test02;

import java.util.*;
import it.xargon.util.*;
import java.awt.Point;

public interface ICreditCard {
   public String getSubscriber();
   public long getPlafond();
   public Map<Identifier, String> getTransactionsLog();
   public Identifier makeTransaction(String merchant, long expense);
   public boolean isDerivedFrom(ICCFactory factory);
   public Point getGeoLocation();
}
