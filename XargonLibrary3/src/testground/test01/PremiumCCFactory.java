package testground.test01;

import it.xargon.util.Debug;
import testground.test02.*;

public class PremiumCCFactory implements ICCFactory {
   public PremiumCCFactory() {
      Debug.stdout.println("-------------- Credit Card Factory, Premium edition");      
   }
   
   public ICreditCard createCCMgr(ICCLogger logger, String subscriber) {
      return new CreditCard(this, logger, subscriber,  4000);
   }
}
