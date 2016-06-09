package testground.test05;

import java.util.*;

import it.xargon.events.*;
import it.xargon.xrpc.*;

public class ChatRoom extends EventsSourceImpl<ChatEvents> implements IChatRoom, XRpcEndpoint.Events {
   private String roomname=null;
   private HashMap<String, ChatSession> users=null;
   
   public void remoteObjectUnavailable(XRpcEndpoint endPoint, Object remObj) {
      String[] usernames=users.keySet().toArray(new String[users.size()]);
      for(String username:usernames) {
         ChatSession session=users.get(username);
         if (session.getClient()==remObj) {
            synchronized (users) {users.remove(username);}
            ChatRoom.this.unregister(session.getClient());
            ChatRoom.this.raiseEvent.leaves(username, "lost connection");
         }
      }
   }
   
   public void disconnected(XRpcEndpoint endpoint) {}

   public void remotePublished(XRpcEndpoint endpoint, String pubname) {}

   public void remoteUnpublished(XRpcEndpoint endpoint, String pubname) {}
         
   private class ChatSession implements IChatSession {
      private IChatClient cl=null;
      private String nickname=null;
      
      public ChatSession(IChatClient client) {
         cl=client;
         nickname=cl.getNickName();
      }
      
      public String[] getUsers() {return ChatRoom.this.users.keySet().toArray(new String[users.size()]);}
      
      public IChatClient getUser(String nickname) {
         ChatSession othersession=ChatRoom.this.users.get(nickname);
         if (othersession==null) return null;
         return othersession.getClient();
      }
      
      public IChatClient getClient() {return cl;}
      
      public String getNickname() {return nickname;}
      
      public String getRoomName() {return roomname;}

      public void sendMessage(String text) {ChatRoom.this.raiseEvent.message(nickname, text);}

      public void sendAction(String text) {ChatRoom.this.raiseEvent.action(nickname, text);}

      public void part(String text) {
         ChatRoom.this.unregister(cl);
         ChatRoom.this.raiseEvent.leaves(nickname, text);
         synchronized (users) {ChatRoom.this.users.remove(nickname);}
      }
   }
   
   public ChatRoom(String name) {
      roomname=name;
      users=new HashMap<String, ChatSession>();
   }
   
   public IChatSession enter(IChatClient client) {
      if (client==null) return null;
      if (users.containsKey(client.getNickName())) return null;
      ChatSession session=new ChatSession(client);
      synchronized (users) {users.put(session.getNickname(), session);}
      client.welcome(roomname);
      register(client);
      raiseEvent.joins(client.getNickName());
      return session;
   }

   public String getName() {return roomname;}
   
   public void close() {
      raiseEvent.serverClosing();
   }
   
   public void sysMessage(String text) {raiseEvent.system(text);}
}
