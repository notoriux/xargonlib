package testground.niomarshal;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import it.xargon.niomarshal.*;
import it.xargon.util.ByteBufferAllocator;
import it.xargon.util.Debug;

public class TestMarshal {
   private final static List<String> VALID=Arrays.asList(new String[] {
         "0","1","2","3","4","5","6","7","8","9",
         "A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z",
         "a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"});
   
   public static void main(String[] args) throws Exception {
      DataBridge dbridge=new DataBridge(ByteBufferAllocator.INDIRECT);
      
      ArrayList<ArrayList<String>> bigObject=new ArrayList<>();
      
      for(int i=0;i<1;i++) {
         ArrayList<String> element=new ArrayList<>();
         for(int j=0;j<2;j++) element.add(generateRandomString(10));
         bigObject.add(element);
      }
      
      Debug.startTimer();
      ByteBuffer buf=dbridge.marshal(bigObject);
      Debug.stopTimer("Encoded in %d nanos%n");
      
      ByteBuffer buf2=buf.duplicate();
      
      Debug.startTimer();
      Object received=dbridge.unmarshal(buf);
      Debug.stopTimer("Decoded in %d nanos%n");
      
      System.out.println(received.toString() + " (" + received.getClass().getName() + ")");
      
      Parser dbParser=new Parser(ByteBufferAllocator.INDIRECT);
      
      ByteBuffer result[]=dbParser.feed(buf2.duplicate(), buf2.duplicate(), buf2.duplicate());
      
      System.out.println(result.length);
   }
   
   private static String generateRandomString(int size) {
      return ThreadLocalRandom.current()
         .ints(size, 0, VALID.size())
         .mapToObj(VALID::get)
         .collect(Collectors.joining());
   }
}
