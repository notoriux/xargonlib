package testground.selectorsuspend;

import it.xargon.channels.SelectorWorker;
import it.xargon.util.Debug;

public class TestSelectorSuspend {
   public static void main(String[] args) throws Exception {
      SelectorWorker sw=new SelectorWorker();
      
      Debug.stdout.println("Press ENTER to start selector worker");
      Debug.stdin.readLine();
      sw.start();
      Debug.stdout.println("Press ENTER to suspend");
      Debug.stdin.readLine();
      sw.suspend();
      Debug.stdout.println("Press ENTER to resume");
      Debug.stdin.readLine();
      sw.resume();
      Debug.stdout.println("Press ENTER to stop");
      Debug.stdin.readLine();
      sw.stop();
      
      Debug.stdout.println("All done.");
   }
}
