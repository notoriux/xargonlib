package testground.test06;

import it.xargon.util.Debug;

import java.io.*;

import testground.test05.*;

public class ChatClient implements IChatClient {
   public static BufferedReader stdin=Debug.stdin;
   public static PrintWriter stdout=Debug.stdout;

   private String inickname=null;
   private String idetails=null;
   private IChatSession isession=null;
   
   private IChatClient privsession=null;
   
   public ChatClient(String nick, String det) {
      inickname=nick;
      idetails=det;
   }
   
   public void processClient(IChatSession session) throws IOException {
      isession=session;
      stdout.println("---------------------------------------------");
      stdout.println("- Digitare una frase seguita da INVIO per parlare nella stanza");
      stdout.println("- Vengono riconosciuti i seguenti comandi:");
      stdout.println("/users : ottiene la lista di utenti in stanza");
      stdout.println("/me <azione> : manda un messaggio come azione");
      stdout.println("/details <utente> : ottiene le informazioni dettagliate che l'utente ha scelto di mostrare");
      stdout.println("/priv <utente> : messaggi e azioni andranno in privato all'utente scelto");
      stdout.println("/pub : esce dalla modalità privata e torna a parlare in pubblico");
      stdout.println("/test <utente>: come /details, test della capacità di rewire");
      stdout.println("/quit <messaggio>: abbandona la chat");
      stdout.println("---------------------------------------------");
      
      String text=null;
      boolean endloop=false;
      do {
         text=stdin.readLine();
         if (text.startsWith("/users")) {
            doUserlist();
         } else if (text.startsWith("/me")) {
            doAction(text.substring(3).trim());
         } else if (text.startsWith("/test")) {

         } else if (text.startsWith("/details")) {
            doDetails(text.substring(8).trim());
         } else if (text.startsWith("/priv")) {
            doPriv(text.substring(6).trim());
         } else if (text.startsWith("/pub")) {
            doPub();
         } else if (text.startsWith("/quit")) {
            doQuit(text.substring(5).trim()); 
            endloop=true;
         } else {
            doMessage(text);
         }
      } while (!endloop);
   }
   
   private void doUserlist() {
      stdout.println("* I seguenti utenti sono presenti nella chat room");
      String[] users=isession.getUsers();
      for(String user:users) stdout.println("* " + user);
   }
   
   private void doAction(String text) {
      if (text==null) return;
      if (privsession!=null) privsession.privAction(inickname, text);
      else isession.sendAction(text);
   }
      
   private void doDetails(String user) {
      if (user==null) stdout.println("* Specificare il nome dell'utente");
      IChatClient otheruser=isession.getUser(user);
      if (otheruser==null) {
         stdout.println("* L'utente " + user + " non è in chat");
         return;
      }
      
      stdout.println("* Dettagli pubblici dell'utente " + user);
      stdout.println("* " + otheruser.getDetails());
   }
   
   private void doPriv(String user) {
      if (user==null) stdout.println("* Specificare il nome dell'utente");
      IChatClient otheruser=isession.getUser(user);
      if (otheruser==null) {
         stdout.println("* L'utente " + user + " non è in chat");
         return;
      }

      privsession=otheruser;
      stdout.println("* Chat privata con " + user + " , usare /pub per tornare in pubblico");
   }
   
   private void doPub() {
      if (privsession==null) {
         stdout.println("* Modalità pubblica già attiva");
         return;
      }
      
      privsession=null;
      stdout.println("* Chat pubblica: tutti gli utenti potranno leggere i tuoi messaggi");
   }
   
   private void doMessage(String text) {
      if (text==null) return;
      if (privsession!=null) privsession.privMessage(inickname, text);
      else isession.sendMessage(text);
   }
   
   private void doQuit(String text) {
      if (text==null) isession.part("");
      isession.part(text);
   }
   
   public String getDetails() {return idetails;}

   public String getNickName() {return inickname;}

   public void privAction(String nickname, String text) {
      stdout.println("(priv) " + nickname + " " + text);
   }

   public void privMessage(String nickname, String text) {
      stdout.println("(priv) " + nickname + "> " + text);
   }

   public void system(String text) {
      stdout.println("*** " + text + " ***");
   }

   public void welcome(String roomname) {
      stdout.println("---------------------------------------------");
      stdout.println("Benvenuto in \"" + roomname + "\"");
   }

   public void action(String nickname, String text) {
      if (nickname.equals(inickname)) return;
      stdout.println("# " + nickname + " " + text);
   }

   public void joins(String nickname) {
      if (nickname.equals(inickname)) return;
      stdout.println("# " + nickname + " è entrato nella stanza");
   }

   public void leaves(String nickname, String text) {
      if (nickname.equals(inickname)) return;
      if ((text!=null) && (text.length()!=0)) stdout.println("# " + nickname + " è uscito dalla stanza (" + text + ")");
      else stdout.println("# " + nickname + " è uscito dalla stanza senza dire una parola");
   }

   public void message(String nickname, String text) {
      if (nickname.equals(inickname)) return;
      stdout.println(nickname + "> " + text);
   }

   public void serverClosing() {
      isession.part("*** QUIT ***");
   }
}
