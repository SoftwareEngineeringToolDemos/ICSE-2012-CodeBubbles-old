/********************************************************************************/
/*										*/
/*		BudaToolTip							*/
/*										*/
/*	BUblles Display Area tool tip with html smart sizing			*/
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



package edu.brown.cs.bubbles.buda;

import javax.swing.JLabel;
import javax.swing.JToolTip;
import javax.swing.text.View;

import java.awt.Dimension;





public class BudaToolTip extends JToolTip
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static final int TIP_WIDTH = 800;


private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Sizing routines 							*/
/*										*/
/********************************************************************************/

@Override public Dimension getPreferredSize()
{
   String txt = getTipText();

   if (txt == null || txt.length() == 0) return super.getPreferredSize();
   if (!txt.startsWith("<html>")) return super.getPreferredSize();
   // txt = txt.replace("\t","");

   JLabel lbl = new JLabel();
   lbl.setFont(getFont());
   lbl.setText(txt);
   Dimension d0 = lbl.getPreferredSize();
   if (d0.width < TIP_WIDTH) return super.getPreferredSize();

   View v = (View) lbl.getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey);
   int w0 = TIP_WIDTH;
   v.setSize(w0,0);
   float w = v.getPreferredSpan(View.X_AXIS);
   float h = v.getPreferredSpan(View.Y_AXIS);

   float wmin = v.getMinimumSpan(View.X_AXIS);
   float wmax = w;
   while (wmax - wmin > 1) {
      w = (wmin + wmax) / 2;
      v.setSize(w,0);
      float h1 = v.getPreferredSpan(View.Y_AXIS);
      if (h1 <= h) {
	 wmax = w;
       }
      else {
	 wmin = w;
       }
    }
   w = Math.round(wmin);
   v.setSize(w,0);
   h = v.getPreferredSpan(View.Y_AXIS);

   // w = v.getMinimumSpan(View.X_AXIS)+16;

   Dimension d = new Dimension((int) Math.ceil(w+10),(int) Math.ceil(h+5));

   return d;
}



}	// end of class BudaToolTip




/* end of BudaToolTip.java */
