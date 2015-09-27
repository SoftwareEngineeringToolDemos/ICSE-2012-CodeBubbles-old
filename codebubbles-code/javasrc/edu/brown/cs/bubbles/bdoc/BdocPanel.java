/********************************************************************************/
/*										*/
/*		BdocPanel.java							*/
/*										*/
/*	Bubbles Environment Documentation display panel 			*/
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


package edu.brown.cs.bubbles.bdoc;


import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.*;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.text.View;
import javax.swing.tree.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.*;
import java.util.List;



class BdocPanel implements BdocConstants, BudaConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JScrollPane	doc_region;
private DocPanel	the_panel;
private BdocReference	ref_item;
private BdocDocItem	doc_item;
private DescriptionView desc_view;
private JTree		item_tree;
private BdocCellRenderer cell_renderer;

private static int	scroll_width = 0;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BdocPanel(BdocReference r) throws BdocException
{
   ref_item = r;

   cell_renderer = new BdocCellRenderer(this);

   try {
      switch (r.getNameType()) {
	 case PACKAGE :
	    doc_item = new BdocDocPackage(r.getReferenceUrl());
	    break;
	 case METHOD :
	 case CONSTRUCTOR :
	    doc_item = new BdocDocMethod(r.getReferenceUrl());
	    break;
	 case FIELDS :
	    doc_item = new BdocDocField(r.getReferenceUrl());
	    break;
	 case CLASS :
	 case ENUM :
	 case INTERFACE :
	 case THROWABLE :
	    doc_item = new BdocDocClass(r.getReferenceUrl());
	    break;
	 default :
	    throw new BdocException("No java doc available for " + r);
       }
    }
   catch (IOException e) {
      throw new BdocException("Problem getting javadoc",e);
    }

   setupPanel();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

JComponent getPanel()
{
   return doc_region;
}



/********************************************************************************/
/*										*/
/*	Panel setup methods							*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   the_panel = new DocPanel();

   JLabel ttl = new JLabel(ref_item.getDigestedName());
   //JLabel ttl = new JLabel(ref_item.getNameHead());
   ttl.setFont(CONTEXT_FONT);
   ttl.setForeground(CONTEXT_COLOR);
   ttl.setBackground(BDOC_TOP_COLOR);
   ttl.addMouseListener(new TitleMouser());
   ttl.setOpaque(true);
   the_panel.setTitleComponent(ttl);

   String desc = "<html>" + doc_item.getDescription();

   desc_view = new DescriptionView(desc);
   the_panel.setDescriptionComponent(desc_view);

   DefaultMutableTreeNode root = new DefaultMutableTreeNode();
   for (ItemRelation ir : ItemRelation.values()) {
      List<SubItem> itms = doc_item.getItems(ir);
      if (itms != null) {// get nicer name
	 DefaultMutableTreeNode relnode = new DefaultMutableTreeNode(ir,true);
	 root.add(relnode);
	 for (SubItem itm : itms) {
	    // really want a tree node that displays the item
	    MutableTreeNode itmnode = new DefaultMutableTreeNode(itm,false);
	    relnode.add(itmnode);
	 }
      }
    }

   item_tree = new ItemTree(root);
   item_tree.setRootVisible(false);
   item_tree.setCellRenderer(cell_renderer);
   item_tree.addMouseListener(new ItemMouser());
   item_tree.addTreeExpansionListener(new TreeListener());
   item_tree.setFocusable(true);

   the_panel.setItemsComponent(item_tree);

   the_panel.addComponentListener(new PanelWidthManager());

   BudaCursorManager.setCursor(the_panel,Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

   the_panel.setBackground(BDOC_TOP_COLOR);
   the_panel.setFocusable(true);

   doc_region = new JScrollPane(the_panel);
}





private void createNewBubble(BdocReference br)
{
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(the_panel);
   BudaBubble bb = BudaRoot.findBudaBubble(the_panel);
   if (bba == null) return;

   try {
      // This can take a while and should be done outside the UI thread, the rest should be done in UI thread
      BdocBubble nbb = new BdocBubble(br);
      bba.addBubble(nbb,the_panel,null,
	    PLACEMENT_RIGHT|PLACEMENT_LOGICAL|PLACEMENT_MOVETO);
      if (bb != null) {
	 BudaBubbleLink lnk = new BudaBubbleLink(
	    bb,
	    new BudaDefaultPort(BudaPortPosition.BORDER_ANY,true),
	    nbb,
	    new BudaDefaultPort(BudaPortPosition.BORDER_ANY,true));
	 bba.addLink(lnk);
       }
    }
   catch (BdocException e) {
      BoardLog.logE("BDOC","Problem creating new doc bubble: " + e);
    }
}



void createLinkBubble(String lbl,URL u)
{
   if (lbl != null) {
      BdocReference br = ref_item.findRelatedReference(lbl);
      if (br != null) {
	 createNewBubble(br);
	 return;
       }
    }

   if (u == null && lbl != null) {
      try {
	 u = new URL(ref_item.getReferenceUrl(),lbl);
       }
      catch (MalformedURLException ex) { }
    }

   if (u == null) {
      BoardLog.logE("BDOC","Can't create URL for " + lbl);
    }
   else {
      BdocReference br = ref_item.findRelatedReference(u);
      if (br != null) {
	 createNewBubble(br);
	 return;
       }

      // create html bubble here
      BoardLog.logI("BDOC","Hyperlink to " + u);
    }
}



/********************************************************************************/
/*										*/
/*	Handle computing editor size						*/
/*										*/
/********************************************************************************/

static Dimension computeEditorSize(JEditorPane ep,int w0)
{
   int delta = getScrollWidth();

   JLabel lbl = new JLabel();
   lbl.setFont(ep.getFont());
   lbl.setText(ep.getText());
   View v = (View) lbl.getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey);
   if (w0 <= 0) w0 = DESCRIPTION_WIDTH;
   v.setSize(w0-delta,0);
   float w = v.getPreferredSpan(View.X_AXIS);
   float h = v.getPreferredSpan(View.Y_AXIS);
   w += delta;

   Dimension d = new Dimension((int) Math.ceil(w),(int) Math.ceil(h));

   return d;
}



static synchronized int getScrollWidth()
{
   if (scroll_width == 0) {
      JScrollBar sb = new JScrollBar(JScrollBar.VERTICAL);
      Dimension dsb = sb.getPreferredSize();
      scroll_width = dsb.width + 4;
    }
   return scroll_width;
}


/********************************************************************************/
/*										*/
/*	Handle formatting text that is read in					*/
/*										*/
/********************************************************************************/

private String noPre(String d)
{
   return d;
}




/********************************************************************************/
/*										*/
/*	Panel implementation							*/
/*										*/
/********************************************************************************/

private class DocPanel extends SwingGridPanel implements Scrollable, BudaConstants.BudaBubbleOutputer {

   private static final long serialVersionUID = 1;

   DocPanel() {
      setOpaque(false);
    }

   // scrollable interface
   @Override public Dimension getPreferredScrollableViewportSize()		{ return getPreferredSize(); }
   @Override public int getScrollableBlockIncrement(Rectangle r,int o,int dir)	{ return 12; }
   @Override public boolean getScrollableTracksViewportHeight() 		{ return false; }
   @Override public boolean getScrollableTracksViewportWidth()			{ return true; }
   @Override public int getScrollableUnitIncrement(Rectangle r,int o,int d)	{ return 1; }

   @Override public String getConfigurator()					{ return "BDOC"; }
   @Override public void outputXml(BudaXmlWriter xw) {
      xw.field("TYPE","JAVADOC");
      xw.field("NAME",ref_item.getKey());
    }

   void setTitleComponent(JComponent ttl) {
      Dimension d = ttl.getPreferredSize();
      ttl.setMinimumSize(d);
      ttl.setMaximumSize(d);
      addGBComponent(ttl,0,0,0,1,0,0);
      //addGBComponent(new JSeparator(),0,1,0,1,1,0);
    }

   void setDescriptionComponent(JComponent desc) {
      addGBComponent(desc,0,2,0,1,1,0);
      //addGBComponent(new JSeparator(),0,3,0,1,1,0);
    }

   void setItemsComponent(JComponent itms) {
      addGBComponent(itms,0,4,0,1,1,0);
    }

   @Override protected void paintComponent(Graphics g0) {
      Graphics2D g2 = (Graphics2D) g0.create();

      if (BDOC_TOP_COLOR.getRGB() != BDOC_BOTTOM_COLOR.getRGB()) {
	 Dimension sz = getSize();
	 Paint p = new GradientPaint(0f,0f,BDOC_TOP_COLOR,0f,sz.height,BDOC_BOTTOM_COLOR);
	 Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
	 g2.setColor(Color.WHITE);		// solid fill first
	 g2.fill(r);
	 g2.setPaint(p);
	 g2.fill(r);
       }

      super.paintComponent(g0);
    }

    @Override public void scrollRectToVisible( Rectangle r ) {}

}	// end of inner class DocPanel



private class PanelWidthManager extends ComponentAdapter {

   @Override public void componentResized(ComponentEvent e) {
      Dimension d = the_panel.getSize();
      cell_renderer.setTreeWidth(d.width-4);
      item_tree.invalidate();
      BasicTreeUI tui = (BasicTreeUI) item_tree.getUI();
      tui.setLeftChildIndent(tui.getLeftChildIndent());
      Dimension d1 = the_panel.getPreferredSize();
      Dimension d2 = the_panel.getSize();
      if (d1.height > d2.height) {
	 d2.height = d1.height + 4;
	 the_panel.setSize(d2);
       }
    }

}	// end of inner class TreeWidthManager



/********************************************************************************/
/*										*/
/*	Class to hold the description						*/
/*										*/
/********************************************************************************/

private class DescriptionView extends JEditorPane {

   private static final long serialVersionUID = 1;


   DescriptionView(String d) {
      super("text/html",noPre(d));
      setFont(NAME_FONT);
      setForeground(NAME_COLOR);
      setEditable(false);
      setOpaque(false);
      BudaCursorManager.setCursor(this,new Cursor(Cursor.TEXT_CURSOR));
      putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,Boolean.TRUE);
      addHyperlinkListener(new DocLinker());
    }

   @Override public boolean getScrollableTracksViewportWidth() { return true; }

   @Override public Dimension getPreferredSize() {
      return computeEditorSize(this,getWidth());
   }

}	// end of inner class DescriptionView




private class DocLinker implements HyperlinkListener {

   @Override public void hyperlinkUpdate(HyperlinkEvent e) {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
	 URL u = e.getURL();
	 String lbl = e.getDescription();
	 createLinkBubble(lbl,u);
       }
      else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
	 BudaCursorManager.setTemporaryCursor(desc_view, new Cursor(Cursor.HAND_CURSOR));
      }
      else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
	 BudaCursorManager.resetDefaults(desc_view);
      }
    }

}	// end of inner class DocLinker




/********************************************************************************/
/*										*/
/*	Class for tree								*/
/*										*/
/********************************************************************************/

private static class ItemTree extends JTree {

   private static final long serialVersionUID = 1;

   ItemTree(TreeNode root) {
      super(root);
      setOpaque(false);
      setToggleClickCount(1);
    }

}	// end of inner class ItemTree



/********************************************************************************/
/*										*/
/*	Tree expansion handling 						*/
/*										*/
/********************************************************************************/

private void checkExpandPanel()
{
   JScrollPane jsp = (JScrollPane) the_panel.getParent().getParent();
   Dimension osz = jsp.getSize();
   Dimension d = the_panel.getPreferredSize();
   if (d.height >= MAX_EXPAND_HEIGHT) d.height = MAX_EXPAND_HEIGHT;
   if (osz.height < d.height) {
      osz.height = d.height;
      jsp.setSize(osz);
    }
}




private class TreeListener implements TreeExpansionListener {

   @Override public void treeCollapsed(TreeExpansionEvent e)	{ }

   @Override public void treeExpanded(TreeExpansionEvent e) {
      checkExpandPanel();
    }

}	// end of inner class TreeListener



/********************************************************************************/
/*										*/
/*	Mouse handling								*/
/*										*/
/********************************************************************************/

private class TitleMouser extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent e) {
      try {
	 URL u = ref_item.getReferenceUrl();
	 if (u == null) return;
	 URI ui = u.toURI();
	 URI uin = new URI(ui.getScheme(),ui.getUserInfo(),ui.getHost(),ui.getPort(),ui.getPath(),null,null);
	 URL un = uin.toURL();
	 createLinkBubble(null,un);
       }
      catch (Exception ex) {
	 BoardLog.logE("BDOC","Problem handling title click",ex);
       }
    }

}	// end of inner class TitleMouser


private class ItemMouser extends MouseAdapter {

   @Override public void mousePressed(MouseEvent e) {
      JTree tree = (JTree) e.getSource();
      int selrow = tree.getRowForLocation(e.getX(),e.getY());
      if (selrow != -1 && e.getClickCount() == 1) {
	 TreePath selpath = tree.getPathForRow(selrow);
	 DefaultMutableTreeNode tn = (DefaultMutableTreeNode) selpath.getLastPathComponent();
	 if (tn.isLeaf()) {
	    SubItem sitm = (SubItem) tn.getUserObject();
	    createLinkBubble(sitm.getRelativeUrl(),sitm.getItemUrl());
	  }
       }
    }

}	// end of inner class ItemMouser





}	// end of class BdocPanel


/* end of BdocPanel.java */



