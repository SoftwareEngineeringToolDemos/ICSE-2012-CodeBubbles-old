/********************************************************************************/
/*										*/
/*		BeamFeedbackReport.java 					*/
/*										*/
/*	Bubbles Environment Auxillary & Missing items feedback report		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Yu Li			      */
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


package edu.brown.cs.bubbles.beam;


import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.bqst.BqstFactory;
import edu.brown.cs.bubbles.bqst.BqstPanel;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;

import javax.swing.*;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;



class BeamFeedbackReport implements BeamConstants, BudaConstants {



/********************************************************************************/
/*										*/
/* Private Storage								*/
/*										*/
/********************************************************************************/

private BudaRoot     for_root;



/********************************************************************************/
/*										*/
/* Constructors 								*/
/*										*/
/********************************************************************************/

BeamFeedbackReport(BudaRoot br)
{
   for_root = br;
}



/********************************************************************************/
/*										*/
/* Setup methods								*/
/*										*/
/********************************************************************************/

void addPanel()
{
   Icon chevron = BoardImage.getIcon("dropdown_chevron",BUDA_BUTTON_RESIZE_WIDTH,BUDA_BUTTON_RESIZE_HEIGHT);

   JButton btn = new JButton("<html><center>Feedback</center></html>", chevron);
   btn.setHorizontalTextPosition(AbstractButton.LEADING);
   Font ft = btn.getFont();
   ft = ft.deriveFont(10f);
   btn.setBackground(BUDA_BUTTON_PANEL_COLOR);
   btn.setMargin(BUDA_BUTTON_INSETS);
   btn.setFont(ft);
   btn.setOpaque(false);
   btn.addActionListener(new FeedbackReportListener());
   btn.setToolTipText("Report feedback");
   for_root.addButtonPanelButton(btn);

   if (isLiLaPresent()) {
      btn = new JButton("<html><center>Too Slow</center></html>", chevron);
      btn.setHorizontalTextPosition(AbstractButton.LEADING);
      btn.setBackground(BUDA_BUTTON_PANEL_COLOR);
      btn.setMargin(BUDA_BUTTON_INSETS);
      btn.setFont(ft);
      btn.setOpaque(false);
      btn.addActionListener(new AngryListener());
      btn.setToolTipText("Report the system running slowly");
      for_root.addButtonPanelButton(btn);
    }
}



/********************************************************************************/
/*										*/
/*	Callback to gather feedback report information				*/
/*										*/
/********************************************************************************/

private class FeedbackReportListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      BqstPanel form = makeReportFeedbackForm();
      form.display();
   }

}  // end of inner class FeedbackReportListener



/**
 * Create a feedback report form
 **/

private BqstPanel makeReportFeedbackForm()
{
   BqstPanel form = BqstFactory.createBqstPanel(for_root,"Code Bubbles User Feedback");
   Icon[] icons = new Icon[]{BoardImage.getIcon("face_smile.png"),BoardImage.getIcon("face_sad.png")};
   Icon[] selectedicons = new Icon[]{BoardImage.getIcon("face_smile_pressed.png"),BoardImage.getIcon("face_sad_pressed.png")};
   form.addMultiChoices("How do you feel about Code Bubble?", null, new String[]{"Good", "Bad"}, icons, selectedicons, false, true);
   form.addLongText("Feedback",
	  "(Any kind of feedback is welcome)",
	  true);
   form.setScreenshotFlag(false);

   return form;
}



/********************************************************************************/
/*										*/
/*	Callback for anger reports						*/
/*										*/
/********************************************************************************/

private static boolean isLiLaPresent()
{
   return ClassLoader.getSystemResource("usi/instrumentation/LiLa.class") != null;
}




private static class AngryListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      try {
	 Class<?> c = Class.forName("usi.bubbles.FlyBy");
	 Method m = c.getMethod("dumpStack");
	 m.invoke(null);
	 BoardLog.logI("BEAM","Anger report successful");
       }
      catch (Throwable t) {
	 BoardLog.logE("BEAM","Anger report failed",t);
       }
    }

}	// end of inner class AngryListener




}	// end of class BeamFeedbackReport



/* end of BeamFeedbackReport.java */




