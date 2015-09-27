/********************************************************************************/
/*										*/
/*		BoardMetrics.java						*/
/*										*/
/*	Bubbles attribute and property management metric gathering		*/
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



package edu.brown.cs.bubbles.board;

import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.*;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


/**
 *	This class is used to gather and report various metrics about the
 *	use of code bubbles to effect appropriate user studies and system
 *	improvements.
 **/

public class BoardMetrics implements BoardConstants {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private JFrame root_window;

private boolean collect_screens;
private boolean collect_experience;
private boolean collect_active;
private boolean collect_commands;
private boolean collect_eclipse;
private boolean collect_dumps;
private boolean collect_monitorlog;
private boolean collect_options;
private boolean collect_workingset;

private String	user_id;
private File	eclipse_file;
private long	eclipse_length;
private long	monitor_length;
private long	start_time;
private List<String> command_data;

private long	last_options;
private long	last_active;
private long	last_screen;
private long	total_active;

private long	next_screen;
private long	next_command;
private long	next_feedback;
private long	next_eclipse;
private long	next_options;
private long	next_workingset;
private long	next_monitorlog;

private boolean default_metrics;

private boolean do_dump;

private static BoardMetrics	the_metrics = new BoardMetrics();
private static String		run_id;
private static Set<String>	bugs_reported = new HashSet<String>();

private static final int BLUR_SIZE = 8;
private static final float [] BLUR_MATRIX;


static {
   run_id = Integer.toString(new Random().nextInt(1000000));

   BLUR_MATRIX = new float[BLUR_SIZE * BLUR_SIZE];
   for (int i = 0; i < BLUR_MATRIX.length; ++i) {
      BLUR_MATRIX[i] = 1.0f / BLUR_MATRIX.length;
    }
}


private static final String [] check_properties = {
   BOARD_METRIC_PROP_SCREENS,
   BOARD_METRIC_PROP_EXPERIENCE,
   BOARD_METRIC_PROP_ACTIVE,
   BOARD_METRIC_PROP_COMMANDS,
   BOARD_METRIC_PROP_ECLIPSE,
   BOARD_METRIC_PROP_DUMPS,
   BOARD_METRIC_PROP_OPTIONS,
   BOARD_METRIC_PROP_WORKINGSET,
   BOARD_METRIC_PROP_WORKINGSET
};




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BoardMetrics()
{
   root_window = null;
   collect_screens = false;
   collect_experience = false;
   collect_active = false;
   collect_commands = false;
   collect_eclipse = false;
   collect_monitorlog = false;
   collect_dumps = true;
   collect_options = true;
   collect_workingset = true;
   user_id = null;
   eclipse_file = null;
   eclipse_length = -1;
   monitor_length = -1;
   start_time = System.currentTimeMillis();
   last_active = start_time;
   last_screen = 0;
   last_options = start_time;
   command_data = null;
   do_dump = true;
   total_active = 0;
   next_screen = 0;
   next_command = 0;
   next_feedback = 0;
   next_eclipse = 0;
   next_options = 0;
   next_workingset = 0;
   next_monitorlog = 0;

   BoardProperties bp = BoardProperties.getProperties("Board");
   default_metrics = bp.getBoolean(BOARD_PROP_DEFAULT_METRICS,true);
}



/********************************************************************************/
/*										*/
/*	External entries							*/
/*										*/
/********************************************************************************/

/**
 *	Called to setup metric gathering and reporting.  No metrics will be
 *	generated without this call.  The first time metrics are used, this
 *	call will let the user opt-out of any of the different metric types.
 **/

public static void setupMetrics(boolean force)
{
   the_metrics.setupOptions(force);
}


/**
 *	Called to set the root window so that screen dumps can be captured
 *	for metrics evaluation.
 **/

public static void setRootWindow(JFrame root)
{
   the_metrics.root_window = root;
}



/**
 *	Called to note that the user is active at this time.
 **/

public static void noteActive(long when)
{
   the_metrics.setActive(when);
}



/**
 *	Notes the issuance of a command from the given source. The command
 *	argument describes the command.
 **/

public static void noteCommand(String src,String cmd)
{
   if (the_metrics.collect_commands)
      the_metrics.saveCommand(src,cmd,System.currentTimeMillis());
}



/**
 *	Called when the user explicit asks to provide feedback on their use
 *	of the system.
 **/

public static void provideFeedback()
{
   // TODO: implement user feedback request
}

public static void getLastCommands(String pfx,Appendable buf)
{
   if (the_metrics.command_data == null) return;
   synchronized (the_metrics.command_data) {
      int ln = the_metrics.command_data.size();
      int start = Math.max(0,ln-10);
      for (int i = start; i < ln; ++i) {
         try {
            if (pfx != null) buf.append(pfx);
            buf.append(the_metrics.command_data.get(i));
            buf.append("\n");
          }
         catch (IOException e) { }
       }
    }
   try {
      buf.append("\n");
    }
   catch (IOException e) { }
}


/**
 *	Force metrics to be dumped immediately
 **/

public static void forceDump()
{
   the_metrics.dumpAll();
}




/********************************************************************************/
/*										*/
/*	Bug report handling							*/
/*										*/
/********************************************************************************/

static void generateBugReport(String src,String msg,Throwable t)
{
   if (!the_metrics.collect_dumps) return;

   StringWriter sw = new StringWriter();
   PrintWriter spw = new PrintWriter(sw);
   t.printStackTrace(spw);
   String trace = sw.toString();

   generateBugReport(src,msg,t.toString(),trace);
}



static void generateBugReport(String src,String msg,String id,String trace)
{
   if (!the_metrics.collect_dumps) return;

   String key = src + "@" + msg + "@" + id;
   if (!bugs_reported.add(key)) return;

   File f = null;
   String ver = BoardSetup.getVersionData();

   try {
      f = File.createTempFile("BoardMetrics_BUG_",".xml");
      IvyXmlWriter xw = new IvyXmlWriter(f);
      xw.begin("BUG");
      Date d = new Date();
      xw.field("DATE",d.toString());
      xw.field("TIME",d.getTime());
      xw.field("VERSION",ver);
      xw.field("SOURCE",src);
      xw.field("USER",the_metrics.user_id);
      xw.field("USERNAME",System.getProperty("user.name"));
      xw.field("OS",System.getProperty("os.name"));
      xw.field("ARCH",System.getProperty("os.arch"));
      xw.field("JVER",System.getProperty("java.version"));
      xw.field("JVEN",System.getProperty("java.vendor"));
      xw.field("JVMN",System.getProperty("java.vm.version"));
      xw.field("JVMV",System.getProperty("java.vm.vendor"));
      xw.field("JCLV",System.getProperty("java.class.version"));
      xw.field("JCOM",System.getProperty("java.compiler"));
      xw.textElement("MESSAGE",msg);
      xw.textElement("EXCEPTION",id);
      xw.textElement("STACK",trace);
      the_metrics.addBugCommands(xw);
      if (src != null && src.equals("BEDROCK")) {
	 the_metrics.addBugMonitor(xw);
       }
      xw.end("BUG");
      xw.close();

      new BoardUpload(f,"BUGS","AUTO");
    }
   catch (IOException e) { }
   finally {
      if (f != null) f.delete();
    }
}




/********************************************************************************/
/*										*/
/*	Methods to set user opt-in/out options					*/
/*										*/
/********************************************************************************/

private void setupOptions(boolean force)
{
   BoardProperties bp = BoardProperties.getProperties("Metrics");
   user_id = bp.getProperty(BOARD_METRIC_PROP_USERID);
   if (user_id == null) {
      if (bp.getBoolean(BOARD_METRIC_PROP_AUTOID)) {
	 user_id = getUserId();
	 bp.setProperty(BOARD_METRIC_PROP_USERID,user_id);
	 force = false;
	 try {
	    bp.save();
	  }
	 catch (IOException e) { }
      }
   }

   BoardLog.logD("BOARD","Metrics setup for user " + user_id);

   boolean newv = default_metrics;
   if (user_id != null) newv = true;

   if (!force) {
      // ensure user is always asked about new properties
      for (String s : check_properties) {
	 if (!bp.containsKey(s)) force = true;
       }
    }

   collect_screens = bp.getBoolean(BOARD_METRIC_PROP_SCREENS,newv);
   collect_experience = bp.getBoolean(BOARD_METRIC_PROP_EXPERIENCE,newv);
   collect_active = bp.getBoolean(BOARD_METRIC_PROP_ACTIVE,newv);
   collect_commands = bp.getBoolean(BOARD_METRIC_PROP_COMMANDS,newv);
   collect_eclipse = bp.getBoolean(BOARD_METRIC_PROP_ECLIPSE,newv);
   collect_dumps = bp.getBoolean(BOARD_METRIC_PROP_DUMPS,newv);
   collect_options = bp.getBoolean(BOARD_METRIC_PROP_OPTIONS,newv);
   collect_workingset = bp.getBoolean(BOARD_METRIC_PROP_WORKINGSET,newv);
   collect_monitorlog = bp.getBoolean(BOARD_METRIC_PROP_WORKINGSET, newv);

   if (user_id == null || force) {
      requestOptions(bp);
    }

   setActive();

   synchronized (this) {
      if (collect_screens) next_screen = SCREEN_DUMP_TIME;
      setupCommandData();
      next_command = COMMAND_DUMP_TIME;
      if (collect_experience) next_feedback = USER_FEEDBACK_TIME;
      if (collect_eclipse) next_eclipse = ECLIPSE_DUMP_TIME;
      if (collect_options) next_options = OPTIONS_DUMP_TIME;
      if (collect_workingset) next_workingset = WORKINGSET_DUMP_TIME;
      if (collect_monitorlog) next_monitorlog = MONITORLOG_DUMP_TIME;
    }

   Runtime.getRuntime().addShutdownHook(new DumpAllTask());
}



private void requestOptions(BoardProperties bp)
{
   MetricsDialog dlg = new MetricsDialog(bp);
   dlg.pack();
   dlg.setVisible(true);

   //if user close the dialog box, generate a random user id.
   if (user_id == null || user_id.trim().equals("")) {
      user_id = getUserId();
      bp.setProperty(BOARD_METRIC_PROP_USERID, user_id);
   }

   try {
      bp.save();
    }
   catch (IOException e) { }
}



private String getUserId()
{
   String unm = System.getProperty("user.name");
   String hnm = IvyExecQuery.computeHostName();
   if (hnm == null) hnm = IvyExecQuery.getHostName();
   if (unm == null) unm = "USER";
   if (hnm == null) hnm = "HOST";

   byte [] drslt;

   try {
      MessageDigest mdi = MessageDigest.getInstance("MD5");
      mdi.update(unm.getBytes());
      mdi.update(hnm.getBytes());
      drslt = mdi.digest();
    }
   catch (NoSuchAlgorithmException e) {
      BoardLog.logE("BOARD","Problem creating user name: " + e);
      return unm + "@" + hnm;
    }

   int rslt = 0;
   for (int i = 0; i < drslt.length; ++i) {
      int j = i % 4;
      rslt ^= (drslt[i] << (j*8));
    }
   rslt &= 0x7fffffff;

   String pfx = "U";
   String cnm = BoardSetup.getSetup().getCourseName();
   if (cnm != null) {
      BoardProperties bp = BoardProperties.getProperties("Board");
      String xpfx = bp.getProperty("Board.prefix." + cnm);
      if (xpfx != null) pfx = xpfx;
    }
   else if (BoardSetup.getSetup().getLanguage() == BoardLanguage.REBUS) {
      pfx = "R";
    }

   return pfx + Integer.toString(rslt);
}




/********************************************************************************/
/*										*/
/*	Class to create a metrics setting up dialog box 			*/
/*										*/
/********************************************************************************/

private class MetricsDialog extends JDialog implements ActionListener, CaretListener
{

   private BoardProperties board_properties;
   // private JCheckBox study_checkbox;
   private JCheckBox scrns_checkbox;
   private JCheckBox exprn_checkbox;
   private JCheckBox activ_checkbox;
   private JCheckBox cmmds_checkbox;
   private JCheckBox optns_checkbox;
   private JCheckBox eclps_checkbox;
   private JCheckBox wrkst_checkbox;
   private JCheckBox mnlog_checkbox;
   private JCheckBox dumps_checkbox;
   // private JTextField userid_textfield;
   // private JButton ok_button;

   private static final long serialVersionUID = 1L;

   MetricsDialog(BoardProperties bp) {
      super(root_window,"User Metrics Options",true);

      board_properties = bp;
      SwingGridPanel pnl = new SwingGridPanel();
      pnl.beginLayout();
      pnl.addBannerLabel("User Metrics Opt-In Options");
      pnl.addSeparator();

      String comment = "<html>Code Bubbles can periodically collect information about ";
      comment += "the use of the system.  This data will provide a basis for improving ";
      comment += "the system and might be used for research purposes.\n";
      comment += "<p>All information collected is anonymous and can not be used to identify ";
      comment += "you or your project.  It also does not contain any information about the ";
      comment += "code you are working on or even the file names.";
      comment += "<p>To reset these options, you can either remove the file ";
      comment += "<i>~/.bubbles/Metrics.props</i> ";
      comment += "or start bubbles with the <i>-collect</i> option";
      comment += "<p>For more information, contact Steven Reiss (spr@cs.brown.edu).";
      JEditorPane ep = new JEditorPane("text/html",comment);
      ep.setEditable(false);
      Dimension sz = new Dimension(350,280);
      ep.setSize(sz);
      ep.setPreferredSize(sz);
      pnl.addLabellessRawComponent("COMMENT",ep);

      scrns_checkbox = pnl.addBoolean("Collect Periodic Blurred Screen Shots",collect_screens,null);
      exprn_checkbox = pnl.addBoolean("Periodically ask for User Experiences",collect_experience,null);
      activ_checkbox = pnl.addBoolean("Determine % time environment is used",collect_active,null);
      cmmds_checkbox = pnl.addBoolean("Collect command execution data",collect_commands,null);
      optns_checkbox = pnl.addBoolean("Collect Options file updates",collect_options,null);
      wrkst_checkbox = pnl.addBoolean("Collect Working Sets",collect_workingset,null);
      eclps_checkbox = pnl.addBoolean("Collect Eclipse usage data",collect_eclipse,null);
      mnlog_checkbox = pnl.addBoolean("Collect Eclipse interactions log",collect_monitorlog,null);
      pnl.addSeparator();
      dumps_checkbox = pnl.addBoolean("Send Automatic bug reports",collect_dumps,null);

      pnl.addSeparator();
      // pnl.addSectionLabel("<html>Name is required if you are participating in the user study<br></font></html>");
      // userid_textfield = pnl.addTextField("First name and your last name's initial: ", user_id, null, null);
      // userid_textfield.addCaretListener(this);
      // study_checkbox = pnl.addBoolean("I am not participating in the user study",false, this);
      // pnl.addSeparator();
      pnl.addBottomButton("Cancel","CANCEL",this);
      pnl.addBottomButton("OK","OK", this);
      // ok_button = pnl.addBottomButton("OK","OK", this);
      // if (user_id == null || user_id.equals("")) ok_button.setEnabled(false);
      pnl.addBottomButtons();

      this.setContentPane(pnl);
    }

   @Override public void caretUpdate(CaretEvent e) {
      // if (!study_checkbox.isSelected() && userid_textfield.getText().trim().equals("")) {
      //    ok_button.setEnabled(false);
      //  }
      // else {
      //    ok_button.setEnabled(true);
      //  }
   }

   @Override public void actionPerformed(ActionEvent e) {
      if (e.getActionCommand().equals("I am not participating in the user study")) {
	 // if (!study_checkbox.isSelected() && userid_textfield.getText().trim().equals("")) {
	 //    ok_button.setEnabled(false);
	 //  }
	 // else {
	 //    ok_button.setEnabled(true);
	 //  }
       }
      else if (e.getActionCommand().equals("CANCEL")) {
	 BoardLog.logE("BOARD","BUBBLES: Setup aborted");
	 System.exit(1);
       }
      else if (e.getActionCommand().equals("OK")) {
	 collect_screens = scrns_checkbox.isSelected();
	 collect_experience = exprn_checkbox.isSelected();
	 collect_active = activ_checkbox.isSelected();
	 collect_commands = cmmds_checkbox.isSelected();
	 collect_options = optns_checkbox.isSelected();
	 collect_workingset = wrkst_checkbox.isSelected();
	 collect_eclipse = eclps_checkbox.isSelected();
	 collect_dumps = dumps_checkbox.isSelected();
	 collect_monitorlog = mnlog_checkbox.isSelected();

	 // if (userid_textfield != null) user_id = userid_textfield.getText();
	 if (user_id == null || user_id.equals("")) user_id = getUserId();
	 else user_id = user_id.replace(" ","_");

	 board_properties.setProperty(BOARD_METRIC_PROP_USERID,user_id);
	 board_properties.setProperty(BOARD_METRIC_PROP_SCREENS,collect_screens);
	 board_properties.setProperty(BOARD_METRIC_PROP_EXPERIENCE,collect_experience);
	 board_properties.setProperty(BOARD_METRIC_PROP_ACTIVE,collect_active);
	 board_properties.setProperty(BOARD_METRIC_PROP_COMMANDS,collect_commands);
	 board_properties.setProperty(BOARD_METRIC_PROP_OPTIONS,collect_options);
	 board_properties.setProperty(BOARD_METRIC_PROP_WORKINGSET,collect_workingset);
	 board_properties.setProperty(BOARD_METRIC_PROP_ECLIPSE,collect_eclipse);
	 board_properties.setProperty(BOARD_METRIC_PROP_DUMPS,collect_dumps);
	 board_properties.setProperty(BOARD_METRIC_PROP_MONITOR,collect_monitorlog);

	 try {
	    board_properties.save();
	  }
	 catch (IOException ioe) { }

	 this.setVisible(false);
      }
   }

}	// end of inner class MetricsDialog




/********************************************************************************/
/*										*/
/*	Methods to handle command data						*/
/*										*/
/********************************************************************************/

private void setupCommandData()
{
   command_data = new ArrayList<String>();
   saveCommand("PROPERTY","java.version="+System.getProperty("java.version"),start_time);
   saveCommand("PROPERTY","os.name="+System.getProperty("os.name"),start_time);
   saveCommand("PROPERTY","os.arch="+System.getProperty("os.arch"),start_time);
   saveCommand("PROPERTY","os.version="+System.getProperty("os.version"),start_time);
   Runtime rt = Runtime.getRuntime();
   saveCommand("PROPERTY","java.maxmemory="+Long.toString(rt.maxMemory()),start_time);
   saveCommand("PROPERTY","java.processors="+Integer.toString(rt.availableProcessors()),start_time);

   BoardProperties bp = BoardProperties.getProperties("System");
   saveCommand("PROPERTY","updates="+bp.getProperty(BOARD_PROP_ECLIPSE_FOREGROUND) + "," +
		  bp.getProperty(BOARD_PROP_AUTO_UPDATE),start_time);
}




private void saveCommand(String src,String cmd,long now)
{
   if (command_data == null) return;

   setActive(now);

   synchronized (command_data) {
      command_data.add(src + "," + cmd + "," + now);
    }
}



private void dumpCommands()
{

   if (command_data == null) return;
   if (command_data.size() == 0) return;

   List<String> oldcmds;

   synchronized (command_data) {
      oldcmds = command_data;
      command_data = new ArrayList<String>();
    }

   File f = null;
   try {
      f = File.createTempFile("BoardMetrics_COMMANDS_",".csv");
      PrintWriter fw = new PrintWriter(new FileWriter(f));
      for (String s : oldcmds) fw.println(s);
      fw.close();
      sendFile(f,"COMMANDS",true);
    }
   catch (IOException e) {
      BoardLog.logE("BOARD","Problem writing command file: " + e);
    }
   finally {
      if (f != null) f.delete();
    }
}



private void addBugCommands(IvyXmlWriter xw)
{
   if (command_data == null) return;
   if (command_data.size() == 0) return;

   List<String> cmds = new ArrayList<String>();
   synchronized (command_data) {
      cmds.addAll(command_data);
    }

   if (cmds.size() > 0) {
      xw.begin("COMMANDS");
      for (String s : cmds) xw.textElement("CMD",s);
      xw.end("COMMANDS");
    }
}




/********************************************************************************/
/*										*/
/*	Methods to create screen dump						*/
/*										*/
/********************************************************************************/

private void dumpScreen()
{
   if (root_window == null || !collect_screens) return;
   if (last_screen >= last_active) return;

   last_screen = System.currentTimeMillis();

   String typ = "png";

   ScreenDumper sd = new ScreenDumper(typ);
   SwingUtilities.invokeLater(sd);
}



private class ScreenDumper implements Runnable {

   private String dump_type;
   private File dump_file;

   ScreenDumper(String typ) {
      dump_type = typ;
      dump_file = null;
    }

   @Override public void run() {
      if (dump_file == null) {
	 BufferedImage bi;
	 Dimension sz = root_window.getSize();
	 bi = new BufferedImage(sz.width,sz.height,BufferedImage.TYPE_INT_RGB);
	 Graphics2D g2 = bi.createGraphics();
	 root_window.paint(g2);

	 BufferedImageOp op = new ConvolveOp(new Kernel(BLUR_SIZE, BLUR_SIZE, BLUR_MATRIX));
	 bi = op.filter(bi, null);

	 File f = null;
	 try {
	    f = File.createTempFile("BoardMetrics_SCREEN_","." + dump_type);
	    ImageIO.write(bi,dump_type,f);
	  }
	 catch (IOException e) {
	    BoardLog.logE("BOARD","Problem dumping screen: " + e);
	  }
	 if (f != null) {
	    dump_file = f;
	    // do the upload in a background thread
	    BoardThreadPool.start(this);
	  }
      }
      else {
	 try {
	    sendFile(dump_file,"SCREEN",true);
	    dump_file.delete();
	    dump_file = null;
	  }
	 catch (IOException e) {
	    BoardLog.logE("BOARD","Problem sending screen image: " + e);
	  }
       }
    }

}	// end of inner class ScreenDumper




/********************************************************************************/
/*										*/
/*	Methods to send Eclipse data						*/
/*										*/
/********************************************************************************/

private void dumpEclipseData()
{
   if (eclipse_length < 0) getEclipseFile();
   if (eclipse_file == null) return;
   long elen = eclipse_file.length();
   int wlen = (int)(elen - eclipse_length);
   if (wlen == 0) return;

   File f = null;
   try {
      f = File.createTempFile("BoardMetrics_ECLIPSE_",".csv");
      FileOutputStream ots = new FileOutputStream(f);
      FileInputStream ins = new FileInputStream(eclipse_file);
      byte [] buf = new byte[8192];

      if (eclipse_length > 0) ins.skip(eclipse_length);
      while (wlen > 0) {
	 int rl = ins.read(buf);
	 if (rl <= 0) break;
	 ots.write(buf,0,rl);
	 wlen -= rl;
       }

      ots.close();
      ins.close();

      eclipse_length = elen;

      sendFile(f,"ECLIPSE",true);
    }
   catch (IOException e) {
      BoardLog.logE("BOARD","Problem writing eclipse data: " + e);
    }
   finally {
      if (f != null) f.delete();
    }

}



private void getEclipseFile()
{
   eclipse_length = 0;
   eclipse_file = null;

   BoardProperties bp = BoardProperties.getProperties("System");
   String ws = bp.getProperty(BOARD_PROP_ECLIPSE_WS);
   File dir = new File(ws);
   dir = new File(dir,".metadata");
   dir = new File(dir,".plugins");
   dir = new File(dir,"org.eclipse.epp.usagedata.recording");
   File [] files = dir.listFiles();
   if (files != null) {
      for (File f : files) {
	 if (!f.getName().startsWith("upload")) continue;
	 if (f.lastModified() > start_time) {
	    eclipse_file = f;
	    break;
	  }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Methods to send Options files						*/
/*										*/
/********************************************************************************/

private void dumpOptions()
{
   File [] files = getOptionsFiles();
   try {
      for (File f: files) {
	 if (f != null && f.lastModified() > last_options) {
	    sendFile(f,"OPTIONS",false);
	  }
       }
      last_options = System.currentTimeMillis();
    }
   catch (IOException e) {
      BoardLog.logE("BOARD", "Error uploading option files: " + e);
    }
}



private File[] getOptionsFiles()
{
   File dir = BoardProperties.getPropertyDirectory();
   File[] files = dir.listFiles(new OptionFileFilter());
   return files;
}



private static class OptionFileFilter implements FileFilter {

   @Override public boolean accept(File path) {
      if (path.isDirectory()) return false;
      String nm = path.getName();
      if (nm.startsWith(".")) return false;
      if (!nm.endsWith(".props")) return false;
      return true;
    }

}	// end of inner class OptionFileFilter




/********************************************************************************/
/*										*/
/*	Methods to handle uploading workingsets 				*/
/*										*/
/********************************************************************************/

private void dumpWorkingset()
{
   File f = privatizeConfiguration();
   if (f != null && f.exists()) {
      try {
	 // TODO: Rewrite config file to remove any names
	 // CONTENT should only include TYPE and FRAGTYPE
	 sendFile(f,"WORKINGSET",false);
	 f.delete();
      }
      catch (IOException e) {
	 BoardLog.logE("BOARD", "Error to upload workingset file");
       }
    }
}


private File privatizeConfiguration()
{
   File f1 = BoardSetup.getConfigurationFile();
   if (f1 == null || !f1.exists()) return null;
   try {
      Element xml = IvyXml.loadXmlFromFile(f1);
      privatizeXml(xml);
      File f2 = File.createTempFile("BubblesConfig",".xml");
      f2.deleteOnExit();
      IvyXmlWriter xw = new IvyXmlWriter(f2);
      xw.writeXml(xml);
      xw.close();
      return f2;
    }
   catch (IOException e) { }

   return null;
}



private void privatizeXml(Element e)
{
   if (IvyXml.isElement(e,"CONTENT")) {
      NamedNodeMap nnm = e.getAttributes();
      for (int i = 0; i < nnm.getLength(); ) {
	 Attr at = (Attr) nnm.item(i);
	 if (at.getName().equals("TYPE") || at.getName().equals("FRAGTYPE")) ++i;
	 else nnm.removeNamedItem(at.getName());
       }
      for ( ; ; ) {
	 Node n = e.getFirstChild();
	 if (n == null) break;
	 e.removeChild(n);
       }
    }
   else {
      for (Element c : IvyXml.children(e)) {
	 privatizeXml(c);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Methods to handle uploading monitor log (from eclipse mylyn)		*/
/*										*/
/********************************************************************************/

private void dumpMonitorLogFile()
{
   File f = BoardLog.getMonitorLogFile();
   if (f != null && f.exists()) {
      try {
	sendFile(f,"MONITORLOG",false);
      }
      catch (IOException e) {
	 BoardLog.logE("BOARD", "Error to upload monitor log file");
       }
      monitor_length = f.length();
    }
}



private void deleteMonitorLogFile()
{
   File f = BoardLog.getMonitorLogFile();
   if (f != null && f.exists()) {
      f.delete();
    }
}



private void addBugMonitor(IvyXmlWriter xw)
{
   File f = BoardLog.getMonitorLogFile();
   if (f != null && f.exists()) {
      try {
	 FileReader ins = new FileReader(f);
	 char [] buf = new char[8192];
	 long elen = f.length();
	 int wlen;
	 if (monitor_length < 0) wlen = (int) elen;
	 else wlen = (int) (elen - monitor_length);
	 if (wlen == 0) {
	    ins.close();
	    return;
	  }

	 if (monitor_length > 0) ins.skip(monitor_length);
	 while (wlen > 0) {
	    int rl = ins.read(buf);
	    if (rl <= 0) break;
	    xw.write(buf,0,rl);
	    wlen -= rl;
	  }
	 ins.close();
       }
      catch (IOException e) { }
    }
}



/********************************************************************************/
/*										*/
/*	Methods to handle user-requested dump					*/
/*										*/
/********************************************************************************/

private void dumpAll()
{
   dumpCommands();
   dumpScreen();
   dumpEclipseData();
   dumpOptions();
   dumpWorkingset();
   dumpMonitorLogFile();
}



/********************************************************************************/
/*										*/
/*	Timing methods								*/
/*										*/
/********************************************************************************/

private void setActive()
{
   setActive(System.currentTimeMillis());
}



private void setActive(long when)
{
   boolean doscreen = false;
   boolean docommand = false;
   boolean dofeedback = false;
   boolean doeclipse = false;
   boolean dooptions = false;
   boolean doworkingset = false;
   boolean domonitorlog = false;

   synchronized (this) {
      long delta = when - last_active;
      if (delta <= 0) return;
      long prev = last_active;
      last_active = when;

      if (delta > INACTIVE_TIME) {
	 if (collect_active) {
	    saveCommand("ACTIVE","inactive.start",prev);
	    saveCommand("ACTIVE","inactive.end",when);
	  }
       }
      else {
	 total_active += delta;
	 if (next_screen > 0 && total_active > next_screen) {
	    doscreen = true;
	    next_screen = total_active + SCREEN_DUMP_TIME;
	  }
	 if (next_command > 0 && total_active > next_command) {
	    docommand = true;
	    next_command = total_active + COMMAND_DUMP_TIME;
	  }
	 if (next_feedback > 0 && total_active > next_feedback) {
	    dofeedback = true;
	    next_feedback = total_active + USER_FEEDBACK_TIME;
	  }
	 if (next_eclipse > 0 && total_active > next_eclipse) {
	    doeclipse = true;
	    next_eclipse = total_active + ECLIPSE_DUMP_TIME;
	  }
	 if (next_options > 0 && total_active > next_options) {
	    dooptions = true;
	    next_options = total_active + OPTIONS_DUMP_TIME;
	  }
	 if (next_workingset > 0 && total_active > next_workingset) {
	    doworkingset = true;
	    next_workingset = total_active + WORKINGSET_DUMP_TIME;
	  }
	 if (next_monitorlog > 0 && total_active > next_monitorlog)  {
	    domonitorlog = true;
	    next_monitorlog = total_active + MONITORLOG_DUMP_TIME;
	 }
       }

      if (doscreen) {
	 BoardThreadPool.start(new ScreenDumpTask());
       }
      if (docommand) {
	 BoardThreadPool.start(new CommandDumpTask());
       }
      if (dofeedback) {
	 BoardThreadPool.start(new UserFeedbackTask());
       }
      if (doeclipse) {
	 BoardThreadPool.start(new EclipseDumpTask());
       }
      if (dooptions) {
	 BoardThreadPool.start(new OptionsDumpTask());
       }
      if (doworkingset) {
	 BoardThreadPool.start(new WorkingsetDumpTask());
       }
      if (domonitorlog) {
	BoardThreadPool.start(new MonitorlogDumpTask());
      }
    }
}



/********************************************************************************/
/*										*/
/*	Methods to send a file to the repository				*/
/*										*/
/********************************************************************************/

private void sendFile(File f,String typ,boolean del) throws IOException
{
   if (!do_dump) return;

   new BoardUpload(f,user_id,run_id);

   if (del) f.delete();
}



/********************************************************************************/
/*										*/
/*	Timer tasks								*/
/*										*/
/********************************************************************************/

private class ScreenDumpTask implements Runnable {

   @Override public void run() {
      if (collect_screens) dumpScreen();
    }

   @Override public String toString() {
      return "BOARD_ScreenDumpTask";
    }

}	// end of inner class ScreenDumpTask



private class CommandDumpTask implements Runnable {

   @Override public void run() {
      if (collect_commands) dumpCommands();
    }

   @Override public String toString() {
      return "BOARD_CommandDumpTask";
    }

}	// end of inner class CommandDumpTask



private static class UserFeedbackTask implements Runnable {

   @Override public void run() {
      // if active, put up user questionaire
    }

   @Override public String toString() {
      return "BOARD_UserFeedbackTask";
    }

}	// end of inner class UserFeedbackTask



private class EclipseDumpTask implements Runnable {

   @Override public void run() {
      if (collect_eclipse) dumpEclipseData();
    }

   @Override public String toString() {
      return "BOARD_EclipseDumpTask";
    }

}	// end of inner class EclipseDumpTask


private class OptionsDumpTask implements Runnable {

   @Override public void run() {
      if (collect_options) dumpOptions();
    }

   @Override public String toString() {
      return "BOARD_OptionsDumpTask";
    }

}	// end of inner class OptionsDumpTask



private class WorkingsetDumpTask implements Runnable {

   @Override public void run() {
      if (collect_workingset) dumpWorkingset();
    }

   @Override public String toString() {
      return "BOARD_WorkingsetDumpTask";
    }

}	// end of inner class WorkingsetDumpTask



private class MonitorlogDumpTask implements Runnable {

   @Override public void run() {
      if (collect_monitorlog) dumpMonitorLogFile();
    }

   @Override public String toString() {
      return "BOARD_MonitorlogDumpTask";
    }

}	// end of inner class MonitorlogDumpTask



private class DumpAllTask extends Thread {

   DumpAllTask() {
      super("BoardMetricsCleanup");
    }

   @Override public void run() {
      dumpCommands();
      if (collect_eclipse) dumpEclipseData();
      if (collect_monitorlog) {
	dumpMonitorLogFile();
	deleteMonitorLogFile();
      }
    }

}	// end of inner class DumpAllTask




}	// end of class BoardMetrics




/* end of BoardMetrics.java */


