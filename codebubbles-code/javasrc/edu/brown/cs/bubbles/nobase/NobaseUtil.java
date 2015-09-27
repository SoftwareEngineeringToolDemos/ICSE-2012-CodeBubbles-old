/********************************************************************************/
/*                                                                              */
/*              NobaseUtil.java                                                 */
/*                                                                              */
/*      Utility and output methods for NOBASE                                   */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.nobase;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import java.io.File;
import java.util.List;


abstract class NobaseUtil implements NobaseConstants
{



/********************************************************************************/
/*                                                                              */
/*      Problem output methods                                                  */
/*                                                                              */
/********************************************************************************/

static void outputProblem(NobaseMessage m,ISemanticData isd,IvyXmlWriter xw)
   {
      NobaseFile ifd = isd.getFileData();
      IDocument doc = ifd.getDocument();
      int sln = m.getStartLine(doc);
      int scl = m.getStartCol(doc);
      int eln = m.getEndLine(doc);
      int ecl = m.getEndCol(doc);
      String msg = m.getMessage();
      List<String> ls = m.getAdditionalInfo();
      
      xw.begin("PROBLEM");
      xw.field("PROJECT",isd.getProject().getName());
      xw.field("FILE",ifd.getFile().getPath());
     // xw.field("MSGID",m.getType());
      xw.field("MESSAGE",msg);
      xw.field("LINE",sln);
      
      switch (m.getSeverity()) {
         case INFO :
         default :
            break;
         case WARNING :
            xw.field("WARNING",true);
            break;
         case ERROR :
            xw.field("ERROR",true);
            break;
       }
      
      try {
         if (sln != 0) {
            xw.field("START",doc.getLineOffset(sln-1) + scl - 1);
            xw.field("END",doc.getLineOffset(eln-1) + ecl - 1);
          }
       }
      catch (BadLocationException e) { }
      
      if (ls != null) {
         for (String s : ls) {
            xw.textElement("ARG",s);
          }
       }
      xw.end("PROBLEM");
    }
   


/********************************************************************************/
/*                                                                              */
/*      Symbol otuput methods                                                   */
/*                                                                              */
/********************************************************************************/

static void outputProjectSymbol(NobaseProject pp,IvyXmlWriter xw)
{
   xw.begin("ITEM");
   xw.field("TYPE","Project");
   xw.field("NAME",pp.getName());
   xw.field("PROJECT",pp.getName());
   xw.field("PATH",pp.getBasePath().getAbsolutePath());
   xw.field("SOURCE","USERSOURCE");
   xw.field("KEY",pp.getName() + "@");
   xw.end("ITEM");
}


static void outputModuleSymbol(NobaseProject pp,IvyXmlWriter xw,NobaseFile file,
      NobaseAst.NobaseAstNode root)
{
   xw.begin("ITEM");
   if (pp != null) xw.field("PROJECT",pp.getName());
   xw.field("PATH",file.getFile().getAbsolutePath());
   String tnm = file.getModuleName();
   xw.field("NAME",tnm);
   xw.field("TYPE","Module");
   if (root != null) {
      xw.field("LINE",root.getStartLine(file));
      xw.field("COL",root.getStartChar(file));
      int off1 = root.getStartPosition(file);
      int off2 = root.getEndPosition(file);
      xw.field("STARTOFFSET",off1);
      xw.field("ENDOFFSET",off2);
      xw.field("LENGTH",off2-off1+1);
    }
   xw.field("HANDLE",tnm);
   xw.end("ITEM");
}



static void outputName(NobaseSymbol nm,IvyXmlWriter xw)
{
   xw.begin("ITEM");
   xw.field("PROJECT",nm.getProject().getName());
   NobaseFile file = nm.getFileData();
   xw.field("PATH",file.getFile().getAbsolutePath());
   String dnm = nm.getName();
   xw.field("NAME",dnm);
   switch (nm.getNameType()) {
      case MODULE :
         xw.field("TYPE","Module");
         break;
      case FUNCTION :
         xw.field("TYPE","Function");
         break;
      case LOCAL :
         xw.field("TYPE","Local");
         break;
      case VARIABLE :
         xw.field("TYPE","Variable");
         break;
    }
   NobaseAst.NobaseAstNode root = nm.getDefNode();
   if (root != null) {
      xw.field("LINE",root.getStartLine(file));
      xw.field("COL",root.getStartChar(file));
      int off1 = root.getStartPosition(file);
      int off2 = root.getEndPosition(file);
      xw.field("STARTOFFSET",off1);
      xw.field("ENDOFFSET",off2);
      xw.field("LENGTH",off2-off1+1);
    }
   xw.field("QNAME",nm.getBubblesName());
   xw.field("HANDLE",nm.getHandle());
   xw.end("ITEM");
}



/********************************************************************************/
/*                                                                              */
/*      Search support                                                          */
/*                                                                              */
/********************************************************************************/

/********************************************************************************/
/*										*/
/*	Handle matching 							*/
/*										*/
/********************************************************************************/

static String convertWildcardToRegex(String s)
{
   if (s == null) return null;
   
   StringBuffer nb = new StringBuffer(s.length()*8);
   int brct = 0;
   boolean qtfg = false;
   boolean bkfg = false;
   String star = null;
   
   star = "\\w*";
   
   nb.append('^');
   
   for (int i = 0; i < s.length(); ++i) {
      char c = s.charAt(i);
      if (bkfg) {
	 if (c == '\\') qtfg = true;
	 else if (!qtfg && c == ']') bkfg = false;
	 else { nb.append(c); qtfg = false; continue; }
       }
      if (c == '/' || c == '\\') {
	 if (File.separatorChar == '\\') nb.append("\\\\");
	 else nb.append(File.separatorChar);
       }
      else if (c == '@') nb.append(".*");
      else if (c == '*') nb.append(star);
      else if (c == '.') nb.append("\\.");
      else if (c == '{') { nb.append("("); ++brct; }
      else if (c == '}') { nb.append(")"); --brct; }
      else if (brct > 0 && c == ',') nb.append('|');
      else if (c == '?') nb.append(".");
      else if (c == '[') { nb.append(c); bkfg = true; }
      else nb.append(c);
    }
   
   nb.append('$');
   
   return nb.toString();
}





}       // end of class NobaseUtil




/* end of NobaseUtil.java */

