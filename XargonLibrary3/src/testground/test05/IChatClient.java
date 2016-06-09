package testground.test05;

public interface IChatClient extends ChatEvents {
   public String getNickName();
   public String getDetails();
   public void privMessage(String nickname, String text);
   public void privAction(String nickname, String text);
   public void welcome(String roomname);
}
