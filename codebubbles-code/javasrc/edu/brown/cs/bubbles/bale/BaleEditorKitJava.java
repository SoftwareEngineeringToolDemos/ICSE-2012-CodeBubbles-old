/********************************************************************************/
/*										*/
/*		BaleEditorKitJava.java						*/
/*										*/
/*	Bubble Annotated Language Editor editor kit for Java-specific cmds	*/
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


package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.bump.BumpClient;

import javax.swing.Action;
import javax.swing.text.Keymap;
import javax.swing.text.TextAction;

import java.awt.event.ActionEvent;



class BaleEditorKitJava implements BaleConstants, BaleConstants.BaleLanguageKit
{




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private static final Action open_eclipse_editor_action = new OpenEclipseEditorAction();

private static final Action [] local_actions = {
   open_eclipse_editor_action
};




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleEditorKitJava()
{
}



/********************************************************************************/
/*										*/
/*	Action Methods								*/
/*										*/
/********************************************************************************/

@Override public Action [] getActions()
{
   return local_actions;
}


@Override public Keymap getKeymap(Keymap base)
{
   return base;
}



/********************************************************************************/
/*										*/
/*	Actions to talk to Eclipse						*/
/*										*/
/********************************************************************************/

private static class OpenEclipseEditorAction extends TextAction {

   private static final long serialVersionUID = 1;

   OpenEclipseEditorAction() {
      super("OpenEclipseEditorAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = BaleEditorKit.getBaleEditor(e);
      if (!BaleEditorKit.checkReadEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      int soff = target.getSelectionStart();

      BumpClient bc = BumpClient.getBump();
      BoardMetrics.noteCommand("BALE","JumpToEclipse");

      bc.openEclipseEditor(bd.getProjectName(),
			      bd.getFile(),
			      bd.findLineNumber(soff));
    }

}	// end of inner class OpenEclipseEditorAction




}	// end of class BaleEditorKitJava



/* end of BaleEditorKitJava.java */
