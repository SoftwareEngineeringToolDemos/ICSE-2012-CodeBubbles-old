/********************************************************************************/
/*										*/
/*		BeamNoteBubble.java						*/
/*										*/
/*	Bubble Environment Auxilliary & Missing items note bubble		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 * This program and the accompanying materials are made available under the	 *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at								 *
 *	http://www.eclipse.org/legal/epl-v10.html				 *
 *										 *
 ********************************************************************************/


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.beam;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.burp.BurpHistory;

import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.swing.SwingText;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.*;
import javax.swing.text.html.HTMLEditorKit;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;


class BeamNoteBubble extends BudaBubble implements BeamConstants,
	BudaConstants.BudaBubbleOutputer, BudaConstants.Scalable
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private NoteArea		note_area;
private String			note_name;
private BeamNoteAnnotation	note_annot;

private static BoardProperties	beam_properties = BoardProperties.getProperties("Beam");

private static Map<String,Document> file_documents;


private static final String	MENU_KEY = "menu";
private static final String	menu_keyname;
private static SimpleDateFormat file_dateformat = new SimpleDateFormat("yyMMddHHmmss");

static {
   int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
   if (mask == Event.META_MASK) menu_keyname = "meta";
   else if (mask == Event.CTRL_MASK) menu_keyname = "ctrl";
   else menu_keyname = "ctrl";

   file_documents = new HashMap<String,Document>();
}




private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Keymap for notes							*/
/*										*/
/********************************************************************************/

private static Keymap		note_keymap;

private static final KeyItem [] key_defs = new KeyItem[] {
   new KeyItem("menu B",new NoteEditorKit.BoldAction()),
      new KeyItem("menu I",new NoteEditorKit.ItalicAction()),
      new KeyItem("menu U",new NoteEditorKit.UnderlineAction()),
      new KeyItem("ctrl 1",new NoteEditorKit.NoteFontSizeAction("font_size_10",10)),
      new KeyItem("ctrl 2",new NoteEditorKit.NoteFontSizeAction("font_size_12",12)),
      new KeyItem("ctrl 3",new NoteEditorKit.NoteFontSizeAction("font_size_16",14)),
      new KeyItem("ctrl 4",new NoteEditorKit.NoteFontSizeAction("font_size_20",16)),
      new KeyItem("ctrl 5",new NoteEditorKit.NoteFontSizeAction("font_size_24",24)),
      new KeyItem("ctrl shift 1",new NoteEditorKit.NoteColorAction("foreground_black",Color.BLACK)),
      new KeyItem("ctrl shift 2",new NoteEditorKit.NoteColorAction("foreground_red",Color.RED)),
      new KeyItem("ctrl shift 3",new NoteEditorKit.NoteColorAction("foreground_green",Color.GREEN)),
      new KeyItem("ctrl shift 4",new NoteEditorKit.NoteColorAction("foreground_blue",Color.BLUE)),
      new KeyItem("ctrl shift 5",new NoteEditorKit.AlignmentAction("align_left",StyleConstants.ALIGN_LEFT)),
      new KeyItem("ctrl shift 6",new NoteEditorKit.AlignmentAction("align_center",StyleConstants.ALIGN_CENTER)),
      new KeyItem("ctrl shift 7",new NoteEditorKit.AlignmentAction("align_right",StyleConstants.ALIGN_RIGHT)),
      new KeyItem("ctrl shift 8",new NoteEditorKit.AlignmentAction("align_justified",StyleConstants.ALIGN_JUSTIFIED)),
      new KeyItem("menu Z",BurpHistory.getUndoAction()),
      new KeyItem("menu Y",BurpHistory.getRedoAction()),
      new KeyItem("menu S",new SaveAction())
};


static {
   Keymap dflt = JTextComponent.getKeymap(JTextComponent.DEFAULT_KEYMAP);
   SwingText.fixKeyBindings(dflt);
   note_keymap = JTextComponent.addKeymap("NOTE",dflt);
   for (KeyItem ka : key_defs) {
      ka.addToKeyMap(note_keymap);
    }
}






/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BeamNoteBubble()
{
   this(null,null,null);
}


BeamNoteBubble(String name,String cnts,BeamNoteAnnotation annot)
{
   super(null,BudaBorder.RECTANGLE);

   Document d = null;
   if (name != null) d = file_documents.get(name);

   if (name != null) note_name = name;
   else createFileName();

   note_area = null;
   if (d != null) {
      note_area = new NoteArea(d);
      file_documents.put(note_name,note_area.getDocument());
    }
   else {
      note_area = new NoteArea(cnts);
      loadNote(false);
      file_documents.put(note_name,note_area.getDocument());
    }

   if (annot != null && annot.getDocumentOffset() < 0) annot = null;
   note_annot = annot;
   if (annot != null) {
      BaleFactory.getFactory().addAnnotation(annot);
      annot.setAnnotationFile(note_name);
    }

   addComponentListener(new ComponentHandler());

   // if contents are null, then set the header part of the html with information about
   // the source of this bubble, date, dlm, title, etc.

   JScrollPane jsp = new JScrollPane(note_area);

   setContentPane(jsp,note_area);
}



@Override protected void localDispose()
{
   if (note_name != null) {
      if (note_area.getText().length() == 0 || note_annot == null) deleteNote();
      note_annot = null;
      note_name = null;
    }
}



/********************************************************************************/
/*										*/
/*	Popup menu methods							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu menu = new JPopupMenu();

   menu.add(getFloatBubbleAction());

   menu.show(this,e.getX(),e.getY());
}



/********************************************************************************/
/*										*/
/*	Scaling requests							*/
/*										*/
/********************************************************************************/

@Override public void setScaleFactor(double sf)
{
   Font ft = beam_properties.getFont(NOTE_FONT_PROP,NOTE_FONT);
   float sz = ft.getSize2D();
   if (sf != 1.0) {
      sz *= sf;
      ft = ft.deriveFont(sz);
    }
   note_area.setFont(ft);
}

/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/**********************************************a**********************************/

@Override protected void paintContentOverview(Graphics2D g,Shape s)
{
   Dimension sz = getSize();

   g.setColor(NOTE_OVERVIEW_COLOR);
   g.fillRect(0,0,sz.width,sz.height);
}




/********************************************************************************/
/*										*/
/*	Save interface								*/
/*										*/
/********************************************************************************/

@Override public void handleSaveRequest()
{
   saveNote();
}



@Override public void handleCheckpointRequest()
{
   saveNote();
}



/********************************************************************************/
/*										*/
/*	Annotation interface							*/
/*										*/
/********************************************************************************/

void clearAnnotation(BeamNoteAnnotation na)
{
   if (na != null && na == note_annot) note_annot = null;
}



/********************************************************************************/
/*										*/
/*	Methods for loading and saving notes					*/
/*										*/
/********************************************************************************/

static void updateNote(String name,String cnts)
{
   Document d = file_documents.get(name);
   if (d == null) return;

   int len = d.getLength();
   try {
      String txt = d.getText(0,len);
      if (txt.equals(cnts)) return;
      EditorKit ek = new NoteEditorKit();
      StringReader sr = new StringReader(cnts);
      ek.read(sr,d,0);
      // d.remove(0,len);
      // d.insertString(0,cnts,null);
    }
   catch (BadLocationException e) {
      BoardLog.logE("BEAM","Problem updating note",e);
    }
   catch (IOException e) {
      BoardLog.logE("BEAM","Problem updating node",e);
    }
}




private synchronized void loadNote(boolean force)
{
   if (note_name == null) createFileName();

   switch (BoardSetup.getSetup().getRunMode()) {
      case CLIENT :
	 BumpClient bc = BumpClient.getBump();
	 try {
	    File tmp = File.createTempFile("BUBBLES_NOTE_",".html");
	    File f = bc.getRemoteFile(tmp,"NOTE",new File(note_name));
	    if (f != null) loadNoteFromFile(f,force);
	    tmp.delete();
	  }
	 catch (IOException e) { }
	 break;
      case SERVER :
      case NORMAL :
	 loadNoteFromFile(getNoteFile(),force);
	 break;
    }
}



private void loadNoteFromFile(File f,boolean force)
{
   String cnts = "";

   try {
      FileReader fr = new FileReader(getNoteFile());
      StringBuffer cbuf = new StringBuffer();
      char [] buf = new char[1024];
      for ( ; ; ) {
	 int ln = fr.read(buf);
	 if (ln < 0) break;
	 cbuf.append(buf,0,ln);
       }
      cnts = cbuf.toString();
      fr.close();
    }
   catch (IOException e) {
      if (force) BoardLog.logE("BEAM","Problem reading note file",e);
      else return;
    }
   note_area.setText(cnts);
}


private synchronized void saveNote()
{
   if (note_name == null) createFileName();

   BoardSetup bs = BoardSetup.getSetup();

   switch (bs.getRunMode()) {
      case CLIENT :
	 break;
      case SERVER :
      case NORMAL :
	 try {
	    FileWriter fw = new FileWriter(getNoteFile());
	    String txt = note_area.getText();
	    fw.write(txt);
	    fw.close();
	  }
	 catch (IOException e) {
	    BoardLog.logE("BEAM","Problem writing note file",e);
	  }
	 break;
    }

   switch (bs.getRunMode()) {
      case CLIENT :
      case NORMAL :
	 MintControl mc = bs.getMintControl();
	 IvyXmlWriter xw = new IvyXmlWriter();
	 xw.begin("BEAM");
	 xw.field("TYPE","NOTE");
	 xw.field("NAME",note_name);
	 xw.cdataElement("TEXT",note_area.getText());
	 xw.end("BEAM");
	 mc.send(xw.toString());
	 break;
      case SERVER :
	 break;
    }
}


private synchronized void deleteNote()
{
   switch (BoardSetup.getSetup().getRunMode()) {
      case CLIENT :
	 break;
      case SERVER :
      case NORMAL :
	 File f = getNoteFile();
	 f.delete();
	 break;
    }
}


File getNoteFile()
{
   if (note_name == null) createFileName();
   File dir = BoardSetup.getBubblesWorkingDirectory();
   return new File(dir,note_name);
}


private void createFileName()
{
   if (note_name != null) return;

   String rid = Integer.toString((int)(Math.random() * 10000));
   String fnm = "Note_" + file_dateformat.format(new Date()) + "_" + rid + ".html";
   note_name = fnm;
}



/********************************************************************************/
/*										*/
/*	Configurator interface							*/
/*										*/
/********************************************************************************/

@Override public String getConfigurator()		{ return "BEAM"; }


@Override public void outputXml(BudaXmlWriter xw)
{
   xw.field("TYPE","NOTE");
   if (note_name != null) xw.field("NAME",note_name);
   xw.cdataElement("TEXT",note_area.getText());
   if (note_annot != null) note_annot.saveAnnotation(xw);
}



/********************************************************************************/
/*										*/
/*	Note area implementation						*/
/*										*/
/********************************************************************************/

private static class NoteArea extends JEditorPane
{
   private static final long serialVersionUID = 1;


   NoteArea(String cnts) {
      super("text/html",cnts);
      initialize();
      setText(cnts);
    }

   NoteArea(Document d) {
      setContentType("text/html");
      initialize();
      setDocument(d);
    }

   private void initialize() {
      setEditorKit(new NoteEditorKit());
      setKeymap(note_keymap);
      Dimension d = new Dimension(beam_properties.getInt(NOTE_WIDTH),beam_properties.getInt(NOTE_HEIGHT));
      setPreferredSize(d);
      setSize(d);
      putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,Boolean.TRUE);
      setFont(beam_properties.getFont(NOTE_FONT_PROP,NOTE_FONT));
      addMouseListener(new BudaConstants.FocusOnEntry());
      addMouseListener(new LinkListener());
      addHyperlinkListener(new HyperListener());

      if (beam_properties.getColor(NOTE_TOP_COLOR).getRGB() == beam_properties.getColor(NOTE_BOTTOM_COLOR).getRGB()) {
	 setBackground(beam_properties.getColor(NOTE_TOP_COLOR));
       }
      else setBackground(new Color(0,true));

      BurpHistory.getHistory().addEditor(this);
    }

   @Override protected void finalize() throws Throwable {
      try {
	 BurpHistory.getHistory().removeEditor(this);
       }
      finally { super.finalize(); }
    }

   @Override protected void paintComponent(Graphics g0) {
      if (beam_properties.getColor(NOTE_TOP_COLOR).getRGB() != beam_properties.getColor(NOTE_BOTTOM_COLOR).getRGB()) {
	 Graphics2D g2 = (Graphics2D) g0.create();
	 Dimension sz = getSize();
	 Paint p = new GradientPaint(0f,0f,beam_properties.getColor(NOTE_TOP_COLOR),0f,sz.height,beam_properties.getColor(NOTE_BOTTOM_COLOR));
	 Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
	 g2.setPaint(p);
	 g2.fill(r);
       }
      super.paintComponent(g0);
    }

}	// end of inner class NoteArea



/********************************************************************************/
/*										*/
/*	Editor Kit for notes							*/
/*										*/
/********************************************************************************/

private static class NoteEditorKit extends HTMLEditorKit
{

   private static final long serialVersionUID = 1;

   NoteEditorKit() {
      setDefaultCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
    }

   private static class NoteFontSizeAction extends FontSizeAction {

      private static final long serialVersionUID = 1;

      NoteFontSizeAction(String nm,int sz) {
	 super(nm,sz);
	 putValue(ACTION_COMMAND_KEY,nm);
       }

    }	// end of inner class NoteFontSizeAction


   private static class NoteColorAction extends ForegroundAction {

      private static final long serialVersionUID = 1;

      NoteColorAction(String nm,Color c) {
	 super(nm,c);
	 putValue(ACTION_COMMAND_KEY,nm);
       }

    }	// end of inner class NoteColorAction

}	// end of inner class NoteEditorKit



private static class SaveAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   @Override public void actionPerformed(ActionEvent e) {
      Component c = (Component) e.getSource();
      BeamNoteBubble bb = (BeamNoteBubble) BudaRoot.findBudaBubble(c);
      if (bb == null) return;
      bb.saveNote();
    }

}	// end of inner class SaveAction



/********************************************************************************/
/*										*/
/*	Key designator class							*/
/*										*/
/********************************************************************************/

private static class KeyItem {

   private KeyStroke key_stroke;
   private Action key_action;

   KeyItem(String key,Action a) {
      key = fixKey(key);
      key_stroke = KeyStroke.getKeyStroke(key);
      if (key_stroke == null) BoardLog.logE("BEAM","Bad key definition: " + key);
      key_action = a;
    }

   void addToKeyMap(Keymap kmp) {
      if (key_stroke != null && key_action != null) {
	 kmp.addActionForKeyStroke(key_stroke,key_action);
       }
    }

   private String fixKey(String key) {
      return key.replace(MENU_KEY,menu_keyname);
    }

}	// end of inner class KeyItem



/********************************************************************************/
/*										*/
/*	Callbacks to handle automatic saving					*/
/*										*/
/********************************************************************************/

private class ComponentHandler extends ComponentAdapter {

   @Override public void componentHidden(ComponentEvent e) {
      if (note_name != null && note_area != null) {
	 if (note_area.getText().length() > 0) saveNote();
       }
    }

}	// end of inner class ComponentHandler



/********************************************************************************/
/*										*/
/*	Link listener								*/
/*										*/
/********************************************************************************/

private static class LinkListener extends HTMLEditorKit.LinkController {

   private static final long serialVersionUID = 1;


   @Override public void mouseClicked(MouseEvent e) {
      JEditorPane editor = (JEditorPane) e.getSource();
      if (!editor.isEditable()) return;
      if (!SwingUtilities.isLeftMouseButton(e)) return;
      int mods = e.getModifiersEx();
      if ((mods & MouseEvent.ALT_DOWN_MASK) != 0) {
	 Point pt = new Point(e.getX(),e.getY());
	 int pos = editor.viewToModel(pt);
	 if (pos >= 0) {
	    activateLink(pos,editor);
	  }
       }
    }

}	// end of inner class LinkListener



private static class HyperListener implements HyperlinkListener {

   @Override public void hyperlinkUpdate(HyperlinkEvent e) {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
	 URL u = e.getURL();
	 try {
	    BeamFactory.showBrowser(u.toURI());
	  }
	 catch (URISyntaxException ex) { }
       }
    }

}	// end of inner class HyperListener



}	// end of class BeamNoteBubble



/* end of BeamNoteBubble.java */
