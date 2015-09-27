/********************************************************************************/
/*										*/
/*		BddtFactory.java						*/
/*										*/
/*	Bubbles Environment dyanmic debugger tool factory and setup class	*/
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



package edu.brown.cs.bubbles.bddt;

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;


/**
 *	This class provides the entries for setting up and providing access to
 *	the various debugging bubbles and environment.
 **/

public class BddtFactory implements BddtConstants, BudaConstants.ButtonListener,
					BumpConstants, BaleConstants, BudaConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaChannelSet		debug_channels;
private BumpLaunchConfig	current_configuration;
private JLabel			launch_label;
private BudaRoot		buda_root;

private static BddtConsoleController console_controller;
private static BddtHistoryController history_controller;
private static BddtFactory	the_factory;
private static BoardProperties	bddt_properties;



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

/**
 *	This routine is called automatically at startup to initialize the module.
 **/

public static void setup()
{
   the_factory = new BddtFactory();
   console_controller = new BddtConsoleController();
   history_controller = new BddtHistoryController();

   bddt_properties = BoardProperties.getProperties("Bddt");

   BudaRoot.addBubbleConfigurator("BDDT",new BddtConfigurator());

   BudaRoot.registerMenuButton(BDDT_BREAKPOINT_BUTTON, the_factory);
   BudaRoot.registerMenuButton(BDDT_CONFIG_BUTTON,the_factory);
   BudaRoot.registerMenuButton(BDDT_PROCESS_BUTTON,the_factory);

   BddtRepository rep = new BddtRepository();
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_LAUNCH_CONFIG,rep);
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_EXPLORER,rep);
}


/**
 *	Return the singleton instance of the ddttext viewer factory.
 **/

public static BddtFactory getFactory()
{
   return the_factory;
}



public static void initialize(BudaRoot br)
{
   if (the_factory.current_configuration == null) {
      the_factory.setCurrentLaunchConfig(null);
    }

   the_factory.setupDebugging(br);
   
   BaleFactory.getFactory().addContextListener(new DebugContextListener());
}





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BddtFactory()
{
   debug_channels = null;
   // status_label = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BddtConsoleController getConsoleControl()		{ return console_controller; }
BddtHistoryController getHistoryControl()		{ return history_controller; }



/********************************************************************************/
/*										*/
/*	Methods to setup up a debugging process 				*/
/*										*/
/********************************************************************************/

public void newDebugger(BumpLaunchConfig blc)
{
   BudaBubbleArea bba = null;

   String label = blc.getProject() + " : " + blc.getConfigName();

   if (debug_channels.getNumChannels() == 1 && debug_channels.isChannelEmpty()) {
      bba = debug_channels.getBubbleArea();
      debug_channels.setChannelName(label);
    }
   else bba = debug_channels.addChannel(label);
   bba.setProperty("Bddt.debug",Boolean.TRUE);

   setCurrentLaunchConfig(blc);

   BddtLaunchControl ctrl = new BddtLaunchControl(blc);
   console_controller.setupConsole(ctrl);

   BudaBubblePosition bbp = BudaBubblePosition.MOVABLE;
   if (bddt_properties.getBoolean(BDDT_PROPERTY_FLOAT_LAUNCH_CONTROL)) {
      bbp = BudaBubblePosition.FLOAT;
    }
   bba.addBubble(ctrl,null,new Point(BDDT_LAUNCH_CONTROL_X,BDDT_LAUNCH_CONTROL_Y),
	 PLACEMENT_EXPLICIT,bbp);

   BudaRoot br = BudaRoot.findBudaRoot(bba);
   if (br == null) return;

   br.setCurrentChannel(ctrl);

   ctrl.setupKeys();
   ctrl.setupInitialBubbles();
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

BumpLaunchConfig getCurrentLaunchConfig()
{
   if (current_configuration == null) return current_configuration;

   return current_configuration;
}


void setCurrentLaunchConfig(BumpLaunchConfig blc)
{
   BumpRunModel brm = BumpClient.getBump().getRunModel();

   if (blc == null) {
      for (BumpLaunchConfig xlc : brm.getLaunchConfigurations()) {
	 if (!xlc.isWorkingCopy()) blc = xlc;
	 break;
       }
    }
   else {
      BumpLaunchConfig xblc = brm.getLaunchConfiguration(blc.getId());
      if (xblc != null) blc = xblc;
    }

   current_configuration = blc;
   if (launch_label != null && blc != null) {
      launch_label.setText(blc.getConfigName());
   }
}




private void setupDebugging(BudaRoot br)
{
   if (debug_channels != null) return;

   buda_root = br;

   String dflt = null;
   if (bddt_properties.getBoolean("Bddt.grow.down")) {
      // set up default bubble area to be larger vertically
    }

   debug_channels = new BudaChannelSet(br,BDDT_CHANNEL_TOP_COLOR,BDDT_CHANNEL_BOTTOM_COLOR,dflt);
   BudaBubbleArea bba = debug_channels.getBubbleArea();
   if (bba != null) bba.setProperty("Bddt.debug",Boolean.TRUE);

   SwingGridPanel pnl = new DebuggingPanel();

   JLabel top = new JLabel("Debug",JLabel.CENTER);
   pnl.addGBComponent(top,0,0,0,1,1,0);

   JButton btn = defineButton("debug","Switch to the debugging perspective");
   pnl.addGBComponent(btn,1,1,1,1,0,0);

   btn = defineButton("new","<html>Create a new debugging channel for current configuration" +
	 " or switch configurations (right click)");
   pnl.addGBComponent(btn,2,1,1,1,0,0);
   btn.addMouseListener(new ConfigSelector());

   launch_label = new JLabel();
   if (current_configuration != null) {
      launch_label.setText(current_configuration.getConfigName());
    }

   pnl.addGBComponent(launch_label,0,3,0,0,1,1);

   br.addPanel(pnl,true);

   // add a button to bring up the task bubble (F2?)
}



/********************************************************************************/
/*										*/
/*	Debugging panel 							*/
/*										*/
/********************************************************************************/

private static class DebuggingPanel extends SwingGridPanel
{
   private static final long serialVersionUID = 1;

   DebuggingPanel() {
      super();
    }

   protected void paintComponent(Graphics g0) {
      super.paintComponent(g0);
      Graphics2D g = (Graphics2D) g0.create();
      if (BDDT_PANEL_TOP_COLOR.equals(BDDT_PANEL_BOTTOM_COLOR)) {
	 g.setColor(BDDT_PANEL_TOP_COLOR);
       }
      else {
	 Paint p = new GradientPaint(0f, 0f, BDDT_PANEL_TOP_COLOR, 0f, this.getHeight(),
					BDDT_PANEL_BOTTOM_COLOR);
	 g.setPaint(p);
       }
      g.fillRect(0, 0, this.getWidth() , this.getHeight());
    }

}	// end of inner class DebuggingPanel



private JButton defineButton(String name,String info)
{
   JButton btn = new JButton(BoardImage.getIcon("debug/" + name + ".png"));
   btn.setToolTipText(info);
   btn.setActionCommand(name.toUpperCase());
   btn.setMargin(new Insets(0,1,0,1));
   btn.setOpaque(false);
   btn.setBackground(new Color(0,true));
   btn.addActionListener(new PanelHandler());

   return btn;
}



private class PanelHandler implements ActionListener {

   @Override public void actionPerformed(ActionEvent e) {
      JButton btn = (JButton) e.getSource();
      BudaRoot br = BudaRoot.findBudaRoot(btn);
      if (br == null) return;
      String cmd = e.getActionCommand();
      if (cmd.equals("DEBUG") && debug_channels.isChannelEmpty()) cmd = "NEW";
      if (cmd.equals("DEBUG")) {
	 if (current_configuration == null) {
	    createConfiguration();
	  }
	 else {
	    BoardMetrics.noteCommand("BDDT","GotoDebug");
	    if (br.getChannelSet() == debug_channels) br.setChannelSet(null);
	    else br.setChannelSet(debug_channels);
	  }
       }
      else if (cmd.equals("NEW")) {
	 if (current_configuration == null) setCurrentLaunchConfig(null);

	 if (current_configuration != null) {
	    BoardMetrics.noteCommand("BDDT","NewDebug");
	    newDebugger(current_configuration);
	  }
	 else {
	    createConfiguration();
	  }
       }
    }

   private void createConfiguration() {
      CreateConfigAction cca = new CreateConfigAction(BumpLaunchConfigType.JAVA_APP);
      ActionEvent act = new ActionEvent(this,0,"NEW");
      cca.actionPerformed(act);
    }

}	// end of inner class PanelHandler



/********************************************************************************/
/*										*/
/*	Bubble making methods (for non-debug mode)				*/
/*										*/
/********************************************************************************/

BudaBubble makeConsoleBubble(BudaBubble src,BumpProcess proc)
{
   if (proc == null) return null;

   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(src);
   if (bba == null) return null;

   BudaBubble bb = null;
   bb = console_controller.createConsole(proc);
   Rectangle r = src.getBounds();
   int x = r.x;
   int y = r.y + r.height + 20;

   bba.addBubble(bb,BudaBubblePosition.MOVABLE,x,y);

   return bb;
}




/********************************************************************************/
/*										*/
/*	Button handling 							*/
/*										*/
/********************************************************************************/

@Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt)
{
   BudaRoot br = BudaRoot.findBudaRoot(bba);
   BudaBubble bb = null;

   if (id.equals(BDDT_CONFIG_BUTTON)) {
      bb = new BddtConfigView();
    }
   else if (id.equals(BDDT_PROCESS_BUTTON)) {
      bb = new BddtProcessView();
   }
   else if (id.equals(BDDT_BREAKPOINT_BUTTON)) {
      bb = new BddtBreakpointBubble();
    }

   if (br != null && bb != null) {
      BudaConstraint bc = new BudaConstraint(pt);
      br.add(bb,bc);
      bb.grabFocus();
    }
}




/********************************************************************************/
/*										*/
/*	Button actions for selecting configurator				*/
/*										*/
/********************************************************************************/

void addNewConfigurationActions(JPopupMenu menu)
{
   switch (BoardSetup.getSetup().getLanguage()) {
      case JAVA :
	 menu.add(new CreateConfigAction(BumpLaunchConfigType.JAVA_APP));
	 menu.add(new CreateConfigAction(BumpLaunchConfigType.REMOTE_JAVA));
	 break;
      case PYTHON :
	 menu.add(new CreateConfigAction(BumpLaunchConfigType.PYTHON));
	 break;
      case JS :
         menu.add(new CreateConfigAction(BumpLaunchConfigType.JS));
         break;
      case REBUS :
	 break;
    }
}



private class ConfigSelector extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON3) {
	 JPopupMenu menu = new JPopupMenu();
	 BumpClient bc = BumpClient.getBump();
	 BumpRunModel bm = bc.getRunModel();
	 Collection<BumpLaunchConfig> blcs = new TreeSet<BumpLaunchConfig>(new ConfigComparator());
	 for (BumpLaunchConfig blc : bm.getLaunchConfigurations()) {
	    if (!blc.isWorkingCopy()) blcs.add(blc);
	  }
	 for (BumpLaunchConfig blc : blcs) {
	    menu.add(new ConfigAction(blc));
	  }
	 addNewConfigurationActions(menu);
	 menu.show((Component) e.getSource(),e.getX(),e.getY());
       }
    }

}	// end of inner class ConfigSelector




private class ConfigAction extends AbstractAction {

   private BumpLaunchConfig for_config;

   ConfigAction(BumpLaunchConfig blc) {
      super(blc.getConfigName());
      for_config = blc;
    }

   @Override public void actionPerformed(ActionEvent e) {
      setCurrentLaunchConfig(for_config);
      BoardMetrics.noteCommand("BDDT","GoToDebug");
      newDebugger(for_config);
    }

}	// end of inner class ConfigAction




private static class CreateConfigAction extends AbstractAction {

   private BumpLaunchConfigType config_type;

   CreateConfigAction(BumpLaunchConfigType typ) {
      super("Create New " + typ.getEclipseName() + " Configuration");
      config_type = typ;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BumpClient bc = BumpClient.getBump();
      BumpRunModel brm = bc.getRunModel();
      BumpLaunchConfig blc = brm.createLaunchConfiguration(null,config_type);
      if (blc != null) {
	 blc = blc.save();
	 BddtLaunchBubble bb = new BddtLaunchBubble(blc);
	 BudaBubbleArea bba = the_factory.buda_root.getCurrentBubbleArea();
	 bba.addBubble(bb,null,null,PLACEMENT_NEW|PLACEMENT_USER);
       }
    }

}	// end of inner class CreateConfigAction



private static class ConfigComparator implements Comparator<BumpLaunchConfig> {

   @Override public int compare(BumpLaunchConfig l1,BumpLaunchConfig l2) {
      return l1.getConfigName().compareTo(l2.getConfigName());
    }

}	// end of inner class ConfigComparator




/********************************************************************************/
/*										*/
/*	General context commands for debugging					*/
/*										*/
/********************************************************************************/

private static class DebugContextListener implements BaleContextListener {

   @Override public BudaBubble getHoverBubble(BaleContextConfig cfg) {
      return null;
    }

   @Override public void addPopupMenuItems(BaleContextConfig cfg,JPopupMenu m) {
      if (cfg.inAnnotationArea()) {
	 // check if line has code that can be breakpointed
	 m.add(new BreakpointAction(cfg,true));
	 m.add(new BreakpointAction(cfg,false));
       }
    }

   @Override public String getToolTipHtml(BaleContextConfig cfg) {
      return null;
    }

}	// end of inner class EditorContextListener



private static class BreakpointAction extends AbstractAction {

   private BumpBreakMode break_mode;
   private BaleContextConfig bale_context;

   BreakpointAction(BaleContextConfig cfg,boolean brk) {
      super(brk ? "Toggle Breakpoint" : "Toggle Tracepoint");
      break_mode = (brk ? BumpBreakMode.DEFAULT : BumpBreakMode.TRACE);
      bale_context = cfg;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BumpBreakModel mdl = BumpClient.getBump().getBreakModel();
      mdl.toggleBreakpoint(bale_context.getEditor().getContentProject(),
	    bale_context.getEditor().getContentFile(),
	    bale_context.getLineNumber(),break_mode);
      BoardMetrics.noteCommand("BDDT","ANNOT_" + e.getActionCommand());
    }

}	// end of inner class BreakpointAction





}	// end of class BddtFactory



/* end of BddtFactory.java */
