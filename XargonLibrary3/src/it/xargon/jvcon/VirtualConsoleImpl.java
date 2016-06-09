package it.xargon.jvcon;

import it.xargon.util.*;
import it.xargon.streams.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.nio.*;
import java.nio.charset.*;

class VirtualConsoleImpl extends JFrame implements VirtualConsole {
   private JButton buttonClearBuffer;
   private JToggleButton buttonScrollLock;
   private JButton buttonEnter;
   private JToolBar toolBar;
   private JPanel panelContents;
   private JTextField textInput;
   private JLabel promptLabel;
   private JTextPane textOutput;
   private JScrollPane textScroll;
   
   private ExecutorService functionRunner=null;
   
   private Macro cbkClose=null;
   private ConsoleBacklog backlog=null;
   
   private StreamFifo userToConsoleFifo=null;
   private boolean doStdinEcho=false;
   private PrintStream stdinEcho=null;
   private volatile boolean scrollLock=false;
   
   private FifoInputStream consoleStdin=null;
   private PrintStream consoleStdout=null;
   private PrintStream consoleStderr=null;
   private String consoleCharSet=null;
   private Color consoleBackground=null;
   
   private BooleanLatch pauseBeforeClose=null;
   
   private LinkedList<PrintStream> consoleStreams=null;
      
   public VirtualConsoleImpl(String charset, int background, String title, String prompt, Macro closeCallBack) {
      super();
      setIconImage(Resources.getImage("frame-icon.png"));
      
      addWindowListener(new WindowAdapter() {
         public void windowClosing(final WindowEvent e) {
            if (pauseBeforeClose!=null) {
               pauseBeforeClose.open();
            } else {
               close(false);
            }
         }
      });
      
      
      functionRunner=Executors.newFixedThreadPool(1);
      
      consoleCharSet=charset;
      consoleBackground=new Color(background);
      userToConsoleFifo=new StreamFifo(8192);
      consoleStdin=new FifoInputStream(userToConsoleFifo);
      
      backlog=new ConsoleBacklog();
      consoleStreams=new LinkedList<PrintStream>();

      Font df=UIManager.getLookAndFeelDefaults().getFont("TextPane.font");
      
      consoleStdout=getUserout(df.getFamily(), df.getSize(), Color.BLACK.getRGB(), Color.WHITE.getRGB(), false, false);
      consoleStderr=getUserout(df.getFamily(), df.getSize(), Color.WHITE.getRGB(), Color.RED.getRGB(), false, false);
      
      Style userinputStyle=backlog.addStyle(null, df.getFamily(), df.getSize(), false, true, Color.BLUE, Color.WHITE);

      stdinEcho=createConsoleStream(userinputStyle);
            
      getPromptLabel().setText((prompt==null)?"":prompt);
      cbkClose=closeCallBack;
      setTitle(title);
      getContentPane().add(getPanelContents(), BorderLayout.CENTER);
      getContentPane().add(getToolBar(), BorderLayout.NORTH);
      getTextInput().requestFocusInWindow();
   }
   
   private class MacroAction extends AbstractAction {
      private Macro icbk=null;
      private VirtualConsole icon=null;
      private ExecutorService irun=null;
      
      public MacroAction(Macro cbk, VirtualConsole con, ExecutorService runner) {
         super(cbk.getName());
         icbk=cbk;
         icon=con;
         irun=runner;
      }
      
      public void actionPerformed(ActionEvent e) {
         Runnable task=new Runnable() {public void run() {icbk.run(icon);}};
         if (irun==null) {
            new Thread(task).start();
         } else {
            irun.submit(task);
         }
      }
   }
   
   public PrintStream getUserout(String fontface, int size, int foreground, int background, boolean bold, boolean italic) {
      if (isClosed()) return null;
      Style outStyle=backlog.addStyle(null, fontface, size, bold, italic, new Color(foreground), new Color(background));
      return createConsoleStream(outStyle);
   }
   
   private PrintStream createConsoleStream(Style outStyle) {
      DirectConsoleOutputStream consoleStream=new DirectConsoleOutputStream(outStyle);
      PrintStream pstream=new PrintStream(consoleStream);
      consoleStreams.add(pstream);
      return pstream;      
   }
   
   private class DirectConsoleOutputStream extends OutputStream {
      private boolean closed=false;
      private CharBuffer oneCharBuffer=CharBuffer.allocate(1);
      private StringBuilder sbuffer=null;
      private Style istyle=null;
      private CharsetDecoder decoder=null;
      private byte[] oneByteArray=new byte[1];
      private byte[] emptyByteArray=new byte[0];
            
      public DirectConsoleOutputStream(Style style) {
         istyle=style;
         sbuffer=new StringBuilder();
         decoder=Charset.forName(consoleCharSet).newDecoder();
      }
      
      public void close() throws IOException {
         write(emptyByteArray,0,0);
         closed=true;
         VirtualConsoleImpl.this.backlog.removeStyle(istyle);
         VirtualConsoleImpl.this.consoleStreams.remove(DirectConsoleOutputStream.this);
      }
      
      public void flush() throws IOException {
         flush(false);
      }
      
      private void flush(boolean newline) throws IOException {
         if (closed) throw new IOException("Stream closed");
         synchronized (VirtualConsoleImpl.this.backlog) {
            String text=sbuffer.toString();
            sbuffer.setLength(0);
            VirtualConsoleImpl.this.insertText(text, istyle);
         }
      }

      public void write(int b) throws IOException {
         if (closed) throw new IOException("Stream closed");
         oneByteArray[0]=Bitwise.asByte(b);
         write(oneByteArray,0,1);
      }
      
      public void write(byte[] b, int off, int len) throws IOException {
         if (closed) throw new IOException("Stream closed");
         sbuffer.setLength(0);
         ByteBuffer bb=ByteBuffer.wrap(b, off, len);
         CoderResult cr=null;
         do {
            cr=decoder.decode(bb, oneCharBuffer, b.length==0);
            if (cr.isError()) throw new IOException(cr.toString());
            oneCharBuffer.flip();
            if (oneCharBuffer.hasRemaining()) {
               char scan=oneCharBuffer.get();
               switch (scan) {
                  case '\r':break; //ignora i caratteri di ritorno carrello (non rappresentabili)
                  case '\n': //newline: output su console e svuotare buffer
                     sbuffer.append(scan);
                     flush(true);
                     break;
                  default: //inserisce il carattere nel buffer
                     sbuffer.append(scan);
                     break;
               }
            }
            
            oneCharBuffer.clear();
         } while (!cr.equals(CoderResult.UNDERFLOW));
         
         //Alla fine svuota comunque il buffer stringa per consentire l'output di linee
         //che non terminano con '\n'
         
         flush(false);
      }
   }

   private Action enterTextAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
         String text=getTextInput().getText() + "\n";
         getTextInput().setText("");
         getTextInput().requestFocusInWindow();
         try {
            pipeToStdin(text.getBytes(consoleCharSet));
         } catch (IOException ignored) {}
      }
   };
   
   private JPopupMenu consoleMenu=null;
   private JMenuItem menuSelectAll=null;
   private JMenuItem menuCopySelected=null;
   private JMenuItem menuPaste=null;
   
   private JTextComponent popupActionTarget=null;
   
   private JPopupMenu getConsoleMenu() {
      if (consoleMenu==null) {
         consoleMenu=new JPopupMenu();
         consoleMenu.add(getMenuSelectAll());
         consoleMenu.add(getMenuCopySelected());
         consoleMenu.add(getMenuPaste());
      }
      return consoleMenu;
   }
   
   private JMenuItem getMenuSelectAll() {
      if (menuSelectAll==null) {
         menuSelectAll=new JMenuItem(new AbstractAction("Select all") {
            public void actionPerformed(ActionEvent e) {popupActionTarget.selectAll();}
         });
      }
      return menuSelectAll;
   }
   
   private JMenuItem getMenuCopySelected() {
      if (menuCopySelected==null) {
         menuCopySelected=new JMenuItem(new AbstractAction("Copy selection") {
            public void actionPerformed(ActionEvent e) {popupActionTarget.copy();}
         });
      }
      return menuCopySelected;      
   }
   
   private JMenuItem getMenuPaste() {
      if (menuPaste==null) {
         menuPaste=new JMenuItem(new AbstractAction("Paste") {
            public void actionPerformed(ActionEvent e) {popupActionTarget.paste();}
         });
      }
      return menuPaste;      
   }
   
   private MouseInputAdapter mouseListener = new MouseInputAdapter() {      
      public void mousePressed(MouseEvent e) {
         maybeShowPopup(e);
      }

      public void mouseReleased(MouseEvent e) {
         maybeShowPopup(e);
      }
      
      private void maybeShowPopup(MouseEvent e) {
         if (e.isPopupTrigger()) {
            Component comp=e.getComponent();
            
            if (comp!=getTextInput() && comp!=getTextOutput()) return;
            
            popupActionTarget=(JTextComponent)comp;
            
            String curtext=popupActionTarget.getText();
            if ((curtext==null) || (curtext.length()==0) || (popupActionTarget==getTextInput() && pauseBeforeClose!=null))
               getMenuSelectAll().setEnabled(false);
            else
               getMenuSelectAll().setEnabled(true);
            
            String seltext=popupActionTarget.getSelectedText();
            if ((seltext==null) || (seltext.length()==0) || (popupActionTarget==getTextInput() && pauseBeforeClose!=null))
               getMenuCopySelected().setEnabled(false);
            else
               getMenuCopySelected().setEnabled(true);
            
            if (comp==getTextInput() && pauseBeforeClose==null && Toolkit.getDefaultToolkit().getSystemClipboard().isDataFlavorAvailable(DataFlavor.stringFlavor))
               getMenuPaste().setEnabled(true);
            else
               getMenuPaste().setEnabled(false);
            
            getConsoleMenu().show(popupActionTarget, e.getX(), e.getY());
         }
     }
   };
   
   private void insertText(final String text, final Style style) {
      Tools.ensureSwingThread(new Runnable() {
         public void run() {
            backlog.insertText(text, style);
         }
      });
   }
   
   private JScrollPane getTextScroll() {
      if (textScroll == null) {
         textScroll = new JScrollPane();
         textScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
         textScroll.setViewportView(getTextOutput());
      }
      return textScroll;
   }

   public void close(final boolean pause) {
      if (isClosed()) return;
      if (cbkClose!=null) {
         try {
            Future<?> cbkCloseFuture=functionRunner.submit(new Runnable() {
               public void run() {cbkClose.run(VirtualConsoleImpl.this);}
            });
            cbkCloseFuture.get();
         } catch (Exception ex) {
            ex.printStackTrace(System.err);
         }
      }
      functionRunner.shutdown();
      userToConsoleFifo.close();
      try {consoleStdin.close();} catch (IOException ignored) {}
      for(PrintStream ps:consoleStreams) {ps.flush();ps.close();}

      if (pause) {
         pauseBeforeClose=new BooleanLatch();
         Tools.ensureSwingThread(new Runnable() {
            public void run() {
               getTextInput().setText("(close this window to continue)");
               setInputsEnabled(false);
               String pauseTitle="(inactive) " + getTitle();
               setTitle(pauseTitle);
            }
         });
         try {pauseBeforeClose.await();} catch (InterruptedException ignored) {}
      }
      
      Tools.ensureSwingThread(new Runnable() {
         public void run() {
            setVisible(false);
            dispose();
         }
      });
   }

   public boolean isClosed() {return functionRunner.isShutdown();}
   
   private JTextPane getTextOutput() {
      if (textOutput == null) {
         textOutput = new JTextPane(backlog.getDocument());
         textOutput.setBackground(consoleBackground);
         textOutput.addMouseListener(mouseListener);
         textOutput.setEditable(false);
         ((DefaultCaret)(textOutput.getCaret())).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
      }
      return textOutput;
   }

   private JLabel getPromptLabel() {
      if (promptLabel == null) {
         promptLabel = new JLabel();
      }
      return promptLabel;
   }

   private JTextField getTextInput() {
      if (textInput == null) {
         textInput = new JTextField();
         //Aggiungiamo il supporto per copia/incolla via mouse
         textInput.addMouseListener(mouseListener);
         //Sostituiamo la mappatura di INVIO per prendere in carico la linea inserita
         textInput.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "ENTER");
         textInput.getActionMap().put("ENTER", enterTextAction);
         textInput.requestFocusInWindow();
      }
      return textInput;
   }

   public void clearOutputBuffer() {
      synchronized (backlog) {
         backlog.clear();
         getTextOutput().validate();
         getTextOutput().setCaretPosition(backlog.getDocument().getLength());
         repaint();
      }
   }

   public String getConsoleTitle() {return getTitle();}

   public char[] getConsoleBuffer() {
      DefaultStyledDocument doc=backlog.getDocument();
      doc.readLock();
      char[] result=null;
      try {result=doc.getText(0, doc.getLength()).toCharArray();} catch (BadLocationException ignored) {}
      doc.readUnlock();
      return result;
   }

   public String getPrompt() {return getPromptLabel().getText();}

   public PrintStream getStderr() {return isClosed()?null:consoleStderr;}

   public InputStream getStdin() {return isClosed()?null:consoleStdin;}
   
   public void pipeToStdin(byte[] buffer) throws IOException {
      if (isClosed()) throw new IOException("Virtual Console is closed");
      if (doStdinEcho) stdinEcho.write(buffer);
      
      try {userToConsoleFifo.write(buffer);}
      catch (InterruptedException ignored) {}
   }
   
   public PrintStream getStdout() {return isClosed()?null:consoleStdout;}

   public void setConsoleTitle(final String title) {
      Tools.ensureSwingThread(new Runnable() {
         public void run() {setTitle((title==null)?"":title);}
      });
   }
   
   public void setInputsEnabled(final boolean enabled) {
      if (isClosed() && enabled) return;
      Tools.ensureSwingThread(new Runnable() {
         public void run() {
            getPromptLabel().setEnabled(enabled);
            getTextInput().setEnabled(enabled);
            getButtonEnter().setEnabled(enabled);
            int cnt=getToolBar().getComponentCount();
            for(int i=0;i<cnt;i++) getToolBar().getComponentAtIndex(i).setEnabled(enabled);
            getToolBar().setEnabled(enabled);
         }
      });      
   }
   
   public boolean isInputEnabled() {return getPromptLabel().isEnabled();}

   public void setEcho(boolean enabled) {
      doStdinEcho=enabled;
   }
   
   public boolean hasEcho() {return doStdinEcho;}
   
   public void setPrompt(final String prompt) {
      Tools.ensureSwingThread(new Runnable() {
         public void run() {getPromptLabel().setText((prompt==null)?"":prompt);}
      });
   }

   private JPanel getPanelContents() {
      if (panelContents == null) {
         panelContents = new JPanel();
         final GridBagLayout gridBagLayout = new GridBagLayout();
         gridBagLayout.columnWidths = new int[] {0,0,0,0,0};
         panelContents.setLayout(gridBagLayout);
         final GridBagConstraints gridBagConstraints = new GridBagConstraints();
         gridBagConstraints.gridwidth = 5;
         gridBagConstraints.insets = new Insets(5, 5, 5, 5);
         gridBagConstraints.weighty = 1;
         gridBagConstraints.weightx = 1;
         gridBagConstraints.fill = GridBagConstraints.BOTH;
         gridBagConstraints.gridy = 0;
         gridBagConstraints.gridx = 0;
         panelContents.add(getTextScroll(), gridBagConstraints);
         final GridBagConstraints gridBagConstraints_1 = new GridBagConstraints();
         gridBagConstraints_1.anchor = GridBagConstraints.EAST;
         gridBagConstraints_1.insets = new Insets(0, 5, 5, 3);
         gridBagConstraints_1.gridy = 1;
         gridBagConstraints_1.gridx = 0;
         panelContents.add(getPromptLabel(), gridBagConstraints_1);
         final GridBagConstraints gridBagConstraints_2 = new GridBagConstraints();
         gridBagConstraints_2.anchor = GridBagConstraints.SOUTHWEST;
         gridBagConstraints_2.fill = GridBagConstraints.BOTH;
         gridBagConstraints_2.insets = new Insets(0, 0, 5, 3);
         gridBagConstraints_2.weightx = 1;
         gridBagConstraints_2.gridy = 1;
         gridBagConstraints_2.gridx = 1;
         panelContents.add(getTextInput(), gridBagConstraints_2);
         final GridBagConstraints gridBagConstraints_3 = new GridBagConstraints();
         gridBagConstraints_3.insets = new Insets(0, 0, 5, 0);
         gridBagConstraints_3.gridy = 1;
         gridBagConstraints_3.gridx = 2;
         panelContents.add(getButtonEnter(), gridBagConstraints_3);
         final GridBagConstraints gridBagConstraints_4 = new GridBagConstraints();
         gridBagConstraints_4.insets = new Insets(0, 0, 5, 0);
         gridBagConstraints_4.gridy = 1;
         gridBagConstraints_4.gridx = 3;
         panelContents.add(getButtonScrollLock(), gridBagConstraints_4);
         final GridBagConstraints gridBagConstraints_5 = new GridBagConstraints();
         gridBagConstraints_5.insets = new Insets(0, 0, 5, 5);
         gridBagConstraints_5.gridy = 1;
         gridBagConstraints_5.gridx = 4;
         panelContents.add(getButtonClearBuffer(), gridBagConstraints_5);
      }
      return panelContents;
   }

   public void setMacros(final Macro[] functions) {
      if (isClosed()) return;
      Tools.ensureSwingThread(new Runnable() {
         public void run() {
            //Eliminare tutte le callback presenti
            getToolBar().removeAll();
            //Verificare se l'array passato è vuoto o punta a qualcosa
            if ((functions==null) || (functions.length==0)) {
               getToolBar().setVisible(false);
            } else {
               //Installare le nuove callback
               for(Macro cbk:functions) {
                  getToolBar().add(new MacroAction(cbk,VirtualConsoleImpl.this,functionRunner));
               }
               getToolBar().setVisible(true);
            }
            
            VirtualConsoleImpl.this.validate();
         }
      });
   }

   private JToolBar getToolBar() {
      if (toolBar == null) {
         toolBar = new JToolBar();
         toolBar.setVisible(false);
      }
      return toolBar;
   }

   private JButton getButtonEnter() {
      if (buttonEnter == null) {
         buttonEnter = new JButton(enterTextAction);
         buttonEnter = new JButton();
         buttonEnter.setIcon(Resources.getIcon("enter.png"));
         buttonEnter.setToolTipText("Enter");
         buttonEnter.setMargin(new Insets(3, 3, 3, 3));
      }
      return buttonEnter;
   }

   private JToggleButton getButtonScrollLock() {
      if (buttonScrollLock == null) {
         buttonScrollLock = new JToggleButton();
         buttonScrollLock.setIcon(Resources.getIcon("sclock-off.png"));
         buttonScrollLock.setSelectedIcon(Resources.getIcon("sclock-on.png"));
         buttonScrollLock.setToolTipText("Scroll lock");
         
         
//         buttonScrollLock.addActionListener(new ActionListener() {
//            public void actionPerformed(final ActionEvent e) {
//               scrollLock=getButtonScrollLock().isSelected();
//               if (scrollLock) {
//                  ((DefaultCaret)(getTextOutput().getCaret())).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);                  
//               } else {
//                  ((DefaultCaret)(getTextOutput().getCaret())).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
//                  getTextOutput().setCaretPosition(backlog.getDocument().getLength());
//               }
//            }
//         });
         
         buttonScrollLock.addActionListener(e -> switchScrollLock());
         
         buttonScrollLock.setMargin(new Insets(3, 3, 3, 3));
      }
      return buttonScrollLock;
   }
   
   private void switchScrollLock() {
      scrollLock=getButtonScrollLock().isSelected();
      if (scrollLock) {
         ((DefaultCaret)(getTextOutput().getCaret())).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);                  
      } else {
         ((DefaultCaret)(getTextOutput().getCaret())).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
         getTextOutput().setCaretPosition(backlog.getDocument().getLength());
      }
   }

   private JButton getButtonClearBuffer() {
      if (buttonClearBuffer == null) {
         buttonClearBuffer = new JButton();
         buttonClearBuffer.setToolTipText("Clear output buffer");
         buttonClearBuffer.setIcon(Resources.getIcon("clear.png"));
//         buttonClearBuffer.addActionListener(new ActionListener() {
//            public void actionPerformed(final ActionEvent e) {
//               clearOutputBuffer();
//            }
//         });
         buttonClearBuffer.addActionListener(e -> clearOutputBuffer());
         buttonClearBuffer.setMargin(new Insets(3, 3, 3, 3));
      }
      return buttonClearBuffer;
   }
}
