/********************************************************************************/
/*										*/
/*		BedrockEclipseMonitor.java					*/
/*										*/
/*		   Eclipse Monitor						*/
/*										*/
/********************************************************************************/
/*	Copyright 2009-2010 Brown University -- Yu Li				*/
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




package edu.brown.cs.bubbles.bedrock;


import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.mylyn.internal.monitor.ui.MonitorUiPlugin;
import org.eclipse.mylyn.internal.monitor.ui.PerspectiveChangeMonitor;
import org.eclipse.mylyn.monitor.core.*;
import org.eclipse.mylyn.monitor.ui.MonitorUi;
import org.eclipse.ui.*;
import org.eclipse.ui.texteditor.ITextEditor;

import java.io.File;
import java.io.IOException;



class BedrockEclipseMonitor {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private PerspectiveChangeMonitor perspective_monitor;
// private KeybindingCommandMonitor keybinding_monitor;
private MonitorLogger monitor_logger;
private File log_file;
private boolean is_start;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockEclipseMonitor()
{
   IWorkspace ws = ResourcesPlugin.getWorkspace();
   String filename = ws.getRoot().getLocation().append("monitor_log.log").toOSString();
   log_file = new File(filename);
   MonitorUiPlugin.getDefault();	// see if this exists

   is_start = false;
}



/********************************************************************************/
/*										*/
/*	Monitoring controls							*/
/*										*/
/********************************************************************************/

void startMonitoring()
{
   if (!is_start) {
      monitor_logger = new MonitorLogger(log_file);
      // keybinding_monitor = new KeybindingCommandMonitor();
      perspective_monitor = new PerspectiveChangeMonitor();
      MonitorUiPlugin.getDefault().addWindowPerspectiveListener(perspective_monitor);

      monitor_logger.startMonitoring();
      MonitorUi.addInteractionListener(monitor_logger);
      is_start = true;
      BedrockPlugin.log("Start Logging Eclipse Interactions");
    }
}



void stopMonitoring()
{
   if (is_start) {
      // keybinding_monitor = null;
      MonitorUiPlugin.getDefault().removeWindowPerspectiveListener(perspective_monitor);
      MonitorUi.removeInteractionListener(monitor_logger);
      monitor_logger.stopMonitoring();
      is_start = false;
      BedrockPlugin.logD("Stop Logging Eclipse Interactions");
    }
}



synchronized void deleteLogFile()
{
   if (is_start) stopMonitoring();

   if (log_file != null && log_file.exists()) log_file.deleteOnExit();
}




/********************************************************************************/
/*										*/
/*	Logger class								*/
/*										*/
/********************************************************************************/

private static class MonitorLogger extends AbstractMonitorLog
		implements IInteractionEventListener {

   private IWorkbench work_bench = PlatformUI.getWorkbench();
   private IvyXmlWriter xml_writer;

   MonitorLogger(File logfile) {
      outputFile = logfile;
    }

   @Override public synchronized void interactionObserved(InteractionEvent event) {
      try {
	 if (started && xml_writer != null) writeLegacyEvent(event);
       }
      catch (Throwable t) {
	 BedrockPlugin.logW("Logger: Could not log interaction event: " + t);
       }
    }

   @Override public void startMonitoring() {
      try {
	 xml_writer = new IvyXmlWriter(outputFile);
       }
      catch (IOException e) {
	 BedrockPlugin.logE("Unable to create a xml writer: " + e.getMessage());
       }

      if (xml_writer != null) {
	 BedrockPlugin.logI("Start logging Eclipse Interactions");
	 super.startMonitoring();
       }
    }

   @Override public void stopMonitoring() {
      if (xml_writer != null) {
	 xml_writer.flush();
	 xml_writer.close();
       }
      super.stopMonitoring();
      BedrockPlugin.logI("Stop logging Eclipse Interactions");
    }

   private void writeLegacyEvent(InteractionEvent e) {
      xml_writer.begin("INTERACTIONEVENT");
      try {
	 xml_writer.textElement("KIND", e.getKind());
	 xml_writer.textElement("DATE", e.getDate());
	 xml_writer.textElement("ORIGINID", e.getOriginId());
	 xml_writer.textElement("DELTA", e.getDelta());
	 xml_writer.textElement("NAVIGATION", getNavigationDetail());
       }
      finally {
	 xml_writer.end("INTERACTIONEVENT");
	 xml_writer.flush();
       }
    }

   private String getNavigationDetail() {
      String detail = null;
      IWorkbenchPage page = work_bench.getActiveWorkbenchWindow().getActivePage();
      if (page != null) {
	 if (!(page.getActiveEditor() instanceof ITextEditor)) return null;
	 ITextEditor fileeditor = (ITextEditor) page.getActiveEditor();
	 if (!(fileeditor.getEditorInput() instanceof IFileEditorInput)) {
	    return fileeditor.getEditorInput().getName();
	  }

	 IFileEditorInput fileeditorinput = (IFileEditorInput) fileeditor.getEditorInput();
	 String path = fileeditorinput.getFile().getProjectRelativePath().toOSString();
	 String filepath = path;
	 IProject project = fileeditorinput.getFile().getProject();

	 IJavaProject javaproject = JavaModelManager.getJavaModelManager().getJavaModel()
	    .getJavaProject(project.getName());
	 try {
	    for (IClasspathEntry entry : javaproject.getRawClasspath()) {
	       if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
		  String sourcePath = entry.getPath().toOSString()
		     .substring(project.getName().length() + 2);
		  if (path.startsWith(sourcePath)) {
		     path = path.substring(sourcePath.length() + 1);
		     path = path.replace(File.separatorChar, '$');
		     path = path.substring(0, path.indexOf("."));

		     filepath = filepath.substring(sourcePath.length() + 1);
		     break;
		   }
		}
	     }
	  }
	 catch (Throwable e1) {
	    BedrockPlugin.logE("Exception : " + e1.getMessage() + ", " + e1.getClass().toString());
	  }
	 detail = filepath;
	 try {
	    IJavaElement javaElement = javaproject.findElement(new Path(filepath));
	    if(!(javaElement instanceof ICompilationUnit)) return detail;

	    ICompilationUnit icu = (ICompilationUnit)javaElement;
	    ISelectionProvider selectionProvider = fileeditor.getSelectionProvider();
	    ISelection selection = selectionProvider.getSelection();

	    if (selection instanceof ITextSelection) {
	       ITextSelection textSelection = (ITextSelection)selection;
	       int offset = textSelection.getOffset();
	       IJavaElement element = icu.getElementAt(offset);
	       detail += ": " + element.getElementName();
	     }
	  }
	 catch (Throwable e2) {
	    BedrockPlugin.logE("Exception : " + e2.getMessage() + ", " + e2.getClass().toString());
	  }
       }

      return detail;
    }

}	// end of inner class MonitorLogger



}	// end of class BedrockEclipseMonitor




/* end of BedrockEclipseMonitor.java */
