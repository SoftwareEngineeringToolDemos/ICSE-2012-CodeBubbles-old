/********************************************************************************/
/*										*/
/*		NobaseFile.java 						*/
/*										*/
/*	Implementation of a file						*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.nobase;

import org.eclipse.jface.text.*;

import java.io.*;
import java.util.IdentityHashMap;
import java.util.Map;


class NobaseFile implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String module_name;
private File for_file;
private NobaseProject for_project;
private IDocument use_document;
private Map<Object,int []> position_data;
private boolean has_changed;
private long last_modified;
private boolean is_library;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseFile(File f,String nm,NobaseProject pp)
{
   for_file = f;
   for_project = pp;
   use_document = new Document();
   module_name = nm;
   loadFile();
   position_data = new IdentityHashMap<Object,int []>();
   has_changed = false;
   last_modified = 0;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

File getFile()				{ return for_file; }
NobaseProject getProject()		{ return for_project; }
IDocument getDocument() 		{ return use_document; }
String getModuleName()			{ return module_name; }
boolean hasChanged()			{ return has_changed; }
void markChanged()			{ has_changed = true; }
long getLastDateLastModified()		{ return last_modified; }
String getFileName()			{ return for_file.getPath(); }
boolean isLibrary()			{ return is_library; }
void setIsLibrary(boolean fg)		{ is_library = fg; }
String getContents()			{ return use_document.get(); }



/********************************************************************************/
/*										*/
/*	Handle positions							*/
/*										*/
/********************************************************************************/

void clearPositions()			{ position_data.clear(); }


void setStart(Object o,int line,int col)
{
   if (line == 0) return;
   int off = 0;
   try {
      off = use_document.getLineOffset(line-1) + col-1;
    }
   catch (BadLocationException e) {
      NobaseMain.logE("Bad location for start offset",e);
      return;
    }
   // System.err.println("SET START " + line + " " + col + " " + off + " " + o);
   int [] offs = position_data.get(o);
   if (offs == null) {
      offs = new int [2];
      position_data.put(o,offs);
    }
   offs[0] = off;
}



void setEnd(Object o,int line,int col)
{
   if (line == 0) return;
   int off = 0;
   try {
      off = use_document.getLineOffset(line-1) + col-1;
    }
   catch (BadLocationException e) {
      NobaseMain.logE("Bad location for end offset",e);
      return;
    }
   setEnd(o,off);
}



void setEndFromStart(Object o,int line,int col)
{
   if (line == 0) return;
   int off = 0;
   try {
      off = use_document.getLineOffset(line-1) + col-1;
      if (col == 1) off -= 2;
      else off -= 1;
    }
   catch (BadLocationException e) {
      NobaseMain.logE("Bad location for end offset",e);
      return;
    }
   setEnd(o,off);
}




void setEnd(Object o,int off)
{
   // System.err.println("SET END " + off + " " + o);
   int [] offs = position_data.get(o);
   if (offs == null) {
      offs = new int[2];
      position_data.put(o,offs);
    }
   offs[1] = off;
}


int getStartOffset(Object o)
{
   int [] offs = position_data.get(o);
   if (offs == null) return 0;
   return offs[0];
}

int getEndOffset(Object o)
{
   int [] offs = position_data.get(o);
   if (offs == null) return 0;
   return offs[1];
}

int getLength(Object o)
{
   int [] offs = position_data.get(o);
   if (offs == null) return 0;
   return offs[1] - offs[0] + 1;
}


int getLineNumber(int offset)
{
   if (use_document == null) return 0;
   try {
      return use_document.getLineOfOffset(offset)+1;
    }
   catch (BadLocationException e) {
      NobaseMain.logE("Bad line offset " + offset + " " + use_document.getLength());
    }
   return 0;
}

int getCharPosition(int offset)
{
   if (use_document == null) return 0;
   try {
      int lno = use_document.getLineOfOffset(offset);
      int lstart = use_document.getLineOffset(lno);
      return offset-lstart+1;
    }
   catch (BadLocationException e) {
      NobaseMain.logE("Bad line offset " + offset + " " + use_document.getLength());
    }
   return 0;
}


/********************************************************************************/
/*										*/
/*	Load and save methods							*/
/*										*/
/********************************************************************************/

void reload()
{
   loadFile();
}



private void loadFile()
{
   try {
      FileReader fr = new FileReader(for_file);
      last_modified = for_file.lastModified();
      StringBuffer fbuf = new StringBuffer();
      char [] buf = new char[4096];
      for ( ; ; ) {
	 int sts = fr.read(buf);
	 if (sts < 0) break;
	 fbuf.append(buf,0,sts);
       }
      use_document.set(fbuf.toString());
      fr.close();
    }
   catch (IOException e) {
      NobaseMain.logE("Problem reading file",e);
    }
}



boolean commit(boolean refresh,boolean save)
{
   boolean upd = false;
   if (refresh) {
      long lm = for_file.lastModified();
      if (lm > last_modified) {
	 loadFile();
	 upd = true;
       }
    }
   else if (save && has_changed) {
      try {
	 FileWriter fw = new FileWriter(for_file);
	 fw.write(use_document.get());
	 fw.close();
	 last_modified = for_file.lastModified();
       }
      catch (IOException e) {
	 NobaseMain.logE("Problem saving file",e);
       }
    }
   has_changed = false;
   return upd;
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return for_file.getPath();
}


}	// end of class NobaseFile




/* end of NobaseFile.java */

