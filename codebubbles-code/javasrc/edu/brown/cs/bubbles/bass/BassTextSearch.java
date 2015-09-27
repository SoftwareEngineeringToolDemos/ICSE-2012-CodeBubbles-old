/********************************************************************************/
/*										*/
/*		BassTextSearch.java						*/
/*										*/
/*	Bubble Augmented Search Strategies text search area			*/
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


/* SVN: $Id$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bass;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.bowi.BowiConstants;
import edu.brown.cs.bubbles.bowi.BowiFactory;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.text.Keymap;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.Collection;


class BassTextSearch extends SwingGridPanel implements BassConstants,
	ActionListener, BudaConstants.BudaBubbleOutputer, BudaConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JTextField	input_field;
private JRadioButton	literal_button;
private JRadioButton	regex_button;
private JCheckBox	usecase_button;

private static String	search_text = "";
private static boolean	search_literal = false;
private static boolean	search_case = false;



private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BassTextSearch()
{
   setInsets(1);
   setOpaque(true);

   Font ft = bass_properties.getFontOption(BASS_TEXT_FONT_PROP,BASS_TEXT_FONT);

   int y = 0;
   JLabel lbl = new JLabel("Text Search",SwingConstants.CENTER);
   addGBComponent(lbl,0,y++,0,1,0,0);
   addGBComponent(new JSeparator(),0,y++,0,1,0,0);

   input_field = new JTextField(36);
   input_field.setText(search_text);
   input_field.setFont(ft);
   Color bptc = bass_properties.getColor(BASS_PANEL_TOP_COLOR_PROP,BASS_PANEL_TOP_COLOR);
   input_field.setBackground(bptc);
   input_field.setActionCommand("INPUT");
   input_field.addActionListener(this);
   input_field.selectAll();
   Keymap kmp = input_field.getKeymap();
   kmp.addActionForKeyStroke(KeyStroke.getKeyStroke("ESCAPE"),new AbortAction());
   kmp.addActionForKeyStroke(KeyStroke.getKeyStroke("F12"),new SearchAction());
   kmp.addActionForKeyStroke(KeyStroke.getKeyStroke("FIND"),new SearchAction());
   addGBComponent(input_field,0,y++,0,1,1,0);
   addGBComponent(new JSeparator(),0,y++,0,1,0,0);

   JLabel spacer = new JLabel("   ");
   addGBComponent(spacer,0,y,1,1,0,0);

   ButtonGroup grp = new ButtonGroup();
   literal_button = new JRadioButton("Literal Search",search_literal);
   literal_button.setOpaque(false);
   grp.add(literal_button);
   addGBComponent(literal_button,1,y++,0,1,0,0);

   regex_button = new JRadioButton("Regex Search",!search_literal);
   regex_button.setOpaque(false);
   grp.add(regex_button);
   addGBComponent(regex_button,1,y++,0,1,0,0);

   usecase_button = new JCheckBox("Case Sensitive",search_case);
   usecase_button.setOpaque(false);
   addGBComponent(usecase_button,1,y++,0,1,0,0);

   addMouseListener(new BudaConstants.FocusOnEntry(input_field));
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

JTextField getEditor()			{ return input_field; }

@Override public void grabFocus()	{ input_field.grabFocus(); }




/********************************************************************************/
/*										*/
/*	Callbacks for text input						*/
/*										*/
/********************************************************************************/

@Override public void actionPerformed(ActionEvent evt)
{
   String cmd = evt.getActionCommand();

   if (cmd.equals("INPUT")) {
      performSearch(true);
      setVisible(false);
    }
}



private void performSearch(boolean inplace)
{
   search_literal = literal_button.isSelected();
   search_case = usecase_button.isSelected();
   search_text = input_field.getText();
   if (search_text == null || search_text.trim().length() == 0) return;
   BoardMetrics.noteCommand("BASS","TextSearch");

   BoardThreadPool.start(new Searcher(search_text,inplace));
}



private class Searcher implements Runnable {

   private boolean in_place;
   private String searcher_text;
   private Collection<BumpLocation> search_result;
   Rectangle search_location;

   Searcher(String txt,boolean inp) {
      searcher_text = txt;
      in_place = inp;
      search_result = null;
      search_location = BudaRoot.findBudaLocation(BassTextSearch.this);
    }

   @Override public void run() {
      if (search_result == null) {
	 BowiFactory.startTask(BowiConstants.BowiTaskType.TEXT_SEARCH);
	 try {
	    BumpClient bc = BumpClient.getBump();
	    Collection<BumpLocation> locs = bc.textSearch(null,searcher_text,
		     search_literal,
		     !search_case,
		     false);
	    if (locs == null || locs.size() == 0) return;
	    search_result = locs;
	  }
	 finally { BowiFactory.stopTask(BowiConstants.BowiTaskType.TEXT_SEARCH); }
	 SwingUtilities.invokeLater(this);
       }
      else {
	 Point pt = null;
	 if (in_place && search_location != null) {
	    pt = new Point(search_location.x,search_location.y);
	  }

	 BaleFactory.getFactory().createBubbleStack(BassTextSearch.this,null,pt,false,
						       search_result,BudaLinkStyle.NONE);
       }
    }

}	// end of inner class Searcher




/********************************************************************************/
/*										*/
/*	Keyboard actions							*/
/*										*/
/********************************************************************************/

private static class AbortAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   AbortAction() {
      super("AbortSearchAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BudaBubble bb = BudaRoot.findBudaBubble((Component) e.getSource());
      if (bb != null) bb.setVisible(false);
    }

}	// end of inner class AbortAction




private class SearchAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   SearchAction() {
      super("SearchAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      performSearch(false);
    }

}	// end of inner class SearchAction




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String getConfigurator()		{ return "BASS"; }


@Override public void outputXml(BudaXmlWriter xw)
{
   xw.field("TYPE","TEXTSEARCH");
   xw.field("LITERAL",search_literal);
   xw.field("USECASE",search_case);
   xw.field("TEXT",search_text);
}



static void outputStatic(BudaXmlWriter xw)
{
   xw.field("TEXTLITERAL",search_literal);
   xw.field("TEXTUSECASE",search_case);
   xw.field("TEXTSTRING",search_text);
}


static void loadStatic(Element data)
{
   search_literal = IvyXml.getAttrBool(data,"TEXTLITERAL",false);
   search_case = IvyXml.getAttrBool(data,"TEXTUSECASE",false);
   search_text = IvyXml.getAttrString(data,"TEXTSTRING");
}




/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override protected void paintComponent(Graphics g)
{
   super.paintComponent(g);

   Graphics2D g2 = (Graphics2D) g.create();
   Dimension sz = getSize();
   Color bptc = bass_properties.getColor(BASS_PANEL_TOP_COLOR_PROP,BASS_PANEL_TOP_COLOR);
   Color bpbc = bass_properties.getColor(BASS_PANEL_BOTTOM_COLOR_PROP,BASS_PANEL_BOTTOM_COLOR);
   Paint p = new GradientPaint(0f,0f,bptc,0f,sz.height,bpbc);
   Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
   g2.setPaint(p);
   g2.fill(r);
}




}	// end of class BassTextSearch




/* end of BassTextSearch.java */
