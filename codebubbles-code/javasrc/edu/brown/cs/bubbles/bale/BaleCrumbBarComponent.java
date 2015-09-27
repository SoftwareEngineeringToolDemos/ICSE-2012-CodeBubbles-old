/********************************************************************************/
/*										*/
/*		BaleCrumbBarComponent.java					*/
/*										*/
/*	Bubble Annotated Language Editor Fragment editor crumb bar component	*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Ian Strickman		      */
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



package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.buda.BudaRoot;

import javax.swing.JTextPane;
import javax.swing.text.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;



class BaleCrumbBarComponent extends JTextPane implements BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BaleCrumbBar			_parent;
private int				nat_width;
private int				nat_height;
private RoundRectangle2D.Double 	_oval;
private boolean 			rolled_over, my_search_up;
private String				my_arrow;
private String				package_name, shown_text;
private BaleCrumbBarComponent		_brother;
private Color				_color;
private boolean 			is_dirty;

private static final char ARROW = '\u25B6';
private static final int ELLIDED_WIDTH = 12; //from experimentation
private static final long serialVersionUID = 1;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleCrumbBarComponent(BaleCrumbBar par, BaleCrumbBarComponent bro, String shotxt)
{
   _parent = par;
   _brother = bro;
   is_dirty = false;

   if (shotxt.endsWith(""+ARROW) || shotxt.endsWith(">")) {
      shown_text = shotxt.substring(0, shotxt.length()-1);
      my_arrow = shotxt.substring(shotxt.length()-1);
    }
   else {
      shown_text = shotxt;
      my_arrow = " ";
    }

   //nat_width = getColumnWidth()*shotxt.length()/2;
   setText(shown_text);
   setEditable(false);

   Dimension d0 = getPreferredSize();
   nat_height = d0.height;

   // DefaultCaret car = (DefaultCaret) getCaret();
   // car.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
   // car.setVisible(false);
   setCaret(new CrumbCaret());

   _oval = new RoundRectangle2D.Double();
   rolled_over=false;
   my_search_up = false;
   package_name = null;
   _color = BALE_CRUMB_COMPONENT_COLOR;
   setForeground(_color);
   append(my_arrow, Color.black);
   addMouseListener(new Mouser());

   if (BudaRoot.showHelpTips()) {
      setToolTipText("Click to begin search starting here");
    }
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

private void instOval()
{
   _oval.setRoundRect(0, 0, this.getWidth()-2, this.getHeight()-1, 5, 5); //makes oval correct size.
}



void setColor(Color tobe)
{
   _color = tobe;
   setText("");
   append(shown_text, _color);
   append(my_arrow, Color.black);
}

String getShownText() { return shown_text; }

void setPackageName(String pfx)
{
   package_name = pfx;
}



String getPackageName()
{
   return package_name;
}


/*
 * This method calculates the total width of the group of BaleCrumbBarComponents being used
 */
int getWidthLocation()
{
   if(_brother == null){
      return getWidth();
   }
   else{
      return _brother.getWidthLocation() + getWidth();
   }
}



/********************************************************************************/
/*										*/
/*	Text appending methods							*/
/*										*/
/********************************************************************************/

void append(String toAp, Color c)
{
   try {
      SimpleAttributeSet attr=new SimpleAttributeSet();
      StyleConstants.setForeground(attr,c);
      getStyledDocument().insertString(getStyledDocument().getLength(),toAp,attr);
    }
   catch(BadLocationException e) { }
}


void setDirty(boolean b)
{
   is_dirty = b;
   resetName();
   updateNatWidth();
   grow();
}


private void resetName()
{
   setText("");
   append(shown_text, _color);
   append(my_arrow, Color.black);
   if (is_dirty) append("*", Color.black);
}



/********************************************************************************/
/*										*/
/*	Resizing methods							*/
/*										*/
/********************************************************************************/

void shrink()
{
   setText("");
   append(" ...", Color.black);
   setSize(ELLIDED_WIDTH, getHeight());
   setCaretPosition(0);
}



void grow()
{
   resetName();
   setSize(nat_width, nat_height);
   setCaretPosition(0);
}



void updateNatWidth()
{
   Dimension d1 = getPreferredSize();
   if (getWidth() != 0 && !getText().equals(" ...")) nat_width = d1.width;
}



int addedWidthIfGrown()
{
   return (nat_width - getWidth());
}



/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override public void paint(Graphics g)
{
   super.paint(g);
   Graphics2D brush = (Graphics2D)g;
   if (rolled_over && package_name!=null){
      brush.setColor(BALE_CRUMB_ROLLOVER_COLOR);
      brush.fill(_oval);
   }
}




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

void handleRequest(){
   BudaRoot root = BudaRoot.findBudaRoot(_parent);
   Rectangle loc = BudaRoot.findBudaLocation(_parent);
   if (root == null || loc == null) return;
   if (!my_search_up) {
       root.createSearchBubble(new Point(loc.x+getWidthLocation(),
					    loc.y+this.getHeight()),
				  _parent.getProjectName(),package_name,false);
    }
   else root.hideSearchBubble();
   my_search_up = !my_search_up;
}



/********************************************************************************/
/*										*/
/*	Callback handlers							*/
/*										*/
/********************************************************************************/

private class Mouser extends MouseAdapter
{

   @Override public void mouseClicked(MouseEvent e) {
      if(package_name!=null) handleRequest();
   }

   @Override public void mouseEntered(MouseEvent e) {
      rolled_over = true;
      instOval(); //called at this point so the textfield is instantiated and at its correct size
      repaint();
   }

   @Override public void mouseExited(MouseEvent e) {
      rolled_over = false;
      my_search_up = false;
      repaint();
   }

}	// end of inner class Mouser



/********************************************************************************/
/*										*/
/*	Dummy caret for the component						*/
/*										*/
/********************************************************************************/

private static class CrumbCaret extends DefaultCaret {

   private static final long serialVersionUID = 1;


   CrumbCaret() {
      setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
      setVisible(false);
    }

   @Override protected void adjustVisibility(Rectangle r)		{ }

}	// end of inner class CrumbCaret




}	// end of class BaleCrumbBarComponent



/* end of BaleCrumbBarComponent.java */
