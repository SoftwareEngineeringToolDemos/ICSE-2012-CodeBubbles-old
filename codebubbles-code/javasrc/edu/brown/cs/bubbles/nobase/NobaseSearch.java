/********************************************************************************/
/*                                                                              */
/*              NobaseSearch.java                                               */
/*                                                                              */
/*      Search management for nobase                                            */
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

import org.eclipse.jface.text.IDocument;

import java.util.List;
import java.util.regex.*;




class NobaseSearch implements NobaseConstants, NobaseAst
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private NobaseMain      nobase_main;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

NobaseSearch(NobaseMain nm)
{
   nobase_main = nm;
}





/********************************************************************************/
/*										*/
/*	Text Search commands							*/
/*										*/
/********************************************************************************/

void handleTextSearch(String proj,int fgs,String pat,int maxresult,IvyXmlWriter xw)
throws NobaseException
{
   Pattern pp = null;
   try {
      pp = Pattern.compile(pat,fgs);
    }
   catch (PatternSyntaxException e) {
      pp = Pattern.compile(pat,fgs|Pattern.LITERAL);
    }

   Pattern filepat = null;

   List<ISemanticData> sds = nobase_main.getProjectManager().getAllSemanticData(proj);
   int rct = 0;
   for (ISemanticData sd : sds) {
      NobaseFile ifd = sd.getFileData();
      if (filepat != null) {
	 String fnm = ifd.getFile().getPath();
	 Matcher m = filepat.matcher(fnm);
	 if (!m.matches()) continue;
       }
      IDocument d = ifd.getDocument();
      String s = d.get();
      Matcher m = pp.matcher(s);
      while (m.find()) {
	 if (++rct > maxresult) break;
	 xw.begin("MATCH");
	 xw.field("STARTOFFSET",m.start());
	 xw.field("LENGTH",m.end() - m.start());
	 xw.field("FILE",ifd.getFile().getPath());
	 FindOuterVisitor ov = new FindOuterVisitor(ifd,m.start(),m.end());
	 NobaseAstNode root = sd.getRootNode();
	 try {
	    root.accept(ov);
	    NobaseAstNode itm = ov.getItem();
	    if (itm != null) {
               NobaseSymbol sym = itm.getDefinition();
	       if (sym != null) {
                  NobaseUtil.outputName(sym,xw);
		}
	     }
	  }
	 catch (Exception e) { }
	 // find method here
	 xw.end("MATCH");
       }
    } 
}



private class FindOuterVisitor extends NobaseAstVisitor {

   private NobaseFile for_file;
   private int start_offset;
   private int end_offset;
   private NobaseAstNode item_found;

   FindOuterVisitor(NobaseFile fd,int start,int end) {
      for_file = fd;
      start_offset = start;
      end_offset = end;
      item_found = null;
    }

   NobaseAstNode getItem() 		{ return item_found; }

   @Override public boolean visit(FileModule n) {
      return checkNode(n);
    }

   @Override public boolean visit(FunctionConstructor n) {
      return checkNode(n);
    }

   private boolean checkNode(NobaseAstNode n) {
      int soff = for_file.getStartOffset(n);
      int eoff = for_file.getEndOffset(n);
      if (soff < start_offset && eoff > end_offset) {
         item_found = n;
       }
      if (start_offset > eoff) return false;
      if (end_offset < soff) return false;
      return true;
    }

}	// end of inner class FindOuterVisitor









}       // end of class NobaseSearch




/* end of NobaseSearch.java */

