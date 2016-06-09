package it.xargon.util;

import java.util.*;

public class Parsers {   
   private enum PMSTATUS {
      WAIT_OPENBRACKET,
      WAIT_KEY_CHAR_EQUAL,
      WAIT_KEY_ANYCHAR,
      WAIT_VALUE_CHAR_COMMA_CLOSEBRACKET,
      WAIT_VALUE_ANYCHAR,
      WAIT_INPUTEND
   }
   
   private enum PLSTATUS {
      WAIT_OPENBRACKET,
      WAIT_CHAR_COMMA_CLOSEBRACKET,
      WAIT_ANYCHAR,
      WAIT_INPUTEND
   }
   
   private Parsers() {}
      
   public static HashMap<String,String> parseStringMap(String text) {
      char[] input=text.toCharArray();
      HashMap<String,String> result=new HashMap<String, String>();
      PMSTATUS status=PMSTATUS.WAIT_OPENBRACKET;
      
      StringBuilder buffer=new StringBuilder();
      String mapkey=null;
      char ch=0;
      
      for(int i=0;i<input.length;i++) {
         ch=input[i];
         switch (status) {
            case WAIT_OPENBRACKET:
               switch (ch) {
               case '{':
                  status=PMSTATUS.WAIT_KEY_CHAR_EQUAL;
                  break;
               default:
                  throw new IllegalArgumentException("Syntax error at index " + i + ": found \"" + ch + "\", expected \"{\"");
               }
               break;
            case WAIT_KEY_CHAR_EQUAL:
               switch (ch) {
                  case ' ':
                     //Ignora gli spazi all'inizio di una stringa
                     if (buffer.length()!=0) buffer.append(ch);
                     break;
                  case '=':
                     //Fine nome, inizio valore
                     mapkey=buffer.toString();
                     buffer.setLength(0);
                     status=PMSTATUS.WAIT_VALUE_CHAR_COMMA_CLOSEBRACKET;
                     break;
                  case '\'':
                     //Override
                     status=PMSTATUS.WAIT_KEY_ANYCHAR;
                     break;
                  case '{':
                  case '}':
                  case ',':
                     //Carattere riservato, non utilizzabile in questo momento
                     //a meno di non utilizzare l'override
                     throw new IllegalArgumentException("Syntax error at index " +i+ ": found a reserved character");
                  default:
                     buffer.append(ch);                  
               }
               break;
            case WAIT_KEY_ANYCHAR:
               buffer.append(ch);
               status=PMSTATUS.WAIT_KEY_CHAR_EQUAL;
               break;
            case WAIT_VALUE_CHAR_COMMA_CLOSEBRACKET:
               switch (ch) {
                  case '\'':
                     //Override
                     status=PMSTATUS.WAIT_VALUE_ANYCHAR;
                     break;
                  case ',':
                     //fine valore, inizio nome della prossima coppia
                     result.put(mapkey, buffer.toString());
                     buffer.setLength(0);
                     status=PMSTATUS.WAIT_KEY_CHAR_EQUAL;
                     break;
                  case '}':
                     //fine input
                     result.put(mapkey, buffer.toString());
                     buffer.setLength(0);
                     status=PMSTATUS.WAIT_INPUTEND;
                     break;
                  case '{':
                  case '=':
                     //caratteri riservati
                     throw new IllegalArgumentException("Syntax error at index "+i+": found a reserved character");
                  default:
                     buffer.append(ch);                  
               }
               break;
            case WAIT_VALUE_ANYCHAR:
               buffer.append(ch);
               status=PMSTATUS.WAIT_VALUE_CHAR_COMMA_CLOSEBRACKET;
               break;
            case WAIT_INPUTEND:
               throw new IllegalArgumentException("Syntax error: unexpected characters after end-of-text flag");
         }
      }
         
      if (status!=PMSTATUS.WAIT_INPUTEND) throw new IllegalArgumentException("Syntax error: unexpected end of text");
            
      return result;
   }
   
   public static HashMap<String,Integer> parseIntMap(String text) {
      char[] input=text.toCharArray();
      HashMap<String,Integer> result=new HashMap<String, Integer>();
      PMSTATUS status=PMSTATUS.WAIT_OPENBRACKET;
      
      StringBuilder buffer=new StringBuilder();
      String mapkey=null;
      char ch=0;
      
      for(int i=0;i<input.length;i++) {
         ch=input[i];
         switch (status) {
            case WAIT_OPENBRACKET:
               switch (ch) {
               case '{':
                  status=PMSTATUS.WAIT_KEY_CHAR_EQUAL;
                  break;
               default:
                  throw new IllegalArgumentException("Syntax error at index " + i + ": found \"" + ch + "\", expected \"{\"");
               }
               break;
            case WAIT_KEY_CHAR_EQUAL:
               switch (ch) {
                  case ' ':
                     //Ignora gli spazi all'inizio di una stringa
                     if (buffer.length()!=0) buffer.append(ch);
                     break;
                  case '=':
                     //Fine nome, inizio valore
                     mapkey=buffer.toString();
                     buffer.setLength(0);
                     status=PMSTATUS.WAIT_VALUE_CHAR_COMMA_CLOSEBRACKET;
                     break;
                  case '\'':
                     //Override
                     status=PMSTATUS.WAIT_KEY_ANYCHAR;
                     break;
                  case '{':
                  case '}':
                  case ',':
                     //Carattere riservato, non utilizzabile in questo momento
                     //a meno di non utilizzare l'override
                     throw new IllegalArgumentException("Syntax error at index "+i+": found a reserved character");
                  default:
                     buffer.append(ch);                  
               }
               break;
            case WAIT_KEY_ANYCHAR:
               buffer.append(ch);
               status=PMSTATUS.WAIT_KEY_CHAR_EQUAL;
               break;
            case WAIT_VALUE_CHAR_COMMA_CLOSEBRACKET:
               switch (ch) {
                  case '\'':
                     //Override
                     status=PMSTATUS.WAIT_VALUE_ANYCHAR;
                     break;
                  case ',':
                     //fine valore, inizio nome della prossima coppia
                     result.put(mapkey, Integer.parseInt(buffer.toString()));
                     buffer.setLength(0);
                     status=PMSTATUS.WAIT_KEY_CHAR_EQUAL;
                     break;
                  case '}':
                     //fine input
                     result.put(mapkey, Integer.parseInt(buffer.toString()));
                     buffer.setLength(0);
                     status=PMSTATUS.WAIT_INPUTEND;
                     break;
                  case '{':
                  case '=':
                     //caratteri riservati
                     throw new IllegalArgumentException("Syntax error at index "+i+": found a reserved character");
                  default:
                     buffer.append(ch);                  
               }
               break;
            case WAIT_VALUE_ANYCHAR:
               buffer.append(ch);
               status=PMSTATUS.WAIT_VALUE_CHAR_COMMA_CLOSEBRACKET;
               break;
            case WAIT_INPUTEND:
               throw new IllegalArgumentException("Syntax error: unexpected characters after end-of-text flag");
         }
      }
         
      if (status!=PMSTATUS.WAIT_INPUTEND) throw new IllegalArgumentException("Syntax error: unexpected end of text");
            
      return result;
   }   
   
   public static HashSet<String> parseStringSet(String text) {
      char[] input=text.toCharArray();

      HashSet<String> result=new HashSet<String>();
      PLSTATUS status=PLSTATUS.WAIT_OPENBRACKET;
      
      StringBuilder buffer=new StringBuilder();
      char ch=0;
      
      for(int i=0;i<input.length;i++) {
         ch=input[i];
         switch (status) {
            case WAIT_OPENBRACKET:
               switch (ch) {
                  case '[':
                     status=PLSTATUS.WAIT_CHAR_COMMA_CLOSEBRACKET;
                     break;
                  default:
                     throw new IllegalArgumentException("Syntax error at index " + i + ": found \"" + ch + "\", expected \"[\"");
               }
               break;
            case WAIT_CHAR_COMMA_CLOSEBRACKET:
               switch (ch) {
                  case ' ':
                     //Ignora gli spazi all'inizio di una stringa
                     if (buffer.length()!=0) buffer.append(ch);
                     break;
                  case ',':
                     //Fine stringa
                     result.add(buffer.toString());
                     buffer.setLength(0);
                     //non cambia lo stato
                     break;
                  case '\'':
                     //Override, aggiungere il prossimo carattere alla stringa incondizionatamente
                     status=PLSTATUS.WAIT_ANYCHAR;
                     break;
                  case '[':
                     //Carattere riservato, non utilizzabile in questo momento
                     //a meno di non utilizzare l'override
                     throw new IllegalArgumentException("Syntax error at index "+i+": found a reserved character");
                  case ']':
                     //Fine input, se ci sono altri caratteri è un errore di sintassi
                     result.add(buffer.toString());
                     buffer.setLength(0);
                     status=PLSTATUS.WAIT_INPUTEND;
                     break;
                  default:
                     buffer.append(ch);
               }
               break;
            case WAIT_ANYCHAR:
               buffer.append(ch);
               status=PLSTATUS.WAIT_CHAR_COMMA_CLOSEBRACKET;
               break;
            case WAIT_INPUTEND:
               throw new IllegalArgumentException("Syntax error: unexpected characters after end-of-text flag");
         }
      }
            
      if (status!=PLSTATUS.WAIT_INPUTEND) throw new IllegalArgumentException("Syntax error: unexpected end of text");

      return result;
   }

   public static HashSet<Integer> parseIntSet(String text) {
      char[] input=text.toCharArray();

      HashSet<Integer> result=new HashSet<Integer>();
      PLSTATUS status=PLSTATUS.WAIT_OPENBRACKET;
      
      StringBuilder buffer=new StringBuilder();
      char ch=0;
      
      for(int i=0;i<input.length;i++) {
         ch=input[i];
         switch (status) {
            case WAIT_OPENBRACKET:
               switch (ch) {
                  case '[':
                     status=PLSTATUS.WAIT_CHAR_COMMA_CLOSEBRACKET;
                     break;
                  default:
                     throw new IllegalArgumentException("Syntax error at index " + i + ": found \"" + ch + "\", expected \"[\"");
               }
               break;
            case WAIT_CHAR_COMMA_CLOSEBRACKET:
               switch (ch) {
                  case ' ':
                     //Ignora gli spazi all'inizio di una stringa
                     if (buffer.length()!=0) buffer.append(ch);
                     break;
                  case ',':
                     //Fine stringa
                     result.add(Integer.parseInt(buffer.toString()));
                     buffer.setLength(0);
                     //non cambia lo stato
                     break;
                  case '\'':
                     //Override, aggiungere il prossimo carattere alla stringa incondizionatamente
                     status=PLSTATUS.WAIT_ANYCHAR;
                     break;
                  case '[':
                     //Carattere riservato, non utilizzabile in questo momento
                     //a meno di non utilizzare l'override
                     throw new IllegalArgumentException("Syntax error at index "+i+": found a reserved character");
                  case ']':
                     //Fine input, se ci sono altri caratteri è un errore di sintassi
                     result.add(Integer.parseInt(buffer.toString()));
                     buffer.setLength(0);
                     status=PLSTATUS.WAIT_INPUTEND;
                     break;
                  default:
                     buffer.append(ch);
               }
               break;
            case WAIT_ANYCHAR:
               buffer.append(ch);
               status=PLSTATUS.WAIT_CHAR_COMMA_CLOSEBRACKET;
               break;
            case WAIT_INPUTEND:
               throw new IllegalArgumentException("Syntax error: unexpected characters after end-of-text flag");
         }
      }
            
      if (status!=PLSTATUS.WAIT_INPUTEND) throw new IllegalArgumentException("Syntax error: unexpected end of text");

      return result;
   }
}
