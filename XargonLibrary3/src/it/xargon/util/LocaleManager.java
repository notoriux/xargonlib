package it.xargon.util;

import java.text.MessageFormat;
import java.util.*;

public class LocaleManager {
   private Map<Enum<?>, MessageFormat> textmessages=null;
   private String packageName=null;
   
   public LocaleManager(Class<? extends Enum<?>> messageIds) {
      textmessages=Collections.synchronizedMap(new HashMap<Enum<?>, MessageFormat>());
      packageName=messageIds.getPackage().getName();

      ResourceBundle mbundle=null;
      try {
         mbundle=ResourceBundle.getBundle(packageName + ".messages",Locale.getDefault());
      } catch (MissingResourceException ex) {
         try {
            mbundle=ResourceBundle.getBundle(packageName + ".messages",Locale.ENGLISH);
         } catch (MissingResourceException ex2) {
            mbundle=null;
         }
      }
      
      for(Enum<?> msgen:messageIds.getEnumConstants()) {
         MessageFormat mformat=null;
         if (mbundle==null) mformat=new MessageFormat("[?RES?" + packageName + "." + msgen.name() + "]");
         else {
            try {
               mformat=new MessageFormat(mbundle.getString(msgen.name()));
            } catch (MissingResourceException ex) {
               mformat=new MessageFormat("[?LNG?" + packageName + "." + msgen.name() + "]");
            }
         }
         textmessages.put(msgen, mformat);
      }      
   }
   
   public String get(Enum<?> msgid, Object... arguments) {
      String result=null;
      MessageFormat formatter=textmessages.get(msgid);
      if (formatter!=null) {
         result=formatter.format(arguments, new StringBuffer() ,null).toString();
      } else {
         result="[?MSG?" + packageName + "." + msgid.name();
         for(Object arg:arguments) result+="," + arg.toString();
         result+="]";
      }
      return result;
   }
}
