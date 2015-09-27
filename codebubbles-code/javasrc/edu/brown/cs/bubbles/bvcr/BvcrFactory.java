/********************************************************************************/
/*										*/
/*		BvcrFactory.java						*/
/*										*/
/*	Bubble Automated Testing Tool factory class for bubbles integration	*/
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


package edu.brown.cs.bubbles.bvcr;

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bass.*;
import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.mint.*;

import org.w3c.dom.Element;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class BvcrFactory implements BvcrConstants, BaleConstants, MintConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private boolean 		server_running;
private boolean                 server_started;

private static BvcrFactory	the_factory = null;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public synchronized static BvcrFactory getFactory()
{
   if (the_factory == null) the_factory = new BvcrFactory();
   return the_factory;
}


private BvcrFactory()
{
   server_running = false;
   server_started = false;
   new BvcrFileManager();
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

public static void setup()
{
   // work is done by the static initializer
}



public static void initialize(BudaRoot br)
{
   BoardThreadPool.start(new BvcrStarter()); 
   // getFactory().startBvcrServer();
   getFactory().setupCallbacks();
}


private void setupCallbacks()
{
   BvcrContexter bc = new BvcrContexter();

   BaleFactory.getFactory().addContextListener(bc);
   BassFactory.getFactory().addPopupHandler(bc);
}




/********************************************************************************/
/*										*/
/*	Bvcr agent setup							*/
/*										*/
/********************************************************************************/

Element getChangesForFile(String proj,String file)
{
   if (!server_running) return null;

   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   MintDefaultReply rply = new MintDefaultReply();
   String cmd = "<BVCR DO='FINDCHANGES'";
   cmd += " PROJECT='" + proj + "'";
   cmd += " FILE='" + file + "' />";
   mc.send(cmd,rply,MINT_MSG_FIRST_NON_NULL);
   Element e = rply.waitForXml();

   // This should use e to get the set of lines changed in the original version
   // and then map these to lines changed in the users version
   // It should return a change structure that encompasses these changes

   return e;
}



Element getHistoryForFile(String proj,String file)
{
   if (!server_running) return null;

   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   MintDefaultReply rply = new MintDefaultReply();
   String cmd = "<BVCR DO='HISTORY'";
   cmd += " PROJECT='" + proj + "'";
   cmd += " FILE='" + file + "' />";
   mc.send(cmd,rply,MINT_MSG_FIRST_NON_NULL);
   Element e = rply.waitForXml();

   return e;
}



Element getFileDifferences(String proj,String file,String v0,String v1)
{
   if (!server_running) return null;

   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   MintDefaultReply rply = new MintDefaultReply();
   String cmd = "<BVCR DO='FILEDIFFS'";
   cmd += " PROJECT='" + proj + "'";
   cmd += " FILE='" + file + "'";
   if (v0 != null) cmd += " FROM='" + v0 + "'";
   if (v1 != null) cmd += " TO='" + v1 + "'";
   cmd += " />";
   mc.send(cmd,rply,MINT_MSG_FIRST_NON_NULL);
   Element e = rply.waitForXml();

   return e;
}





/********************************************************************************/
/*										*/
/*	Server code								*/
/*										*/
/********************************************************************************/

void startBvcrServer()
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   IvyExec exec = null;

   synchronized (this) {
      if (server_running || server_started) return;

      long mxmem = Runtime.getRuntime().maxMemory();
      mxmem = Math.min(512*1024*1024L,mxmem);

      List<String> args = new ArrayList<String>();
      args.add("java");
      args.add("-Xmx" + Long.toString(mxmem));
      args.add("-cp");
      args.add(System.getProperty("java.class.path"));
      args.add("edu.brown.cs.bubbles.bvcr.BvcrMain");
      args.add("-S");
      args.add("-m");
      args.add(bs.getMintName());

      for (int i = 0; i < 100; ++i) {
	 MintDefaultReply rply = new MintDefaultReply();
	 mc.send("<BVCR DO='PING' />",rply,MINT_MSG_FIRST_NON_NULL);
	 String rslt = rply.waitForString(1000);
	 if (rslt != null) {
	    server_running = true;
	    break;
	  }
	 if (i == 0) {
	    try {
	       exec = new IvyExec(args,null,IvyExec.ERROR_OUTPUT);
               server_started = true;
	       BoardLog.logD("BVCR","Run " + exec.getCommand());
	    }
	    catch (IOException e) {
	       break;
	     }
	  }
	 else {
	    try {
	       if (exec != null) {
		  // check if process exited (nothing to do)
		  exec.exitValue();
		  break;
		}
	     }
	    catch (IllegalThreadStateException e) { }
	  }

	 try {
	    wait(2000);
	  }
	 catch (InterruptedException e) { }
       }
      if (!server_running) {
	 BoardLog.logE("BVCR","Unable to start bvcr server: " + args);
       }
    }
}

private static class BvcrStarter implements Runnable {
   
   @Override public void run() {
      getFactory().startBvcrServer();
    }
   
}       // end of inner class BvcrStarter


/********************************************************************************/
/*										*/
/*	Action for showing method history					*/
/*										*/
/********************************************************************************/

private class BvcrContexter implements BaleContextListener, BassConstants.BassPopupHandler {

   @Override public BudaBubble getHoverBubble(BaleContextConfig cfg) {
      return null;
    }

   @Override public String getToolTipHtml(BaleContextConfig cfg) {
      return null;
    }

   @Override public void addPopupMenuItems(BaleContextConfig cfg,JPopupMenu menu) {
      // only if bvcr is running (i.e. under version management)
      menu.add(new HistoryAction(cfg));
      menu.add(new DiffAction(cfg));
    }

   @Override public void addButtons(BudaBubble bb,Point where,JPopupMenu menu,String name,
		BassName forname) {
      BumpLocation loc = null;
      if (forname == null) {
	 String proj = null;
	 int idx = name.indexOf(":");
	 if (idx > 0) {
	    proj = name.substring(0,idx);
	    name = name.substring(idx+1);
	 }
	 List<BumpLocation> locs = null;
	 if (name.length() > 0)
	    locs = BumpClient.getBump().findClassDefinition(proj,name);
	 if (locs != null && locs.size() > 0) {
	    loc = locs.get(0);
	 }
      }
      else {
	 switch (forname.getNameType()) {
	    case FILE :
	    case CLASS :
	       loc = forname.getLocation();
	       break;
	    default :
	       break;
	 }
      }
      if (loc != null) {
	 if (loc.getProject() != null && loc.getFile() != null) {
	    menu.add(new HistoryAction(bb,where,loc));
	    menu.add(new DiffAction(bb,where,loc));
	 }
      }
    }

}	// end of inner class BvcrContexter


private class HistoryAction extends AbstractAction {

   private BvcrHistoryDisplay history_manager;

   private static final long serialVersionUID = 1;

   HistoryAction(BaleContextConfig cfg) {
      super("Investigate Code History");
      history_manager = new BvcrHistoryDisplay(cfg);
    }

   HistoryAction(BudaBubble bb,Point where,BumpLocation loc) {
      super("Investigate File History");
      history_manager = new BvcrHistoryDisplay(bb,where,loc);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BudaRoot.hideSearchBubble(evt);
      history_manager.process();
    }

}	// end of inner class HistoryAction



private class DiffAction extends AbstractAction {

   private BvcrDiffViewer history_manager;

   private static final long serialVersionUID = 1;

   DiffAction(BaleContextConfig cfg) {
      super("Show Version Differences");
      history_manager = new BvcrDiffViewer(cfg);
    }

   DiffAction(BudaBubble bb,Point where,BumpLocation loc) {
      super("Show Version Differences");
      history_manager = new BvcrDiffViewer(bb,where,loc);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BudaRoot.hideSearchBubble(evt);
      history_manager.process();
    }

}	// end of inner class HistoryAction



}	// end of class BvcrFactory




/* end of BvcrFactory.java */



