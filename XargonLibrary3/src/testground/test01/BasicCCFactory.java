package testground.test01;

import it.xargon.util.Debug;
import testground.test02.*;

public class BasicCCFactory implements ICCFactory {
   public BasicCCFactory() {
      Debug.stdout.println("-------------- Credit Card Factory, Basic edition");      
   }
   
   public ICreditCard createCCMgr(ICCLogger logger, String subscriber) {
      return new CreditCard(this, logger, subscriber, 1200);
   }
}
