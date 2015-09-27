/********************************************************************************/
/*                                                                              */
/*              BeamEmailBugReport.java                                         */
/*                                                                              */
/*      description of class                                                    */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.beam;

import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;

import javax.swing.*;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;


class BeamEmailBugReport implements BeamConstants, BudaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BudaRoot                for_root;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BeamEmailBugReport(BudaRoot br)
{
   for_root = br;
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void addPanel()
{
   Icon chevron = BoardImage.getIcon("dropdown_chevron",BUDA_BUTTON_RESIZE_WIDTH, BUDA_BUTTON_RESIZE_HEIGHT);
   JButton btn = new JButton("<html><center>Email&nbsp;Bug</center></html>", chevron);
   btn.setHorizontalTextPosition(AbstractButton.LEADING);
   Font ft = btn.getFont();
   ft = ft.deriveFont(10f);
   btn.setBackground(BUDA_BUTTON_PANEL_COLOR);
   btn.setMargin(BUDA_BUTTON_INSETS);
   btn.setFont(ft);
   btn.setOpaque(false);
   
   btn.addActionListener(new BugReportListener());
   btn.setToolTipText("Report a bubbles bug");
   for_root.addButtonPanelButton(btn);
}




/********************************************************************************/
/*                                                                              */
/*      Callback to set up email message                                        */
/*                                                                              */
/********************************************************************************/

private class BugReportListener implements ActionListener {
   
   @Override public void actionPerformed(ActionEvent evt) {
      String addr = "spr+bubblesbug@cs.brown.edu";
      String subj = "Bubbles bug report";
      
      StringBuffer body = new StringBuffer();
      body.append("Description of the problem:\n\n\n\n");
      body.append("Severity of the problem: \n\n\n");
      body.append("Log data:\n");
      File lf1 = BoardLog.getBubblesLogFile();
      addToOutput("  ",body,lf1);
      File lf2 = BoardLog.getBedrockLogFile();
      addToOutput("  ",body,lf2);
      BoardMetrics.getLastCommands("  ",body);
      
      BeamFactory.sendMail(addr,subj,body.toString());
    }
   
   private void addToOutput(String pfx,Appendable buf,File f) {
      if (f == null) return;
      String [] lns = new String[10];
      int ct = 0;
      try {
         BufferedReader br = new BufferedReader(new FileReader(f));
         for ( ; ; ) {
            String ln = br.readLine();
            if (ln == null) break;
            lns[ct%lns.length] = ln;
            ++ct;
          }
         br.close();
         for (int i = 0; i < lns.length; ++i) {
            int idx = (ct+i)%lns.length;
            if (lns[idx] != null) {
               if (pfx != null) buf.append(pfx);
               buf.append(lns[idx]);
               buf.append("\n");
             }
          }
       }
      catch (IOException e) { }
    }
   
}       // end of inner class BugReportListener




}       // end of class BeamEmailBugReport




/* end of BeamEmailBugReport.java */

