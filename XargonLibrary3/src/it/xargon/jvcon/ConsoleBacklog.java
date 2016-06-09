package it.xargon.jvcon;

import java.awt.*;
import javax.swing.text.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.*;

class ConsoleBacklog {
   private StyleContext backlogStyles=null;
   private DefaultStyledDocument backlogDocument=null;
   private AtomicLong idGenerator=null;
   private ArrayList<Style> styles=null;
   
   public ConsoleBacklog() {
      backlogStyles=new StyleContext();
      styles=new ArrayList<Style>();
      idGenerator=new AtomicLong(0);
      backlogDocument=new DefaultStyledDocument(backlogStyles);
   }

   public DefaultStyledDocument getDocument() {return backlogDocument;}
   
   public Style getDefaultStyle() {return backlogStyles.getStyle(StyleContext.DEFAULT_STYLE);}

   public Style newStyle(String name, Style parentStyle) {
      Style parent=(parentStyle!=null)?parentStyle:getDefaultStyle();
      Style result=backlogStyles.addStyle(name, parent);
      styles.add(result);
      return result;
   }
      
   public StyleContext getAllStyles() {return backlogStyles;}

   public Style newStyle(Style parentStyle) {
      long vl=idGenerator.incrementAndGet();
      String name="_style_" + Long.toString(vl);
      return newStyle(name, parentStyle);
   }
   
   public Style addStyle(Style parentStyle, String fontFace, int fontSize, boolean bold, boolean italic, Color foreground, Color background) {
      Style result=newStyle(parentStyle);
      
      StyleConstants.setFontFamily(result, fontFace);
      StyleConstants.setFontSize(result, fontSize);
      StyleConstants.setBold(result, bold);
      StyleConstants.setItalic(result, italic);
      StyleConstants.setForeground(result, foreground);
      StyleConstants.setBackground(result, background);
      StyleConstants.setAlignment(result, StyleConstants.ALIGN_LEFT);
      
      return result;      
   }
   
   public void removeStyle(Style style) {
      if (!styles.contains(style)) throw new IllegalArgumentException("Specified style doesn't belong to this document");
      backlogStyles.removeStyle(style.getName());
   }
   
   public void insertText(String text, Style style) {
      try {backlogDocument.insertString(backlogDocument.getLength(), text, style);}
      catch (BadLocationException ignore) {}
   }
   
   public void clear() {
      try {backlogDocument.remove(0, backlogDocument.getLength());}
      catch (BadLocationException ignore) {}
   }
}
