/********************************************************************************/
/*										*/
/*		BedrockPlugin.java						*/
/*										*/
/*	Main class for the Eclipse-Bubbles interface				*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss, Hsu-Sheng Ko      */
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


package edu.brown.cs.bubbles.bedrock;

import edu.brown.cs.ivy.exec.IvySetup;
import edu.brown.cs.ivy.mint.*;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.*;
import org.w3c.dom.Element;

import java.io.*;
import java.net.InetAddress;
import java.net.URI;
import java.util.*;


public class BedrockPlugin extends Plugin implements IStartup,
		BedrockConstants, MintConstants {


//TODO: verbose error message if setup not correct
//TODO: use .bubbles/System to get location for $(IVY)


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private MintControl mint_control;

private Object send_sema;
private boolean workbench_inited;
private boolean monitor_on;
private BedrockProject bedrock_project;
private BedrockJava bedrock_java;
private BedrockRuntime bedrock_runtime;
private BedrockBreakpoint bedrock_breakpoint;
private BedrockEditor bedrock_editor;
private BedrockCall bedrock_call;
private BedrockProblem bedrock_problem;
private BedrockEclipseMonitor bedrock_monitor;
private BedrockQuickFix quick_fixer;
private boolean shutdown_mint;
private boolean doing_exit;
private int	num_clients;

private static PrintStream log_file = null;
private static BedrockLogLevel log_level = BedrockLogLevel.INFO;
private static boolean use_stderr = false;

private static BedrockPlugin the_plugin = null;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BedrockPlugin()
{
   the_plugin = this;
   workbench_inited = false;
   monitor_on = false;
   shutdown_mint = false;
   num_clients = 0;
   doing_exit = false;

   String hm = System.getProperty("user.home");
   File f1 = new File(hm);
   File f2 = new File(f1,".bubbles");
   File f3 = new File(f2,".ivy");
   File f4 = new File(f1,".ivy");

   if (!f2.exists()) return;
   if (!f3.exists() || !IvySetup.setup(f3)) {
      if (!f4.exists()) return;
      IvySetup.setup();
    }

   IWorkspace ws = ResourcesPlugin.getWorkspace();

   if (log_file == null) {
      try {
	 String filename = ws.getRoot().getLocation().append("bedrock_log.log").toOSString();
	 log_file = new PrintStream(new FileOutputStream(filename),true);
       }
      catch (FileNotFoundException e) {
	 log_file = null;
	 BedrockPlugin.logE("Error initialising file: " + e.getMessage());
       }
    }

   BedrockPlugin.logI("STARTING");
   BedrockPlugin.logI("MEMORY " + Runtime.getRuntime().maxMemory() + " " +
			 Runtime.getRuntime().totalMemory());

   mint_control = null;
   send_sema = new Object();

   try {
      bedrock_project = new BedrockProject(this);
      bedrock_java = new BedrockJava(this);
      bedrock_runtime = new BedrockRuntime(this);
      bedrock_breakpoint = new BedrockBreakpoint(this);
      bedrock_editor = new BedrockEditor(this);
      bedrock_call = new BedrockCall(this);
      bedrock_problem = new BedrockProblem(this);
      quick_fixer = new BedrockQuickFix(this);
      try {
	 bedrock_monitor = new BedrockEclipseMonitor();
       }
      catch (Throwable t) {
	 BedrockPlugin.logI("Problem starting eclipse monitor: " + t);
	 // might not have mylin or ui
       }
    }
   catch (Throwable t) {
      BedrockPlugin.logE("PROBLEM STARTING BEDROCK: " + t,t);
    }

   initWorkbench();
}


private void initWorkbench() {
   if (!workbench_inited && PlatformUI.isWorkbenchRunning()) {
      BedrockPlugin.logI("workbench inited");
      workbench_inited = true;
    }
}




/********************************************************************************/
/*										*/
/*	Mint methods								*/
/*										*/
/********************************************************************************/

private synchronized void setupMint()
{
   IvySetup.setup();

   if (mint_control != null) return;

   String mintname = System.getProperty("edu.brown.cs.bubbles.MINT");
   if (mintname == null) mintname = System.getProperty("edu.brown.cs.bubbles.mint");
   if (mintname == null) mintname = BEDROCK_MESSAGE_ID;

   mint_control = MintControl.create(mintname,MintSyncMode.SINGLE);
   mint_control.register("<BUBBLES DO='_VAR_1' />",new CommandHandler());
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public static BedrockPlugin getPlugin() 		{ return the_plugin; }

BedrockProject getProjectManager()			{ return bedrock_project; }

void getActiveElements(IJavaElement elt,List<IJavaElement> rslt)
{
   bedrock_editor.getActiveElements(elt,rslt);
}

void waitForEdits()
{
   bedrock_editor.waitForEdits();
}


void getWorkingElements(IJavaElement elt,List<ICompilationUnit> rslt)
{
   bedrock_editor.getWorkingElements(elt,rslt);
}

void getCompilationElements(IJavaElement elt,List<ICompilationUnit> rslt)
{
   bedrock_editor.getCompilationElements(elt,rslt);
}


ICompilationUnit getCompilationUnit(String proj,String file) throws BedrockException
{
   return bedrock_editor.getCompilationUnit(proj,file);
}


CompilationUnit getAST(String bid,String proj,String file) throws BedrockException
{
   return bedrock_editor.getAST(bid,proj,file);
   // return null;
}


void addFixes(IProblem ip,IvyXmlWriter xw)
{
   bedrock_problem.addFixes(ip,xw);
}




void addFixes(IMarkerDelta ip,IvyXmlWriter xw)
{
   bedrock_problem.addFixes(ip,xw);
}




void addFixes(IMarker ip,IvyXmlWriter xw)
{
   bedrock_problem.addFixes(ip,xw);
}




/********************************************************************************/
/*										*/
/*	Start/stop methods							*/
/*										*/
/********************************************************************************/

public void earlyStartup()
{
   BedrockPlugin.logI("Startup called");
}



@Override public void start(BundleContext ctx) throws Exception
{
   super.start(ctx);

   startBedrock();
}




@Override public void stop(BundleContext ctx) throws Exception
{
   IvyXmlWriter xw = beginMessage("STOP");
   finishMessage(xw);

   BedrockPlugin.logI("Stop called");

   if (bedrock_monitor != null) bedrock_monitor.stopMonitoring();

   if (!doing_exit) {
      doing_exit = true;
      shutdown_mint = true;
      if (mint_control != null) mint_control.shutDown();
    }

   if (bedrock_project != null) bedrock_project.terminate();

   super.stop(ctx);
}




private void startBedrock()
{
   try {
      BedrockPlugin.logI("Start called");

      initWorkbench();

      bedrock_project.initialize();
      bedrock_runtime.start();
      bedrock_breakpoint.start();
      bedrock_editor.start();

      setupMint();

      bedrock_project.register();
    }
   catch (Throwable t) {
      BedrockPlugin.logE("Problem starting bedrock: " + t);
      t.printStackTrace();
    }
}



private void saveEclipse()
{
   try {
      IWorkspace ws = ResourcesPlugin.getWorkspace();
      ws.save(true,new BedrockProgressMonitor(this,"Saving Workbench"));
    }
   catch (Throwable t) {
      BedrockPlugin.logE("Problem saving workbench: " + t,t);
    }
}



/********************************************************************************/
/*										*/
/*	Methods for sending out messages					*/
/*										*/
/********************************************************************************/

private void sendMessage(String msg)
{
   synchronized (send_sema) {
      if (mint_control != null && !doing_exit)
	 mint_control.send(msg);
    }
}


private String sendMessageWait(String msg,long delay)
{
   MintDefaultReply rply = new MintDefaultReply();

   synchronized (send_sema) {
      if (mint_control != null && !doing_exit) {
	 mint_control.send(msg,rply,MINT_MSG_FIRST_NON_NULL);
       }
      else return null;
    }

   return rply.waitForString(delay);
}



IvyXmlWriter beginMessage(String typ)
{
   return beginMessage(typ,null);
}


IvyXmlWriter beginMessage(String typ,String bid)
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("BEDROCK");
   xw.field("SOURCE","ECLIPSE");
   xw.field("TYPE",typ);
   if (bid != null) xw.field("BID",bid);

   return xw;
}


void finishMessage(IvyXmlWriter xw)
{
   xw.end("BEDROCK");

   sendMessage(xw.toString());
}



String finishMessageWait(IvyXmlWriter xw)
{
   return finishMessageWait(xw,0);
}


String finishMessageWait(IvyXmlWriter xw,long delay)
{
   xw.end("BEDROCK");

   return sendMessageWait(xw.toString(),delay);
}



/********************************************************************************/
/*										*/
/*	New command processors							*/
/*										*/
/********************************************************************************/

private String handleCommand(String cmd,String proj,Element xml) throws BedrockException
{
   BedrockPlugin.logI("Handle command " + cmd + " for " + proj);
   long start = System.currentTimeMillis();

   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("RESULT");

   if (cmd.equals("PING")) {
      if (doing_exit || shutdown_mint) xw.text("EXIT");
      else if (PlatformUI.isWorkbenchRunning()) xw.text("PONG");
      else xw.text("UNSET");
    }
   else if (shutdown_mint) {
      xw.close();
      throw new BedrockException("Command during exit");
    }
   else if (cmd.equals("PROJECTS")) {
      bedrock_project.listProjects(xw);
    }
   else if (cmd.equals("OPENPROJECT")) {
      bedrock_project.openProject(proj,IvyXml.getAttrBool(xml,"FILES",false),
				     IvyXml.getAttrBool(xml,"PATHS",false),
				     IvyXml.getAttrBool(xml,"CLASSES",false),
				     IvyXml.getAttrBool(xml,"OPTIONS",false),
	 IvyXml.getAttrBool(xml,"IMPORTS",false),
				     IvyXml.getAttrString(xml,"BACKGROUND"),xw);
    }
   else if (cmd.equals("EDITPROJECT")) {
      bedrock_project.editProject(proj,IvyXml.getAttrBool(xml,"LOCAL"),
	    IvyXml.getChild(xml,"PROJECT"),xw);
    }
   else if (cmd.equals("CREATEPROJECT")) {
      bedrock_project.createProject();
    }
   else if(cmd.equals("IMPORTPROJECT")){
      try{
	 bedrock_project.importExistingProject(proj);
       }
      catch(Throwable t) {
	 xw.close();
	 throw new BedrockException("Exception constructing project: " + t.getMessage());
       }
    }
   else if (cmd.equals("BUILDPROJECT")) {
      bedrock_project.buildProject(proj,IvyXml.getAttrBool(xml,"CLEAN"),
				      IvyXml.getAttrBool(xml,"FULL"),
				      IvyXml.getAttrBool(xml,"REFRESH"),xw);
    }
   else if (cmd.equals("CREATEPACKAGE")) {
      bedrock_project.createPackage(proj,IvyXml.getAttrString(xml,"NAME"),
				       IvyXml.getAttrBool(xml,"FORCE",false),xw);
    }
   else if (cmd.equals("FINDPACKAGE")) {
      bedrock_project.findPackage(proj,IvyXml.getAttrString(xml,"NAME"),xw);
    }
   else if (cmd.equals("GETALLNAMES")) {
      bedrock_java.getAllNames(proj,IvyXml.getAttrString(xml,"BID","*"),
				  getSet(xml,"FILE"),
				  IvyXml.getAttrString(xml,"BACKGROUND"),xw);
    }
   else if (cmd.equals("FINDDEFINITIONS")) {
      bedrock_java.handleFindAll(proj,IvyXml.getAttrString(xml,"FILE"),
				    IvyXml.getAttrInt(xml,"START"),IvyXml.getAttrInt(xml,"END"),
				    IvyXml.getAttrBool(xml,"DEFS",true),
				    IvyXml.getAttrBool(xml,"REFS",false),
				    IvyXml.getAttrBool(xml,"IMPLS",false),
				    IvyXml.getAttrBool(xml,"EQUIV",false),
				    IvyXml.getAttrBool(xml,"EXACT",false),
				    IvyXml.getAttrBool(xml,"SYSTEM",false),
				    IvyXml.getAttrBool(xml,"TYPE",false),
				    false,false,
				    xw);
    }
   else if (cmd.equals("FINDREFERENCES")) {
      bedrock_java.handleFindAll(proj,IvyXml.getAttrString(xml,"FILE"),
				    IvyXml.getAttrInt(xml,"START"),IvyXml.getAttrInt(xml,"END"),
				    IvyXml.getAttrBool(xml,"DEFS",true),
				    IvyXml.getAttrBool(xml,"REFS",true),
				    IvyXml.getAttrBool(xml,"IMPLS",false),
				    IvyXml.getAttrBool(xml,"EQUIV",false),
				    IvyXml.getAttrBool(xml,"EXACT",false),
				    IvyXml.getAttrBool(xml,"SYSTEM",false),
				    IvyXml.getAttrBool(xml,"TYPE",false),
				    IvyXml.getAttrBool(xml,"RONLY",false),
				    IvyXml.getAttrBool(xml,"WONLY",false),
				    xw);
    }
   else if (cmd.equals("OPENEDITOR")) {
       bedrock_java.handleOpenEditor(proj, IvyXml.getAttrString(xml,"FILE"), IvyXml.getAttrInt(xml,"LINENUMBER"));
    }
   else if (cmd.equals("PATTERNSEARCH")) {
      bedrock_java.handleJavaSearch(proj,IvyXml.getAttrString(xml,"BID","*"),
				       IvyXml.getAttrString(xml,"PATTERN"),
				       IvyXml.getAttrString(xml,"FOR"),
				       IvyXml.getAttrBool(xml,"DEFS",true),
				       IvyXml.getAttrBool(xml,"REFS",true),
				       IvyXml.getAttrBool(xml,"IMPLS",false),
				       IvyXml.getAttrBool(xml,"EQUIV",false),
				       IvyXml.getAttrBool(xml,"EXACT",false),
				       IvyXml.getAttrBool(xml,"SYSTEM",false),
				       xw);
    }
   else if (cmd.equals("SEARCH")) {
      bedrock_java.textSearch(proj,IvyXml.getAttrInt(xml,"FLAGS",0),
				 IvyXml.getTextElement(xml,"PATTERN"),
				 IvyXml.getAttrInt(xml,"MAX",MAX_TEXT_SEARCH_RESULTS),
				 xw);
    }
   else if(cmd.equals("GETFULLYQUALIFIEDNAME")) {
      bedrock_java.getFullyQualifiedName(proj,IvyXml.getAttrString(xml,"FILE"),
					    IvyXml.getAttrInt(xml,"START"),
					    IvyXml.getAttrInt(xml,"END"),xw);
    }
   else if (cmd.equals("CREATECLASS")) {
      bedrock_java.handleNewClass(proj,IvyXml.getAttrString(xml,"NAME"),
				     IvyXml.getAttrBool(xml,"FORCE",false),
				     IvyXml.getTextElement(xml,"CONTENTS"), xw);
    }
   else if (cmd.equals("FINDHIERARCHY")) {
      bedrock_java.handleFindHierarchy(proj,IvyXml.getAttrString(xml,"PACKAGE"),
					  IvyXml.getAttrString(xml,"CLASS"),
					  IvyXml.getAttrBool(xml,"ALL",false), xw);
    }
   else if (cmd.equals("GETRUNCONFIG")) {
      bedrock_runtime.getRunConfigurations(xw);
    }
   else if (cmd.equals("NEWRUNCONFIG")) {
      bedrock_runtime.getNewRunConfiguration(proj,
						IvyXml.getAttrString(xml,"NAME"),
						IvyXml.getAttrString(xml,"CLONE"),
						IvyXml.getAttrString(xml,"TYPE","Java Application"),xw);
    }
   else if (cmd.equals("EDITRUNCONFIG")) {
      bedrock_runtime.editRunConfiguration(IvyXml.getAttrString(xml,"LAUNCH"),
					      IvyXml.getAttrString(xml,"PROP"),
					      IvyXml.getAttrString(xml,"VALUE"),xw);
    }
   else if (cmd.equals("SAVERUNCONFIG")) {
      bedrock_runtime.saveRunConfiguration(IvyXml.getAttrString(xml,"LAUNCH"),xw);
    }
   else if (cmd.equals("DELETERUNCONFIG")) {
      bedrock_runtime.deleteRunConfiguration(IvyXml.getAttrString(xml,"LAUNCH"),xw);
    }
   else if (cmd.equals("START")) {
      bedrock_runtime.runProject(IvyXml.getAttrString(xml,"NAME"),
				    IvyXml.getAttrString(xml,"MODE",ILaunchManager.DEBUG_MODE),
				    IvyXml.getAttrBool(xml,"BUILD",true),
				    IvyXml.getAttrBool(xml,"REGISTER",true),
				    IvyXml.getAttrString(xml,"VMARG"),
				    IvyXml.getAttrString(xml,"ID"),
				    xw);
    }
   else if (cmd.equals("GETALLBREAKPOINTS")) {
      bedrock_breakpoint.getAllBreakpoints(xw);
    }
   else if (cmd.equals("ADDLINEBREAKPOINT")) {
      bedrock_breakpoint.setLineBreakpoint(proj,IvyXml.getAttrString(xml,"BID","*"),
					      IvyXml.getTextElement(xml,"FILE"),
					      IvyXml.getAttrString(xml,"CLASS"),
					      IvyXml.getAttrInt(xml,"LINE"),
					      IvyXml.getAttrBool(xml,"SUSPENDVM",false),
					      IvyXml.getAttrBool(xml,"TRACE",false));
    }
   else if (cmd.equals("ADDEXCEPTIONBREAKPOINT")) {
      bedrock_breakpoint.setExceptionBreakpoint(proj,IvyXml.getAttrString(xml,"CLASS"), IvyXml.getAttrBool(xml,"CAUGHT",false),
						   IvyXml.getAttrBool(xml,"UNCAUGHT",true),
						   IvyXml.getAttrBool(xml,"CHECKED",false),
						   IvyXml.getAttrBool(xml,"SUSPENDVM",false));
    }
   else if (cmd.equals("EDITBREAKPOINT")) {
      bedrock_breakpoint.editBreakpoint(IvyXml.getAttrInt(xml,"ID"),
					   IvyXml.getAttrString(xml,"PROP"),
					   IvyXml.getAttrString(xml,"VALUE"),
					   IvyXml.getAttrString(xml,"PROP1"),
					   IvyXml.getAttrString(xml,"VALUE1"),
					   IvyXml.getAttrString(xml,"PROP2"),
					   IvyXml.getAttrString(xml,"VALUE2"));
    }
   else if (cmd.equals("CLEARALLLINEBREAKPOINTS")) {
      bedrock_breakpoint.clearLineBreakpoints(proj,null,null,0);
    }
   else if (cmd.equals("CLEARLINEBREAKPOINT")) {
      bedrock_breakpoint.clearLineBreakpoints(proj,IvyXml.getAttrString(xml,"FILE"),
						 IvyXml.getAttrString(xml,"CLASS"),
						 IvyXml.getAttrInt(xml,"LINE"));
    }
   else if (cmd.equals("DEBUGACTION")) {
      bedrock_runtime.debugAction(IvyXml.getAttrString(xml,"LAUNCH"),
				     IvyXml.getAttrString(xml,"TARGET"),
				     IvyXml.getAttrString(xml,"PROCESS"),
				     IvyXml.getAttrString(xml,"THREAD"),
				     IvyXml.getAttrString(xml,"FRAME"),
				     IvyXml.getAttrEnum(xml,"ACTION",BedrockDebugAction.NONE),xw);
    }
   else if (cmd.equals("CONSOLEINPUT")) {
      bedrock_runtime.consoleInput(IvyXml.getAttrString(xml,"LAUNCH"),
				      IvyXml.getTextElement(xml,"INPUT"));
    }
   else if (cmd.equals("GETSTACKFRAMES")) {
      bedrock_runtime.getStackFrames(IvyXml.getAttrString(xml,"LAUNCH"),
					IvyXml.getAttrString(xml,"THREAD"),
					IvyXml.getAttrInt(xml,"COUNT",-1),
					IvyXml.getAttrInt(xml,"DEPTH",0),xw);
    }
   else if(cmd.equals("VARVAL")) {
      bedrock_runtime.getVariableValue(IvyXml.getAttrString(xml,"THREAD"),
					  IvyXml.getAttrString(xml,"FRAME"),
					  IvyXml.getTextElement(xml,"VAR"),
					  IvyXml.getAttrInt(xml,"DEPTH",1),xw);
    }
   else if(cmd.equals("VARDETAIL")) {
      bedrock_runtime.getVariableValue(IvyXml.getAttrString(xml,"THREAD"),
					  IvyXml.getAttrString(xml,"FRAME"),
					  IvyXml.getTextElement(xml,"VAR"),-1,xw);
      /********
      bedrock_runtime.getVariableDetails(IvyXml.getAttrString(xml,"THREAD"),
					    IvyXml.getAttrString(xml,"FRAME"),
					    IvyXml.getTextElement(xml,"VAR"),xw);
      **********/
    }
   else if (cmd.equals("EVALUATE")) {
      bedrock_runtime.evaluateExpression(proj,IvyXml.getAttrString(xml,"BID","*"),
					    IvyXml.getTextElement(xml,"EXPR"),
					    IvyXml.getAttrString(xml,"THREAD"),
					    IvyXml.getAttrString(xml,"FRAME"),
					    IvyXml.getAttrBool(xml,"IMPLICIT",false),
					    IvyXml.getAttrBool(xml,"BREAK",true),
					    IvyXml.getAttrString(xml,"REPLYID"),xw);
    }
   else if (cmd.equals("EDITPARAM")) {
      bedrock_editor.handleParameter(IvyXml.getAttrString(xml,"BID","*"),
					IvyXml.getAttrString(xml,"NAME"),
					IvyXml.getAttrString(xml,"VALUE"));
    }
   else if (cmd.equals("STARTFILE")) {
      bedrock_editor.handleStartFile(proj,IvyXml.getAttrString(xml,"BID","*"),
					IvyXml.getAttrString(xml,"FILE"),
					IvyXml.getAttrString(xml,"ID"),
					IvyXml.getAttrBool(xml,"CONTENTS",false),xw);
    }
   else if (cmd.equals("EDITFILE")) {
      bedrock_editor.handleEdit(proj,IvyXml.getAttrString(xml,"BID","*"),
				   IvyXml.getAttrString(xml,"FILE"),
				   IvyXml.getAttrString(xml,"ID"),
				   getEditSet(xml),xw);
    }
   else if (cmd.equals("COMMIT")) {
      bedrock_editor.handleCommit(proj,IvyXml.getAttrString(xml,"BID","*"),
				     IvyXml.getAttrBool(xml,"REFRESH",false),
				     IvyXml.getAttrBool(xml,"SAVE",false),
				     getElements(xml,"FILE"),xw);
    }
   else if (cmd.equals("CREATEPRIVATE")) {
      bedrock_editor.createPrivateBuffer(proj,IvyXml.getAttrString(xml,"BID","*"),
					    IvyXml.getAttrString(xml,"PID"),
					    IvyXml.getAttrString(xml,"FILE"),xw);
    }
   else if (cmd.equals("PRIVATEEDIT")) {
      bedrock_editor.handleEdit(proj,IvyXml.getAttrString(xml,"PID","*"),
				   IvyXml.getAttrString(xml,"FILE"),
				   null,
				   getEditSet(xml),xw);
    }
   else if (cmd.equals("REMOVEPRIVATE")) {
      bedrock_editor.removePrivateBuffer(proj,
	    IvyXml.getAttrString(xml,"PID"),
	    IvyXml.getAttrString(xml,"FILE"));
    }
   else if (cmd.equals("DELETE")) {
      bedrock_editor.handleDelete(proj,
	    IvyXml.getAttrString(xml,"WHAT"),
	    IvyXml.getAttrString(xml,"PATH"));
    }
   else if (cmd.equals("GETCOMPLETIONS")) {
      bedrock_editor.getCompletions(proj,IvyXml.getAttrString(xml,"BID","*"),
				       IvyXml.getAttrString(xml,"FILE"),
				       IvyXml.getAttrInt(xml,"OFFSET"),xw);
    }
   else if (cmd.equals("ELIDESET")) {
      bedrock_editor.elisionSetup(proj,IvyXml.getAttrString(xml,"BID","*"),
				     IvyXml.getAttrString(xml,"FILE"),
				     IvyXml.getAttrBool(xml,"COMPUTE",true),
				     getElements(xml,"REGION"),xw);
    }
   else if (cmd.equals("FILEELIDE")) {
      bedrock_editor.fileElide(IvyXml.getBytesElement(xml,"FILE"),xw);
    }
   else if (cmd.equals("RENAME")) {
      bedrock_editor.rename(proj,IvyXml.getAttrString(xml,"BID","*"),
			       IvyXml.getAttrString(xml,"FILE"),
			       IvyXml.getAttrInt(xml,"START"),IvyXml.getAttrInt(xml,"END"),
			       IvyXml.getAttrString(xml,"NAME"),IvyXml.getAttrString(xml,"HANDLE"),
			       IvyXml.getAttrString(xml,"NEWNAME"),
			       IvyXml.getAttrBool(xml,"KEEPORIGINAL",false),
			       IvyXml.getAttrBool(xml,"RENAMEGETTERS",false),
			       IvyXml.getAttrBool(xml,"RENAMESETTERS",false),
			       IvyXml.getAttrBool(xml,"UPDATEHIERARCHY",false),
			       IvyXml.getAttrBool(xml,"UPDATEQUALIFIED",false),
			       IvyXml.getAttrBool(xml,"UPDATEREFS",true),
			       IvyXml.getAttrBool(xml,"UPDATESIMILAR",false),
			       IvyXml.getAttrBool(xml,"UPDATETEXT",false),
			       IvyXml.getAttrBool(xml,"DOEDIT",false),
			       IvyXml.getAttrString(xml,"FILES"),xw);
    }
   else if (cmd.equals("MOVEELEMENT")) {
      bedrock_editor.moveElement(proj,IvyXml.getAttrString(xml,"BID","*"),
	    IvyXml.getAttrString(xml,"WHAT"),
	    IvyXml.getAttrString(xml,"FILE"),
	    IvyXml.getAttrInt(xml,"START"),IvyXml.getAttrInt(xml,"END"),
	    IvyXml.getAttrString(xml,"NAME"),IvyXml.getAttrString(xml,"HANDLE"),
	    IvyXml.getAttrString(xml,"TARGET"),
	    IvyXml.getAttrBool(xml,"UPDATEQUALIFIED",true),
	    IvyXml.getAttrBool(xml,"UPDATEREFS",true),
	    IvyXml.getAttrBool(xml,"EDIT",false),xw);
    }
   else if (cmd.equals("RENAMERESOURCE")) {
      bedrock_editor.renameResource(proj,IvyXml.getAttrString(xml,"BID","*"),
				       IvyXml.getAttrString(xml,"FILE"),
				       IvyXml.getAttrString(xml,"NEWNAME"),xw);
    }
   else if (cmd.equals("EXTRACTMETHOD")) {
      bedrock_editor.extractMethod(proj,IvyXml.getAttrString(xml,"BID","*"),
				      IvyXml.getAttrString(xml,"FILE"),
				      IvyXml.getAttrInt(xml,"START"),IvyXml.getAttrInt(xml,"END"),
				      IvyXml.getAttrString(xml,"NEWNAME"),
				      IvyXml.getAttrBool(xml,"REPLACEDUPS",false),
				      IvyXml.getAttrBool(xml,"COMMENTS",false),
				      IvyXml.getAttrBool(xml,"EXCEPTIONS",false),xw);
    }
   else if (cmd.equals("FORMATCODE")) {
      bedrock_editor.formatCode(proj,IvyXml.getAttrString(xml,"BID","*"),
	    IvyXml.getAttrString(xml,"FILE"),
	    IvyXml.getAttrInt(xml,"START"),
	    IvyXml.getAttrInt(xml,"END"),xw);
    }
   else if (cmd.equals("FINDREGIONS")) {
      bedrock_editor.getTextRegions(proj,IvyXml.getAttrString(xml,"BID","*"),
	    IvyXml.getAttrString(xml,"FILE"),
	    IvyXml.getAttrString(xml,"CLASS"),
	    IvyXml.getAttrBool(xml,"PREFIX",false),
	    IvyXml.getAttrBool(xml,"STATICS",false),
	    IvyXml.getAttrBool(xml,"COMPUNIT",false),
	    IvyXml.getAttrBool(xml,"IMPORTS",false),
	    IvyXml.getAttrBool(xml,"PACKAGE",false),
	    IvyXml.getAttrBool(xml,"TOPDECLS",false),
	    IvyXml.getAttrBool(xml,"FIELDS",false),
	    IvyXml.getAttrBool(xml,"ALL",false),xw);
    }
   else if (cmd.equals("FINDBYKEY")) {
      bedrock_editor.findByKey(proj,
				IvyXml.getAttrString(xml,"BID","*"),
				IvyXml.getAttrString(xml,"KEY"),
				IvyXml.getAttrString(xml,"FILE"), xw);
    }
   else if (cmd.equals("CALLPATH")) {
      bedrock_call.getCallPath(proj,IvyXml.getAttrString(xml,"FROM"),
				  IvyXml.getAttrString(xml,"TO"),
				  IvyXml.getAttrBool(xml,"SHORTEST",false),
				  IvyXml.getAttrInt(xml,"LEVELS",0),xw);
    }
   else if (cmd.equals("FIXIMPORTS")) {
      bedrock_editor.fixImports(proj,
	    IvyXml.getAttrString(xml,"BID","*"),
	    IvyXml.getAttrString(xml,"FILE"),
	    IvyXml.getAttrInt(xml,"DEMAND",0),
	    IvyXml.getAttrInt(xml,"STATICDEMAND",0),
	    IvyXml.getAttrString(xml,"ORDER"),
            IvyXml.getAttrString(xml,"ADD"),
            xw);;
    }
   else if (cmd.equals("PREFERENCES")) {
      bedrock_project.handlePreferences(proj,xw);
    }
   else if (cmd.equals("SETPREFERENCES")) {
      Element pxml = IvyXml.getChild(xml,"profile");
      if (pxml == null) pxml = IvyXml.getChild(xml,"OPTIONS");
      bedrock_project.handleSetPreferences(proj,pxml,xw);
    }
   else if (cmd.equals("LOGLEVEL")) {
      log_level = IvyXml.getAttrEnum(xml,"LEVEL",BedrockLogLevel.ERROR);
    }
   else if (cmd.equals("MONITOR")) {
      monitor_on = IvyXml.getAttrBool(xml, "ON");
      if (bedrock_monitor != null) {
	 if (monitor_on && workbench_inited) bedrock_monitor.startMonitoring();
	 else bedrock_monitor.stopMonitoring();
       }
   }
   else if (cmd.equals("GETHOST")) {
      String h1 = null;
      String h2 = null;
      String h3 = null;
      try {
	 InetAddress lh = InetAddress.getLocalHost();
	 h1 = lh.getHostAddress();
	 h2 = lh.getHostName();
	 h3 = lh.getCanonicalHostName();
       }
      catch (IOException e) { }
      if (h1 != null) xw.field("ADDR",h1);
      if (h2 != null) xw.field("NAME",h2);
      if (h3 != null) xw.field("CNAME",h3);
    }
   else if (cmd.equals("GETPROXY")) {
      String h1 = IvyXml.getAttrString(xml,"HOST");
      getProxyForHost(h1,xw);
    }
   else if (cmd.equals("QUICKFIX")) {
      // done in the front end for now
      // bedrock_editor.handleCommit(proj,null,false,false,null,null);
      quick_fixer.handleQuickFix(proj,IvyXml.getAttrString(xml,"BID","*"),
				    IvyXml.getAttrString(xml,"FILE"),
				    IvyXml.getAttrInt(xml,"OFFSET"),
				    IvyXml.getAttrInt(xml,"LENGTH"),
				    getElements(xml,"PROBLEM"),xw);
    }
   else if (cmd.equals("ENTER")) {
      BedrockApplication.enterApplication();
      ++num_clients;
      xw.text(Integer.toString(num_clients));
    }
   else if (cmd.equals("EXIT")) {
      if (--num_clients <= 0) {
	 xw.text("EXITING");
	 forceExit();
       }
    }
   else {
      xw.close();
      throw new BedrockException("Unknown plugin command " + cmd);
    }

   xw.end("RESULT");

   long delta = System.currentTimeMillis() - start;

   BedrockPlugin.logD("Result (" + delta + ") = " + xw.toString());
   return xw.toString();
}



void forceExit()
{
   logD("FORCE EXIT");
   doing_exit = true;
   saveEclipse();
   BedrockApplication.stopApplication();
   if (bedrock_monitor != null) {
      bedrock_monitor.stopMonitoring();
      bedrock_monitor.deleteLogFile();
    }
   BedrockPlugin.logD("Stopping application");
   shutdown_mint = true;
}




private Set<String> getSet(Element xml,String key)
{
   Set<String> items = null;

   for (Element c : IvyXml.children(xml,key)) {
      String v = IvyXml.getText(c);
      if (v == null || v.length() == 0) continue;
      if (items == null) items = new HashSet<String>();
      items.add(v);
    }

   return items;
}



private List<Element> getElements(Element xml,String key)
{
   List<Element> elts = null;

   for (Element c : IvyXml.children(xml,key)) {
      if (elts == null) elts = new ArrayList<Element>();
      elts.add(c);
    }

   return elts;
}




/********************************************************************************/
/*										*/
/*	Methods for handling edits						*/
/*										*/
/********************************************************************************/

private List<EditData> getEditSet(Element xml)
{
   List<EditData> edits = new ArrayList<EditData>();

   for (Element c : IvyXml.children(xml,"EDIT")) {
      EditDataImpl edi = new EditDataImpl(c);
      edits.add(edi);
    }

   return edits;
}



private static class EditDataImpl implements EditData {

   private int start_offset;
   private int end_offset;
   private String edit_text;

   EditDataImpl(Element e) {
      start_offset = IvyXml.getAttrInt(e,"START");
      end_offset = IvyXml.getAttrInt(e,"END",start_offset);
      edit_text = IvyXml.getText(e);
      if (edit_text != null && edit_text.length() == 0) edit_text = null;
      if (edit_text != null && IvyXml.getAttrBool(e,"ENCODE")) {
	 byte [] bytes = IvyXml.stringToByteArray(edit_text);
	 edit_text = new String(bytes);
       }
    }

   public int getOffset()			{ return start_offset; }
   public int getLength()			{ return end_offset - start_offset; }
   public String getText()			{ return edit_text; }

}	// end of innerclass EditDataImpl




/********************************************************************************/
/*										*/
/*	Methods for handling proxy requests					*/
/*										*/
/********************************************************************************/

private void getProxyForHost(String host,IvyXmlWriter xw)
{
   try {
      Bundle bdl = getBundle();
      if (bdl == null) return;
      BundleContext ctx = bdl.getBundleContext();
      ServiceReference<?> svr = ctx.getServiceReference("org.eclipse.core.net.proxy.IProxyService");
      if (svr == null) return;
      IProxyService ips = (IProxyService) ctx.getService(svr);
      if (ips == null) return;

      URI uri = new URI(host);
      IProxyData [] pds = ips.select(uri);
      for (IProxyData pd : pds) {
	 xw.begin("PROXY");
	 xw.field("TYPE",pd.getType());
	 xw.field("PORT",pd.getPort());
	 xw.field("HOST",pd.getHost());
	 if (pd.isRequiresAuthentication()) {
	    xw.field("USER",pd.getUserId());
	    xw.field("PWD",pd.getPassword());
	  }
	 xw.end("PROXY");
       }
    }
   catch (Throwable t) {
      logD("Problem getting proxy information for " + host + ": " + t);
    }
}




/********************************************************************************/
/*										*/
/*	Mint handlers								*/
/*										*/
/********************************************************************************/

private class CommandHandler implements MintHandler {

   public void receive(MintMessage msg, MintArguments args) {
      String cmd = args.getArgument(1);
      Element xml = msg.getXml();
      String proj = IvyXml.getAttrString(xml,"PROJECT");

      String rslt = null;

      try {
	 rslt = handleCommand(cmd,proj,xml);
       }
      catch (BedrockException e) {
	 String xmsg = "BEDROCK: error in command " + cmd + ": " + e;
	 BedrockPlugin.logE(xmsg,e);
	 rslt = "<ERROR><![CDATA[" + xmsg + "]]></ERROR>";
       }
      catch (Throwable t) {
	 String xmsg = "BEDROCK: Problem processing command " + cmd + ": " + t + " " +
	    doing_exit + " " + shutdown_mint + " " +  num_clients;
	 BedrockPlugin.logE(xmsg);
	 System.err.println(xmsg);
	 t.printStackTrace();
	 StringWriter sw = new StringWriter();
	 PrintWriter pw = new PrintWriter(sw);
	 t.printStackTrace(pw);
	 Throwable xt = t;
	 for (	; xt.getCause() != null; xt = xt.getCause());
	 if (xt != null && xt != t) {
	    rslt += "\n";
	    xt.printStackTrace(pw);
	  }
	 BedrockPlugin.logE("TRACE: " + sw.toString());
	 rslt = "<ERROR>";
	 rslt += "<MESSAGE>" + xmsg + "</MESSAGE>";
	 rslt += "<EXCEPTION><![CDATA[" + t.toString() + "]]></EXCEPTION>";
	 rslt += "<STACK><![CDATA[" + sw.toString() + "]]></STACK>";
	 rslt += "</ERROR>";
       }

      msg.replyTo(rslt);

      if (shutdown_mint) mint_control.shutDown();
    }

}	// end of subclass CommandHandler





/********************************************************************************/
/*										*/
/*	Logging methods 							*/
/*										*/
/********************************************************************************/

static void logE(String msg,Throwable t) { log(BedrockLogLevel.ERROR,msg,t); }

static void logE(String msg)		{ log(BedrockLogLevel.ERROR,msg,null); }

static void logW(String msg)		{ log(BedrockLogLevel.WARNING,msg,null); }

static void logI(String msg)		{ log(BedrockLogLevel.INFO,msg,null); }

static void logD(String msg)		{ log(BedrockLogLevel.DEBUG,msg,null); }

static void logX(String msg)
{
   try {
      throw new Error();
    }
   catch (Error x) {
      log(BedrockLogLevel.DEBUG,msg,x);
    }
}

static void logEX(String msg)
{
   try {
      throw new Error();
    }
   catch (Error x) {
      log(BedrockLogLevel.ERROR,msg,x);
    }
}



static void log(String msg)		{ logI(msg); }



static void log(BedrockLogLevel lvl,String msg,Throwable t)
{
   if (lvl.ordinal() > log_level.ordinal()) return;

   String pfx = "BEDROCK:" + lvl.toString().substring(0,1) + ": ";

   if (log_file != null) {
      log_file.println(pfx + msg);
      if (t != null) {
	 t.printStackTrace(log_file);
	 Throwable r = null;
	 for (r = t.getCause(); r != null && r.getCause() != null; r = r.getCause());
	 if (r != null) r.printStackTrace(log_file);
       }
    }
   if (use_stderr || log_file == null) {
      System.err.println(pfx + msg);
      if (t != null) t.printStackTrace();
    }
   if (log_file != null) log_file.flush();
}



}	// end of class BedrockPlugin




/* end of BedrockPlugin.java */

