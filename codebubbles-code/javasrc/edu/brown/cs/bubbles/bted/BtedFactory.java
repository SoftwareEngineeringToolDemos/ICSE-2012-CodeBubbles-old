/********************************************************************************/
/*										*/
/*		BtedFactory.java						*/
/*										*/
/*	Bubble Environment text editor facility factory 			*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Adam M. Cook 			*/
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



package edu.brown.cs.bubbles.bted;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.buda.BudaConstants.BudaBubblePosition;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.burp.BurpHistory;

import javax.swing.JEditorPane;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import java.awt.Point;
import java.io.*;
import java.util.*;


public class BtedFactory implements BtedConstants, BumpConstants,
	 BudaConstants.ButtonListener {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private HashMap<String, Document>      active_documents;
private HashMap<Document, Integer>     document_count;

private static HashMap<String, String> file_extensions = new HashMap<String,String>();

private static BtedFactory	     the_factory;
private static BoardProperties	 bted_props = BoardProperties.getProperties("Bted");



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 *	Return the singleton instance of the breakpoint factory.
 **/
public static BtedFactory getFactory()
{
   return the_factory;
}


private BtedFactory()
{
   active_documents = new HashMap<String, Document>();
   document_count = new HashMap<Document, Integer>();
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

/**
 *	Setup the module.  This should be called from the initialization module
 *	(i.e. listed in the initialization setup).  It will register any buttons and
 *	bubble configurators that are needed.
 **/

public static void setup()
{
   BumpClient.getBump();
   the_factory = new BtedFactory();
   BudaRoot.addBubbleConfigurator("BTED", new BtedConfigurator());
   BudaRoot.registerMenuButton(NEW_FILE_BUTTON, the_factory);
   BudaRoot.registerMenuButton(LOAD_FILE_BUTTON, the_factory);
}



/********************************************************************************/
/*										*/
/*	Document methods							*/
/*										*/
/********************************************************************************/

/**
 * Extracts the file extensions from the string that contains the file extensions
 *
 * @param String containing file extensions seperated by spaces, commas, or semicolons
 * @return a vector containing each file extension as a string
 */
private Vector<String> getExtensions(String str)
{
   str = str.toLowerCase();
   Vector<String> rslt = new Vector<String>();
   boolean inExt = false;
   int start = 0;
   for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      if (ch == '.') {
	 inExt = true;
	 start = i;
      }
      else if ((ch == ' ' || ch == ',' || ch == ';' || ch == '\n' || ch == '\r' || ch == Character.MIN_VALUE)
	       && inExt) {
	 inExt = false;
	 rslt.add(str.substring(start, i));
      }
      else if (i == str.length() - 1 && inExt) {
	 rslt.add(str.substring(start));
      }
   }
   return rslt;
}



/**
 * Resets the file extension map.  Useful if the user changes the file extension
 * properties.
 */
private void getExtensionsFromProperties()
{
   file_extensions.clear();
   for (String str : getExtensions(bted_props.getString(BASH_EXTENSION))) {
      file_extensions.put(str, "text/bash");
   }
   for (String str : getExtensions(bted_props.getString(C_EXTENSION))) {
      file_extensions.put(str, "text/c");
   }
   for (String str : getExtensions(bted_props.getString(CLOJURE_EXTENSION))) {
      file_extensions.put(str, "text/clojure");
   }
   for (String str : getExtensions(bted_props.getString(CPP_EXTENSION))) {
      file_extensions.put(str, "text/cpp");
   }
   for (String str : getExtensions(bted_props.getString(DOSBATCH_EXTENSION))) {
      file_extensions.put(str, "text/dosbatch");
   }
   for (String str : getExtensions(bted_props.getString(GROOVY_EXTENSION))) {
      file_extensions.put(str, "text/groovy");
   }
   for (String str : getExtensions(bted_props.getString(JAVA_EXTENSION))) {
      file_extensions.put(str, "text/java");
   }
   for (String str : getExtensions(bted_props.getString(JAVASCRIPT_EXTENSION))) {
      file_extensions.put(str, "text/javascript");
   }
   for (String str : getExtensions(bted_props.getString(JFLEX_EXTENSION))) {
      file_extensions.put(str, "text/jflex");
   }
   for (String str : getExtensions(bted_props.getString(LUA_EXTENSION))) {
      file_extensions.put(str, "text/lua");
   }
   for (String str : getExtensions(bted_props.getString(PROPERTIES_EXTENSION))) {
      file_extensions.put(str, "text/properties");
   }
   for (String str : getExtensions(bted_props.getString(PYTHON_EXTENSION))) {
      file_extensions.put(str, "text/python");
   }
   for (String str : getExtensions(bted_props.getString(RUBY_EXTENSION))) {
      file_extensions.put(str, "text/ruby");
   }
   for (String str : getExtensions(bted_props.getString(SCALA_EXTENSION))) {
      file_extensions.put(str, "text/scala");
   }
   for (String str : getExtensions(bted_props.getString(SQL_EXTENSION))) {
      file_extensions.put(str, "text/sql");
   }
   for (String str : getExtensions(bted_props.getString(TAL_EXTENSION))) {
      file_extensions.put(str, "text/tal");
   }
   for (String str : getExtensions(bted_props.getString(XHTML_EXTENSION))) {
      file_extensions.put(str, "text/xhtml");
   }
   for (String str : getExtensions(bted_props.getString(XML_EXTENSION))) {
      file_extensions.put(str, "text/xml");
   }
   for (String str : getExtensions(bted_props.getString(XPATH_EXTENSION))) {
      file_extensions.put(str, "text/xpath");
   }
}



/**
 * Loads a file into the editor.  Keeps track of which files are open.	If a
 * file is opened more than once, the duplicate editor is pointed at the same
 * Document as the original.
 *
 * @param file - the file to load into the editor
 * @param editor - the editor to load the file into
 */

void loadFileIntoEditor(File file,JEditorPane editor,UndoableEditListener listener)
{
   this.getExtensionsFromProperties();

   String path = file.getPath().toLowerCase();
   int index = -1;
   for (int i = path.length() - 1; i >= 0; i--) {
      char ch = path.charAt(i);
      if (ch == '.') {
	 index = i;
	 break;
      }
      else if (ch == '\\' || ch == '/') {
	 break;
      }
    }
   Document od = editor.getDocument();
   Object tabp = od.getProperty(PlainDocument.tabSizeAttribute);
   if (index >= 0) {
      String extension = path.substring(index);
      if (file_extensions.containsKey(extension)) {
	 editor.setContentType(file_extensions.get(extension));
      }
      else editor.setContentType("text/plain");
   }
   else editor.setContentType("text/plain");

   if (active_documents.containsKey(file.getPath())) {
      editor.setDocument(active_documents.get(file.getPath()));
      this.increaseCount(file);
   }
   else {
      try {
	 Reader fileRead = new FileReader(file);
	 editor.read(fileRead, null);
	 active_documents.put(file.getPath(), editor.getDocument());
	 document_count.put(editor.getDocument(), Integer.valueOf(1));
      }
      catch (IOException e) {
	 e.printStackTrace();
      }
   }

   editor.getDocument().putProperty(PlainDocument.tabSizeAttribute,tabp);

   BurpHistory.getHistory().addEditor(editor);
   editor.getDocument().addUndoableEditListener(listener);
}




/**
 * Reopens the old bubble. Necessary to keep track of the number of identical
 * Documents that are open.  It is called when a new file is opened in an old
 * bubble.
 *
 * @param path
 * @param oldBubble
 */
void reopenBubble(String path,BtedBubble oldBubble)
{
   BudaBubble bb = new BtedBubble(path,false);
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(oldBubble);
   BudaRoot br = BudaRoot.findBudaRoot(bba);
   BudaConstraint bc = new BudaConstraint(BudaBubblePosition.MOVABLE,oldBubble.getX(),
	    oldBubble.getY());
   if (bba != null) bba.removeBubble(oldBubble);
   if (br != null) br.add(bb, bc);
}



/**
 * Increases the count to the file.
 *
 * @param file
 */
void increaseCount(File file)
{
   Document doc = active_documents.get(file.getPath());
   int i = document_count.remove(doc);
   i = i + 1;
   document_count.put(doc, i);
}



/**
 * Decreases the count to the file and removes the corresponding document
 * from the map if the count becomes 0.
 *
 * @param file
 */
void decreaseCount(File file)
{
   Document doc = active_documents.get(file.getPath());

   if (doc == null || !document_count.containsKey(doc)) return;

   int i = document_count.remove(doc);
   i = i - 1;
   document_count.put(doc, i);
   if (i == 0) {
      Document removed = active_documents.remove(file.getPath());
      document_count.remove(removed);
   }
}



/**
 * Determines if the file is open
 * @param file
 * @return true if the file is open
 */
boolean isFileOpen(File file)
{
   return active_documents.containsKey(file.getPath());
}



/********************************************************************************/
/*										*/
/*	Menu button handling							*/
/*										*/
/********************************************************************************/

@Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt)
{

   BudaRoot br = BudaRoot.findBudaRoot(bba);
   BudaBubble bb = null;

   try {
      if (id.equals(NEW_FILE_BUTTON)) {
	 bb = new BtedBubble(null,true);
       }
      else if (id.equals(LOAD_FILE_BUTTON)) {
	 bb = new BtedBubble(null,false);
       }
    }
   catch (Throwable t) {
      String msg = "Problem creating text editor bubbles: " + t + "\n";
      String cp = System.getProperty("java.class.path");
      StringTokenizer tok = new StringTokenizer(cp,File.pathSeparator);
      while (tok.hasMoreTokens()) {
	 String f = tok.nextToken();
	 File ff = new File(f);
	 msg += "  CP " + f + " " + ff.exists() + "\n";
       }
      BoardLog.logX("BTED",msg);
    }

   if (br != null && bb != null) {
      BudaConstraint bc = new BudaConstraint(pt);
      br.add(bb, bc);
      bb.grabFocus();
   }

}


}	// end of class BtedFactory




/* end of BtedFactory.java */
