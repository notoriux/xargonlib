package testground.test05;

public interface IChatSession {
   public void sendMessage(String text);
   public void sendAction(String text);
   public void part(String text);
   public String getRoomName();
   public String[] getUsers();
   public IChatClient getUser(String nickname);
}
