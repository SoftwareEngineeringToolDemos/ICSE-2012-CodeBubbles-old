/********************************************************************************/
/*										*/
/*		BedrockOpenEditorBubbleActionDelegate.java						*/
/*										*/
/*	Bubbles Environment Documentation repository of available javadocs	*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Hsu-Sheng Ko 	      */
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

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.*;
import org.eclipse.ui.texteditor.ITextEditor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BedrockOpenEditorBubbleActionDelegate implements IWorkbenchWindowActionDelegate
{

private IWorkbenchWindow our_window;

@Override public void init(IWorkbenchWindow window) {
   our_window = window;
}

@Override public void dispose() {}

@Override public void run(IAction action)
{
   IWorkbenchPage page = our_window.getActivePage();

   if (page != null) {
      if (!(page.getActiveEditor() instanceof ITextEditor))
	 return;

      ITextEditor fileEditor = (ITextEditor)page.getActiveEditor();

      IFileEditorInput fileEditorInput = (IFileEditorInput)fileEditor.getEditorInput();
      String path = fileEditorInput.getFile().getProjectRelativePath().toOSString();
      String filePath = path;
      IProject project = fileEditorInput.getFile().getProject();

      IJavaProject javaProject = JavaModelManager.getJavaModelManager().getJavaModel().getJavaProject(project.getName());

      try {
	 for(IClasspathEntry entry : javaProject.getRawClasspath()) {
	    if(entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
	       String sourcePath = entry.getPath().toOSString().substring(project.getName().length() + 2);

	       if(path.startsWith(sourcePath)) {
		  path = path.substring(sourcePath.length() + 1);
		  path = path.replace(File.separatorChar, '$');
		  path = path.substring(0, path.indexOf("."));

		  filePath = filePath.substring(sourcePath.length() + 1);

		  break;
		}
	     }
	  }
       }
      catch (Exception e1) {
	 BedrockPlugin.log("Exception : " + e1.getMessage() + ", " + e1.getClass().toString());
       }

      try {
	 IJavaElement javaElement = javaProject.findElement(new Path(filePath));

	 if(!(javaElement instanceof ICompilationUnit))
	    return;

	 ICompilationUnit icu = (ICompilationUnit)javaElement;

	 ISelectionProvider selectionProvider = fileEditor.getSelectionProvider();
	 ISelection selection = selectionProvider.getSelection();

	 if (selection instanceof ITextSelection) {
	    ITextSelection textSelection = (ITextSelection)selection;
	    int offset = textSelection.getOffset();

	    IJavaElement element = icu.getElementAt(offset);

	    IvyXmlWriter xw = BedrockPlugin.getPlugin().beginMessage("OPENEDITOR");
	    xw.field("PROJECT", project.getName());

	    if (element == null) {
	       xw.field("RESOURCEPATH", path);
	     }
	    else {
	       boolean isFirstElement = true;
	       boolean isMethod = false;

	       String fileName = path.substring(path.lastIndexOf('$') + 1);

	       List<String> list = new ArrayList<String>();

	       while (element != null && (!element.getElementName().equals(fileName) || element.getElementType() == IJavaElement.METHOD)) {
		  if (isFirstElement && (element.getElementType() == IJavaElement.METHOD || element.getElementType() == IJavaElement.TYPE)) {
		     list.add(element.getElementName());

		     if (element.getElementType() == IJavaElement.METHOD) {
			isMethod = true;
		      }

		     isFirstElement = false;
		   }
		  else if (!isFirstElement) {
		     list.add(element.getElementName());
		   }

		  element = element.getParent();

		  if ("".equals(element.getElementName())) {
		     xw.field("RESOURCEPATH", path);
		     BedrockPlugin.getPlugin().finishMessage(xw);

		     return;
		   }
		}

	       String[] aryPath = new String[list.size()];
	       list.toArray(aryPath);
				
	       for(int i=aryPath.length-1; i>=0; i--) {
		  path += ("$" + aryPath[i]);
		}

	       xw.field("RESOURCEPATH", path);

	       if (isMethod)
		  xw.field("RESOURCETYPE", "Function");
	     }

	    BedrockPlugin.getPlugin().finishMessage(xw);
	 }
      }
      catch (Exception e2) {
	 BedrockPlugin.log("Exception : " + e2.getMessage() + ", " + e2.getClass().toString());
       }
    }
}



@Override public void selectionChanged(IAction action, ISelection selection) {}



}	// end of class BedrockOpenEditorBubbleActionDelegate




/* end of BedrockOpenEditorBubbleActionDelegate.java */
