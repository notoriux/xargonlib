package testground.test05;

public interface IChatRoom {
   public String getName();
   public IChatSession enter(IChatClient client);
}
