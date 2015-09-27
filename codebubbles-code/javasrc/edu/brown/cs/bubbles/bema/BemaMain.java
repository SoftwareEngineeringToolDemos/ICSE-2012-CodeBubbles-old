/********************************************************************************/
/*										*/
/*		BemaMain.java							*/
/*										*/
/*	Bubbles Environment Main Application main program			*/
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


package edu.brown.cs.bubbles.bema;


import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bdoc.BdocFactory;
import edu.brown.cs.bubbles.bedu.BeduFactory;
import edu.brown.cs.bubbles.board.BoardConstants.BoardLanguage;
import edu.brown.cs.bubbles.board.BoardConstants.RunMode;
import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bueno.BuenoFactory;
import edu.brown.cs.bubbles.bump.BumpClient;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.*;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.jar.*;



/**
 *	Bubbles main program.
 **/

public class BemaMain implements BemaConstants
{


/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

/**
 *	Starting point for the bubbles environment.
 **/

public static void main(String [] args)
{
   BemaMain bm = new BemaMain(args);
   
   if (System.getProperty("os.name").startsWith("Mac")) {
      try {
	 UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
       }
      catch (Throwable t) {
	 System.err.println("BEMA: Problem setting l&f: " + t);
       }
    }

   bm.start();
}



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private boolean 	restore_session;
private boolean 	save_session;
private boolean 	force_setup;
private boolean 	force_metrics;
private boolean 	skip_setup;
private boolean 	skip_splash;
private boolean 	allow_debug;
private boolean 	use_lila;
private boolean 	use_web;
private String		use_workspace;
private boolean 	new_workspace;
private boolean 	ask_workspace;
private Element 	load_config;
private String []	java_args;
private RunMode 	run_mode;
private String		course_name;
private BoardLanguage	for_language;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BemaMain(String [] args)
{
   restore_session = true;
   save_session = true;
   force_setup = false;
   force_metrics = false;
   skip_setup = false;
   skip_splash = false;
   allow_debug = false;
   use_workspace = null;
   new_workspace = false;
   ask_workspace = true;
   use_web = false;
   java_args = args;
   use_lila = false;
   run_mode = RunMode.NORMAL;
   course_name = null;
   for_language = null;

   scanArgs(args);
}



/********************************************************************************/
/*										*/
/*	Argument scanning methods						*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   int ln = args.length;

   for (int i = 0; i < ln; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-nosetup")) {                  // -nosetup
	    skip_setup = true;
	  }
	 else if (args[i].startsWith("-nos")) {                 // -nosave
	    save_session = false;
	  }
	 else if (args[i].startsWith("-nor")) {                 // -norestore
	    restore_session = false;
	  }
	 else if (args[i].startsWith("-f")) {                   // -force
	    force_setup = true;
	  }
	 else if (args[i].startsWith("-py")) {                  // -python
	    for_language = BoardLanguage.PYTHON;
	    File fa = new File(System.getProperty("user.home"));
	    fa = new File(fa,".pybles");
	    // Don't use IvyFile here since it trys to use ivy without initializing it
	    BoardProperties.setPropertyDirectory(fa.getPath());
	  }
         else if (args[i].startsWith("-js")) {                  // -js (javascript)
            for_language = BoardLanguage.JS;
            File fa = new File(System.getProperty("user.home"));
            fa = new File(fa,".nobbles");
            BoardProperties.setPropertyDirectory(fa.getPath());
          }
	 else if (args[i].startsWith("-rebus")) {               // -rebus
	    for_language = BoardLanguage.REBUS;
	    File fa = new File(System.getProperty("user.home"));
	    File fb = new File(fa,".rebus");
	    if (!fb.exists()) fb.mkdir();
	    File fc = new File(fb,".metadata");
	    if (!fc.exists()) fc.mkdir();

	    BoardProperties.setPropertyDirectory(fb.getPath());
	    if (use_workspace == null) {
	       use_workspace = fb.getPath();
	       new_workspace = true;
	       ask_workspace = false;
	     }
	  }
	 else if (args[i].startsWith("-course") && i+1 < ln) {  // -course <course>
	    course_name = args[++i];
	    File fa = new File(System.getProperty("user.home"));
	    fa = new File(fa,".suds" + course_name);
	    // Don't use IvyFile here since it trys to use ivy without initializing it
	    BoardProperties.setPropertyDirectory(fa.getPath());
	  }
	 else if (args[i].startsWith("-c")) {                   // -collect
	    force_metrics = true;
	  }
	 else if (args[i].startsWith("-w") && i+1 < ln) {       // -workspace <ws>
	    use_workspace = args[++i];
	  }
	 else if (args[i].startsWith("-n")) {                   // -new
	    new_workspace = true;
	  }
	 else if (args[i].startsWith("-nosp")) {                // -nosplash
	    skip_splash = true;
	  }
	 else if (args[i].startsWith("-p") && i+1 < ln) {       // -prop <propdir>
	    BoardProperties.setPropertyDirectory(args[++i]);
	  }
	 else if (args[i].startsWith("-Debug")) {               // -Debug
	    allow_debug = true;
	  }
	 else if (args[i].startsWith("-m") && i+1 < ln) {       // -msg <id>
	    System.setProperty("edu.brown.cs.bubbles.MINT",args[++i]);
	  }
	 else if (args[i].startsWith("-W")) {                   // -WEB
	    use_web = true;
	  }
	 else if (args[i].startsWith("-lila")) {
	    use_lila = true;
	  }
	 else if (args[i].startsWith("-C")) {                   // -Client
	    run_mode = RunMode.CLIENT;
	  }
	 else if (args[i].startsWith("-S")) {                   // -Server
	    run_mode = RunMode.SERVER;
	    skip_splash = true;
	    use_lila = false;
	    restore_session = false;
	    save_session = false;
	  }
	 else if (args[i].startsWith("-Dfile.encoding")) ;
	 else if (args[i].startsWith("-s")) {                   // -save
	    save_session = true;
	  }
	 else if (args[i].startsWith("-r")) {                   // -restore
	    restore_session = true;
	  }
	 else badArgs();
       }
      else if (args[i].equals("")) ;
      else if (args[i].equals("edu.brown.cs.bubbles.bema.BemaMain")) ;
      else badArgs();
    }
}



private void badArgs()
{
   System.err.println("BUBBLES: bubbles [-nosave] [-norestore] [-force] [-workspace <workspace>]");
   System.exit(1);
}




/********************************************************************************/
/*										*/
/*	Methods to actually run bubbles 					*/
/*										*/
/********************************************************************************/

private void start()
{

   // first setup the environment
   BoardSetup bs = BoardSetup.getSetup();

   if (course_name != null) bs.setCourseName(course_name);
   if (for_language != null) bs.setLanguage(for_language);

   if (skip_setup) bs.setSkipSetup();
   if (force_setup) bs.setForceSetup(true);
   if (force_metrics) bs.setForceMetrics(true);
   if (skip_splash) bs.setSkipSplash();
   if (allow_debug) bs.setAllowDebug();
   if (use_lila) bs.setUseLila();
   if (new_workspace) bs.setCreateWorkspace(use_workspace);
   else if (use_workspace != null) bs.setDefaultWorkspace(use_workspace);
   if (!ask_workspace) bs.setAskWorkspace(false);
   if (run_mode != null) bs.setRunMode(run_mode);
   bs.setJavaArgs(java_args);

   if (bs.getCourseName() != null) {
      BeduFactory.getFactory();
    }

   bs.doSetup();

   bs.setSplashTask("Setting up Metrics and Logging");
   BoardMetrics.setupMetrics(force_setup);

   BoardProperties bp = BoardProperties.getProperties("Bema");

   if (use_web) {
      String url = bp.getProperty("Bema.web.url");
      if (url == null) {
	 BoardLog.logE("BEMA","Bema.web.url property not defined");
	 use_web = false;
       }
      else {
	 File f = BoardSetup.getPropertyBase();
	 File f1 = new File(f,"webkey");
	 if (!f1.exists()) {
	    BoardLog.logE("BEMA","Can't find webkey file in property directory");
	    use_web = false;
	  }
	 else {
	    try {
	       BufferedReader br = new BufferedReader(new FileReader(f1));
	       String ln = br.readLine();
	       if (ln == null) {
		  BoardLog.logE("BEMA","No key defined in webkey file");
		  use_web = false;
		}
	       else {
		  ln = ln.trim();
		  BumpClient bc = BumpClient.getBump();
		  bc.useWebMint(ln,url);
		}
	       br.close();
	     }
	    catch (IOException e) {
	       BoardLog.logE("BEMA","Unable to read webkey file");
	       use_web = false;
	     }
	    catch (Throwable e) {
	       BoardLog.logE("BEMA","Problem setting up mint",e);
	       use_web = false;
	     }
	  }
       }
    }

   // next start Messaging
   bs.setSplashTask("Setting up messaging");
   BumpClient bc = null;

   try {
      bc = BumpClient.getBump();
    }
   catch (Error e) {
      BoardLog.logE("BEMA","Problem starting bump",e);
    }

   if (bc == null) {
      JOptionPane.showMessageDialog(null,"Can't setup messaging environment",
	    "Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
      System.exit(1);
      return;
    }

   // next start Eclipse
   bs.setSplashTask("Starting IDE (" + bc.getName() + ") and Updating Projects");
   bc.waitForIDE();

   // ensure various components are setup

   bs.setSplashTask("Initializing components");
   BaleFactory.setup();
   BassFactory.setup();
   BdocFactory.setup();

   bs.setSplashTask("Loading Project Symbols");
   BassFactory.waitForNames();

   for (String s : getSetupPackageProperties()) {
      String nm = bp.getProperty(s);
      if (nm == null || nm.equals("*") || nm.equals("")) continue;
      String ld = bp.getProperty(s + ".load");
      setupPackage(nm,ld);
    }

   String pinf = bp.getProperty("Bema.pluginfolder");
   if (pinf != null) {
      File dir = new File(pinf);
      if (dir.exists() && dir.isDirectory()) {
	 File [] cands = dir.listFiles(new JarFilter());
	 if (cands != null) {
	    for (File jfn : cands) {
	       try {
		  JarFile jf = new JarFile(jfn);
		  Manifest mf = jf.getManifest();
		  if (mf == null) continue;
		  Attributes at = mf.getMainAttributes();
		  String nm = at.getValue("Bubbles.start");
		  System.err.println("BEMA: Load plugin " + jfn + " using " + nm);
		  // need to create a class loader for this jar, then use it to load the given class and
		  // use it to initialize things
		  // TODO: load plugin in jar file
		  jf.close();
		}
	       catch (IOException e) {
		  BoardLog.logE("BEMA","Can't access plugin jar file " + jfn);
		}
	     }
	  }
       }
    }

   bs.setSplashTask("Loading and Caching JavaDoc");
   BdocFactory.getFactory().waitForReady();

   // setup top level session from backup

   bs.setSplashTask("Restoring configuration");
   load_config = null;
   if (restore_session) {
      File cf = BoardSetup.getConfigurationFile();
      load_config = IvyXml.loadXmlFromFile(cf);
    }

   ToolTipManager ttm = ToolTipManager.sharedInstance();
   ttm.setDismissDelay(Integer.MAX_VALUE);

   // now start windows
   BudaRoot root = new BudaRoot(load_config);

   initializePackage("edu.brown.cs.bubbles.bale.BaleFactory",root);
   initializePackage("edu.brown.cs.bubbles.bass.BassFactory",root);

   for (String s : getSetupPackageProperties()) {
      String nm = bp.getProperty(s);
      initializePackage(nm,root);
    }

   if (bs.getRunMode() != RunMode.SERVER) {
      root.pack();
      root.restoreConfiguration(load_config);
      root.setVisible(true);
    }

   bs.removeSplash();

   if (save_session) {
      Runtime.getRuntime().addShutdownHook(new SaveSession(root));
    }

   if (bs.getRunMode() == RunMode.SERVER) {
       waitForServerExit(root);
    }

   BumpClient nbc = BumpClient.getBump();
   Element xe = nbc.getAllProjects(300000);
   if (IvyXml.getChild(xe,"PROJECT") == null) {
      BudaBubble bb = null;
      switch (BoardSetup.getSetup().getLanguage()) {
	 default :
	    bb = BuenoFactory.getFactory().getCreateProjectBubble();
	    break;
	 case REBUS :
	    try {
	       Class<?> c = Class.forName("edu.brown.cs.bubbles.rebus.RebusFactory");
	       Method m = c.getMethod("createSearchBubble");
	       bb = (BudaBubble) m.invoke(null);
	     }
	    catch (Throwable t) {
	       BoardLog.logE("BEMA","Problem creating rebus bubble",t);
	     }
	    break;
       }
      if (bb != null) {
	 root.waitForSetup();
	 BudaBubbleArea bba = root.getCurrentBubbleArea();
	 Dimension d = bb.getSize();
	 Rectangle r = bba.getViewport();
	 int x0 = r.x + r.width/2 - d.width/2;
	 int y0 = r.y + r.height/2 - d.height/2;
	 bba.addBubble(bb, x0, y0);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Initialization by name							*/
/*										*/
/********************************************************************************/

private void setupPackage(String nm,String load)
{
   // BoardLog.logD("BEMA","Setup " + nm);

   if (load != null) {
      // setup class loader for load jar file
      // use this instead of Class.forName();
      // see URLClassLoader
    }

   try {
      Class<?> c = Class.forName(nm);
      Method m = c.getMethod("setup");
      m.invoke(null);
    }
   catch (ClassNotFoundException e) {
      BoardLog.logE("BEMA","Can't find initialization package " + nm);
    }
   catch (Exception e) {
      // missing setup method is okay
    }
}



private void initializePackage(String nm,BudaRoot br)
{
   // BoardLog.logD("BEMA","Initialize " + nm);

   try {
      Class<?> c = Class.forName(nm);
      Method m = c.getMethod("initialize",BudaRoot.class);
      m.invoke(null,br);
    }
   catch (Throwable e) {
      // missing initialize method is okay, missing class already caught
    }
}



private Collection<String> getSetupPackageProperties()
{
   Map<Integer,String> loads = new TreeMap<Integer,String>();
   BoardProperties bp = BoardProperties.getProperties("Bema");
   BoardSetup bs = BoardSetup.getSetup();

   for (String s : bp.stringPropertyNames()) {
      boolean use = false;
      if (s.startsWith("Bema.plugin.") && s.lastIndexOf(".") == 11) use = true;
      switch (bs.getRunMode()) {
	 case NORMAL :
	    if (s.startsWith("Bema.plugin.normal.")) use = true;
	    break;
	 case CLIENT :
	    if (s.startsWith("Bema.plugin.client.")) use = true;
	    break;
	 case SERVER :
	    if (s.startsWith("Beam.plugin.server.")) use = true;
	    break;
       }
      if (use) {
	 int idx = s.lastIndexOf(".");
	 try {
	    int v = Integer.parseInt(s.substring(idx+1));
	    loads.put(v,s);
	  }
	 catch (NumberFormatException e) { }
       }
    }

   return new LinkedHashSet<String>(loads.values());
}




/********************************************************************************/
/*										*/
/*	Server mode dialog							*/
/*										*/
/********************************************************************************/

private void waitForServerExit(BudaRoot root)
{
   JOptionPane.showConfirmDialog(root,"Exit from Code Bubbles Server",
	    "Exit When Done",
	    JOptionPane.DEFAULT_OPTION,
	    JOptionPane.QUESTION_MESSAGE);

   root.handleSaveAllRequest();

   System.exit(0);
}



/********************************************************************************/
/*										*/
/*	Methods to save session at the end					*/
/*										*/
/********************************************************************************/

private static class SaveSession extends Thread {

   private BudaRoot for_root;

   SaveSession(BudaRoot br) {
      super("SaveSessionAtEnd");
      for_root = br;
    }

   @Override public void run() {
      File cf = BoardSetup.getConfigurationFile();
      try {
	 // for_root.handleSaveAllRequest();
	 for_root.handleCheckpointAllRequest();
	 for_root.saveConfiguration(cf);
       }
      catch (IOException e) {
	 BoardLog.logE("BEMA","Problem saving session: " + e);
       }
    }

}	// end of inner class SaveSession


/********************************************************************************/
/*										*/
/*	Filter for jar files							*/
/*										*/
/********************************************************************************/

private static class JarFilter implements FilenameFilter {

   @Override public boolean accept(File dir,String name) {
      return name.endsWith(".jar");
   }

}	// end of inner class JarFilter



}	// end of class BemaMain




/* end of BemaMain.java */



