/********************************************************************************/
/*										*/
/*		BaleCorrector.java						*/
/*										*/
/*	Class to handle spelling corrections					*/
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



package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.*;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;



class BaleCorrector implements BaleConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BaleFragmentEditor	for_editor;
private BaleDocument		for_document;
private DocHandler		event_handler;
private ProblemHandler		problem_handler;

private int			start_offset;
private int			end_offset;
private long			start_time;
private int			caret_position;
private Set<BumpProblem>	active_problems;
private Set<String>		imports_added;

private static Map<String,ImportChecker> import_checkers;


private static Contexter	context_handler = null;
private static Map<BaleCorrector,Boolean> all_correctors;

private static int	MAX_REGION_SIZE = 150;
private static boolean		correct_syntax = BALE_PROPERTIES.getBoolean("Bale.correct.syntax");

static {
   all_correctors = new WeakHashMap<BaleCorrector,Boolean>();
   context_handler = new Contexter();
   BaleFactory.getFactory().addContextListener(context_handler);
   import_checkers = new HashMap<String,ImportChecker>();
}





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleCorrector(BaleFragmentEditor ed,boolean auto)
{
   for_editor = ed;
   for_document = ed.getDocument();
   event_handler = null;
   problem_handler = null;

   start_offset = -1;
   end_offset = -1;
   start_time = 0;
   caret_position = -1;
   active_problems = new ConcurrentSkipListSet<BumpProblem>(new ProblemComparator());
   imports_added = new HashSet<String>();

   if (auto) {
      event_handler = new DocHandler();
      problem_handler = new ProblemHandler();
      for_document.addDocumentListener(event_handler);
      for_editor.getEditor().addCaretListener(event_handler);
      BumpClient.getBump().addProblemHandler(for_document.getFile(),problem_handler);
    }

   all_correctors.put(this,Boolean.TRUE);
}




/********************************************************************************/
/*                                                                              */
/*      Edit commands                                                           */
/*                                                                              */
/********************************************************************************/

static void fixErrorsInRegions(BaleDocument ed,int soff,int eoff)
{
   for (BaleCorrector bc : all_correctors.keySet()) {
      if (bc.for_document == ed) {
         bc.fixErrorsInRegion(soff,eoff);
         break;
       }
    }
}



private void fixErrorsInRegion(int startoff,int endoff)
{
   List<BumpProblem> totry = new ArrayList<BumpProblem>();
   BumpClient bc = BumpClient.getBump();
   List<BumpProblem> probs = bc.getProblems(for_document.getFile());
   if (probs.isEmpty()) return;
   for (Iterator<BumpProblem> it = probs.iterator(); it.hasNext(); ) {
      BumpProblem bp = it.next();
      int soff = for_document.mapOffsetToJava(bp.getStart());
      if (soff < startoff || soff > endoff) {
	 it.remove();
	 continue;
       }
      totry.add(bp);
    }
   
   if (totry.isEmpty()) return;
   
   for (BumpProblem bp : totry) {
      List<String> cands = trySpellingProblem(bp);
      if (cands != null) {
	 for (String txt : cands) {
	    BoardLog.logD("BALE","SPELL: HANDLE PROBLEM " + bp.getMessage());
	    int minsize = BALE_PROPERTIES.getInt("Bale.correct.spelling.edits",2);
	    SpellFixer sf = new SpellFixer(bp,txt,minsize);
	    BoardThreadPool.start(sf);
	  }
       }
      else if (trySyntaxProblem(bp)) {
	 BoardLog.logD("BALE","SPELL: HANDLE PROBLEM " + bp.getMessage());
	 SyntaxFixer sf = new SyntaxFixer(bp);
	 BoardThreadPool.start(sf);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Region management							*/
/*										*/
/********************************************************************************/

private void clearRegion()
{
   BoardLog.logD("BALE", "Clear corrector region");
   start_offset = -1;
   end_offset = -1;
   start_time = 0;
   caret_position = -1;
   active_problems.clear();
}


private void handleTyped(int off,int len)
{
   if (!checkFocus()) return;

   if (start_offset < 0) {
      int lno = for_document.findLineNumber(off);	
      start_offset = for_document.findLineOffset(lno);
      start_time = System.currentTimeMillis();
    }
   caret_position = off+len;
   end_offset = Math.max(end_offset,caret_position);

   while (end_offset - start_offset > MAX_REGION_SIZE) {
      int lno = for_document.findLineNumber(start_offset);
      int npos = for_document.findLineOffset(lno+1);
      if (npos < end_offset) {
	 start_offset = npos;
       }
      else break;
    }
}


private void handleBackspace(int off)
{
   if (start_offset < 0) return;
   if (!checkFocus()) return;

   caret_position = off;
   start_offset = Math.min(start_offset,caret_position);
}



private void addProblem(BumpProblem bp)
{
   // must be an error, not a warning
   if (bp.getErrorType() != BumpErrorType.ERROR) return;

   int soff = for_document.mapOffsetToJava(bp.getStart());
   if (start_offset >= 0 && soff >= start_offset && soff < end_offset) {
      BoardLog.logD("BALE","SPELL: consider problem " + bp.getMessage());
      active_problems.add(bp);
    }
}


private void removeProblem(BumpProblem bp)
{
   active_problems.remove(bp);
}


private boolean checkFocus()
{
   BudaBubble bbl = BudaRoot.findBudaBubble(for_editor);
   if (bbl == null) {
      clearRegion();
      return false;
    }
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bbl);
   if (bba.getFocusBubble() == bbl) return true;
   clearRegion();
   return false;
}



/********************************************************************************/
/*										*/
/*	Find something to fix							*/
/*										*/
/********************************************************************************/

private void checkForElementToFix()
{
   List<BumpProblem> totry = new ArrayList<BumpProblem>();

   if (start_offset < 0) return;
   if (active_problems.isEmpty()) return;
   for (Iterator<BumpProblem> it = active_problems.iterator(); it.hasNext(); ) {
      BumpProblem bp = it.next();
      int soff = for_document.mapOffsetToJava(bp.getStart());
      if (soff < start_offset) {
	 it.remove();
	 continue;
       }
      totry.add(bp);
    }

   if (totry.isEmpty()) return;

   for (BumpProblem bp : totry) {
      List<String> cands = trySpellingProblem(bp);
      if (cands != null) {
	 for (String txt : cands) {
	    BoardLog.logD("BALE","SPELL: HANDLE PROBLEM " + bp.getMessage());
	    int minsize = BALE_PROPERTIES.getInt("Bale.correct.spelling.edits",2);
	    SpellFixer sf = new SpellFixer(bp,txt,minsize);
	    BoardThreadPool.start(sf);
	  }
	 break;
       }
      else if (trySyntaxProblem(bp)) {
	 BoardLog.logD("BALE","SPELL: HANDLE PROBLEM " + bp.getMessage());
	 SyntaxFixer sf = new SyntaxFixer(bp);
	 BoardThreadPool.start(sf);
	 break;
      }
    }
}




private List<String> trySpellingProblem(BumpProblem bp)
{
   BoardLog.logD("BALE","SPELL: try problem " + bp.getMessage());
   int soff = for_document.mapOffsetToJava(bp.getStart());
   int eoff = for_document.mapOffsetToJava(bp.getEnd());

  if (eoff == soff && bp.getMessage().startsWith("Syntax error")) return null;
   BaleElement elt = for_document.getCharacterElement(soff);
   // need to have an identifier to correct
   if (!elt.isIdentifier()) return null;
   // can't be working on the identifier at this point
   int elstart = elt.getStartOffset();
   int eloff = elt.getEndOffset();
   if (eoff + 1 != eloff) return null;
   if (end_offset > 0 && eloff + 1 >= end_offset) return null;
   //  if (!elt.isUndefined()) continue;

   String txt = null;
   try {
      txt = for_document.getText(elstart,eloff-elstart);
    }
   catch (BadLocationException e) { }
   if (txt == null) return null;
   List<String> rslt = new ArrayList<String>();
   rslt.add(txt);

   String txt1 = null;
   BaleElement nelt = elt.getNextCharacterElement();
   if (nelt != null && nelt.getTokenType() != BaleTokenType.LANGLE) {
      BaleElement nnelt = nelt.getNextCharacterElement();
      if (nnelt != null && nnelt.isIdentifier() && (nnelt.getStartOffset() - elt.getEndOffset()) == 1) {
	 int neloff = nnelt.getEndOffset();
	 if (end_offset < 0 || neloff + 1 < end_offset) {
	    try {
	       txt1 = for_document.getText(elstart,neloff - elstart);
	    }
	    catch (BadLocationException e) { }
	 }
      }
   }
   if (txt1 != null) rslt.add(txt1);

   return rslt;
}


private boolean trySyntaxProblem(BumpProblem bp)
{
   if (!correct_syntax) return false;

   String msg = bp.getMessage();
   if (!msg.startsWith("Syntax error")) return false;

   int soff = for_document.mapOffsetToJava(bp.getStart());
   int eoff = for_document.mapOffsetToJava(bp.getEnd());
   int lnoerr = for_document.findLineNumber(soff);
   int lnocur = for_document.findLineNumber(caret_position);

   BaleElement elt = for_document.getCharacterElement(soff);
   int eloff = elt.getEndOffset();
   if (eoff + 1 != eloff) return false;
   if (end_offset > 0 && eloff + 1 >= end_offset) return false;
   if (lnoerr == lnocur) return false;

   String txt = null;
   try {
      txt = for_document.getText(soff,eoff-soff+1);
    }
   catch (BadLocationException e) { }
   if (txt == null) return false;

   return true;
}




/********************************************************************************/
/*										*/
/*	String methods								*/
/*										*/
/********************************************************************************/

private static int stringDiff(CharSequence s,CharSequence t)
{
   int n = s.length();
   int m = t.length();
   if (n == 0) return m;
   if (m == 0) return n;

   int [][] d = new int[n+1][m+1];
   for (int i = 0; i <= n; i++) d[i][0] = i;
   for (int j = 0; j <= m; j++) d[0][j] = j;

   for (int i = 1; i <= n; ++i) {
      char s_i = s.charAt(i-1);
      for (int j = 1; j <= m; ++j) {
	 char t_j = t.charAt (j - 1);
	 int cost = (s_i == t_j ? 0 : 1);
	 d[i][j] = min3(d[i-1][j]+1,d[i][j-1]+1,d[i-1][j-1]+cost);
       }
    }

   return d[n][m];
}



private static int min3(int a, int b, int c)
{
   if (b < a) a = b;
   if (c < a) a = c;
   return a;
}



/********************************************************************************/
/*										*/
/*	Error methods								*/
/*										*/
/********************************************************************************/

private static boolean checkProblemPresent(BumpProblem prob,Collection<BumpProblem> bpl)
{
   for (BumpProblem bp : bpl) {
      if (!bp.getProblemId().equals(prob.getProblemId())) continue;
      if (bp.getStart() != prob.getStart()) continue;
      if (bp.getEnd() != prob.getEnd()) continue;
      if (!bp.getFile().equals(prob.getFile())) continue;
      return true;
    }

   return false;
}



private static boolean checkAnyProblemPresent(BumpProblem prob,Collection<BumpProblem> bpl,int sdelta,int edelta)
{
   for (BumpProblem bp : bpl) {
      if (!bp.getFile().equals(prob.getFile())) continue;
      if (bp.getErrorType() != BumpErrorType.ERROR) continue;
      if (bp.getStart() < prob.getEnd()+edelta && bp.getEnd() > prob.getStart()+sdelta) return true;
    }

   return false;
}




private static int getErrorCount(Collection<BumpProblem> bpl)
{
   int ct = 0;

   for (BumpProblem bp : bpl) {
      if (bp.getErrorType() == BumpErrorType.ERROR) ++ct;
    }

   return ct;
}



/********************************************************************************/
/*										*/
/*	Class to find a good fix						*/
/*										*/
/********************************************************************************/

private class SpellFixer implements Runnable {

   private BumpProblem for_problem;
   private String for_identifier;
   private long initial_time;
   private int min_size;

   SpellFixer(BumpProblem bp,String txt,int min) {
      for_problem = bp;
      for_identifier = txt;
      initial_time = start_time;
      min_size = min;
      if (min <= 0) min_size = 2;
    }

   @Override public void run() {
      if (checkSpellingFix()) return;
      if (checkImportFix()) return;
    }

   private boolean checkSpellingFix() {
      // return true if we will try to fix the problem or if problem can't be fixed at all
      String proj = for_document.getProjectName();
      File file = for_document.getFile();
      String filename = file.getAbsolutePath();
      Set<SpellFix> totry = new TreeSet<SpellFix>();
      int minsize = Math.min(min_size, for_identifier.length()-1);
      minsize = Math.min(minsize,(for_identifier.length()+2)/3);

      BumpClient bc = BumpClient.getBump();
      Collection<BumpCompletion> cmps = bc.getCompletions(proj,file,-1,for_problem.getStart());
      for (BumpCompletion bcm : cmps) {
	 String txt = bcm.getCompletion();
	 if (txt == null || txt.length() == 0) continue;
	 int d = stringDiff(for_identifier,txt);
	 if (d <= minsize && d > 0) {
	    BoardLog.logD("BALE","SPELL: Consider replacing " + for_identifier + " WITH " + txt);
	    SpellFix sf = new SpellFix(for_identifier,txt,d);
	    totry.add(sf);
	  }
       }
      if (totry.size() == 0) {
	 cmps = bc.getCompletions(proj,file,-1,for_problem.getStart()+1);
	 for (BumpCompletion bcm : cmps) {
	    String txt = bcm.getCompletion();
	    if (txt == null || txt.length() == 0) continue;
	    int d = stringDiff(for_identifier,txt);
	    if (d <= minsize && d > 0) {
	       BoardLog.logD("BALE","SPELL: Consider replacing " + for_identifier + " WITH " + txt);
	       SpellFix sf = new SpellFix(for_identifier,txt,d);
	       totry.add(sf);
	     }
	  }
       }

      String key = for_identifier;
      if (key.length() > 3) {
	 key = key.substring(0,3) + "*";
	 List<BumpLocation> rslt = bc.findTypes(proj,key);
	 if (rslt != null) {
	    for (BumpLocation bl : rslt) {
	       String nm = bl.getSymbolName();
	       int idx = nm.lastIndexOf(".");
	       if (idx >= 0) nm = nm.substring(idx+1);
	       int d = stringDiff(for_identifier,nm);
	       if (d <= minsize && d > 0) {
		  BoardLog.logD("BALE","SPELL: Consider replacing " + for_identifier + " WITH " + nm);
		  SpellFix sf = new SpellFix(for_identifier,nm,d);
		  totry.add(sf);
		}
	     }
	  }
       }
      Collection<String> keys = BaleTokenizer.getKeywords(for_document.getLanguage());
      for (String s : keys) {
	 int d = stringDiff(for_identifier,s);
	 if (d <= minsize && d > 0) {
	    BoardLog.logD("BALE","SPELL: Consider replacing " + for_identifier + " WITH " + s);
	    SpellFix sf = new SpellFix(for_identifier,s,d);
	    totry.add(sf);
	  }
       }

      // remove problematic cases
      for (Iterator<SpellFix> it = totry.iterator(); it.hasNext(); ) {
	 SpellFix sf = it.next();
	 String txt = sf.getText();
	 if (for_identifier.equals("put") && txt.equals("get")) it.remove();
	 else if (for_identifier.startsWith("set") && txt.startsWith("get")) it.remove();
	 else if (for_identifier.equals("List") && txt.equals("int")) it.remove();
	 else if (for_identifier.equals("is") && txt.equals("if")) it.remove();
	 else if (for_identifier.equals("add") && txt.equals("do")) it.remove();
	 else if (for_identifier.equals("min") && txt.equals("sin")) it.remove();
	 else if (for_identifier.equals(txt + "2D")) it.remove();
	 else if (txt.equals(for_identifier + "2D")) it.remove();
       }

      if (totry.size() == 0) {
	 BoardLog.logD("BALE", "SPELL: No spelling correction found");
	 return false;
       }

      String pid = bc.createPrivateBuffer(proj,filename,null);
      if (pid == null) return true;
      BoardLog.logD("BALE","SPELL: using private buffer " + pid);
      SpellFix usefix = null;

      try {
	 Collection<BumpProblem> probs = bc.getPrivateProblems(filename,pid);
	 if (probs == null) {
	    BoardLog.logE("BALE","SPELL: Problem getting errors for " + pid);
	    return true;
	  }
	 int probct = getErrorCount(probs);
	 if (!checkProblemPresent(for_problem,probs)) {
	    BoardLog.logD("BALE","SPELL: Problem went away");
	    return true;
	  }
	 int soff = for_problem.getStart();
	 int eoff = for_problem.getEnd()+1;

	 for (SpellFix sf : totry) {
	    bc.beginPrivateEdit(filename,pid);
	    BoardLog.logD("BALE","SPELL: Try replacing " + for_identifier + " WITH " + sf.getText());
	    bc.editPrivateFile(proj,file,pid,soff,eoff,sf.getText());
	    probs = bc.getPrivateProblems(filename,pid);
	    bc.beginPrivateEdit(filename,pid);		// undo and wait
	    bc.editPrivateFile(proj,file,pid,soff,soff+sf.getText().length(),for_identifier);
	    bc.getPrivateProblems(filename,pid);
	
	    int edelta = sf.getText().length() - for_identifier.length();
	    if (probs == null || getErrorCount(probs) >= probct) continue;
	    if (checkAnyProblemPresent(for_problem,probs,0,edelta)) continue;
	    if (usefix != null) {
	       if (sf.getEditCount() > usefix.getEditCount()) break;
	       // multiple edits of same length seem to work out -- ignore.
	       return true;
	     }
	    else usefix = sf;
	  }
       }
      finally {
	 bc.removePrivateBuffer(proj,filename,pid);
       }

      if (usefix == null) return false;
      if (start_time != initial_time) return true;
      BoardLog.logD("BALE","SPELL: DO replace " + for_identifier + " WITH " + usefix.getText());
      BoardMetrics.noteCommand("BALE","SPELLFIX");
      SpellDoer sd = new SpellDoer(for_problem,usefix,initial_time);
      SwingUtilities.invokeLater(sd);
      return true;
    }

   private boolean checkImportFix() {
      int soffet = for_document.mapOffsetToJava(for_problem.getStart());
       BaleElement elt = for_document.getCharacterElement(soffet);
       if (!(elt instanceof BaleElement.TypeId)) {
	  if (for_identifier.length() == 0) return false;
	  if (!Character.isUpperCase(for_identifier.charAt(0))) return false;
	  // return false;
       }

      ImportChecker ic = getImportCheckerForProject(for_problem.getProject());
      if (ic == null) return false;
      String type = ic.findImport(for_identifier);
      if (type == null) return false;

      BumpClient bc = BumpClient.getBump();
      String proj = for_document.getProjectName();
      File file = for_document.getFile();
      String filename = file.getAbsolutePath();
      String pid = bc.createPrivateBuffer(proj,filename,null);
      if (pid == null) return true;
      BoardLog.logD("BALE","IMPORT: using private buffer " + pid);
      int inspos = -1;

      try {
	 Collection<BumpProblem> probs = bc.getPrivateProblems(filename,pid);
	 if (probs == null) {
	    BoardLog.logE("BALE","SPELL: Problem getting errors for " + pid);
	    return true;
	  }
	 int probct = getErrorCount(probs);
	 if (!checkProblemPresent(for_problem,probs)) {
	    BoardLog.logD("BALE","SPELL: Problem went away");
	    return true;
	  }
	 inspos = findImportLocation(type);
	 if (inspos < 0) return false;
	 String impstr = "import " + type + ";\n";
	 bc.beginPrivateEdit(filename,pid);
	 BoardLog.logD("BALE","IMPORT:  " + type);
	 bc.editPrivateFile(proj,file,pid,inspos,inspos,impstr);
	 probs = bc.getPrivateProblems(filename,pid);
	 if (probs == null || getErrorCount(probs) >= probct) return false;
	 int delta = impstr.length();
	 if (checkAnyProblemPresent(for_problem,probs,delta,delta)) return false;
       }
      finally {
	 bc.removePrivateBuffer(proj,filename,pid);
       }

      if (inspos < 0) return false;

      if (start_time != initial_time) return true;
      BoardLog.logD("BALE","IMPORT: DO " + type);
      BoardMetrics.noteCommand("BALE","IMPORTFIX");
      ImportDoer id = new ImportDoer(for_problem,type,inspos,initial_time);
      SwingUtilities.invokeLater(id);

      return true;
    }

   private int findImportLocation(String type) {
      int lasteol = -1;
      int preveol = -1;
      boolean inimport = false;
      BaleDocument base = for_document.getBaseEditDocument();
      String body = null;
      try {
	 body = base.getText(0,base.getLength());
       }
      catch (BadLocationException e) {
	 return -1;
       }

      BaleTokenizer tokenizer = BaleTokenizer.create(body,BaleTokenState.NORMAL,BoardLanguage.JAVA);
      BaleToken tok = tokenizer.getNextToken();
      while (tok != null) {
	 switch (tok.getType()) {
	    case KEYWORD :	// public,private, protected, abstract
	    case STATIC :
	    case CLASS :
	    case INTERFACE :
	    case ENUM :
	       if (preveol >= 0) return preveol;
	       if (lasteol >= 0) return lasteol;
	       return 0;
	    case IMPORT:
	       lasteol = -1;
	       preveol = -1;
	       inimport = true;
	       break;
	    case SEMICOLON :
	       if (inimport) {
		  inimport = false;
		}
	       break;
	    case EOL :
	       if (!inimport) {
		  preveol = lasteol;
		  lasteol = tok.getStartOffset() + tok.getLength();
		}
	       break;
	    case PACKAGE :
	       lasteol = -1;
	       preveol = -1;
	       inimport = true;
	       break;
	    case SPACE :
	       break;
	    default :
	       preveol = -1;
	       break;
	  }
	 tok = tokenizer.getNextToken();
       }
      return -1;
    }

}	// end of inner class SpellFixer



/********************************************************************************/
/*										*/
/*	Import checking 							*/
/*										*/
/********************************************************************************/

private static synchronized ImportChecker getImportCheckerForProject(String proj)
{
   if (proj == null) return null;

   ImportChecker ic = import_checkers.get(proj);
   if (ic == null) {
      ic = new ImportChecker(proj);
      import_checkers.put(proj,ic);
      ic.loadProjectClasses();
    }
   return ic;
}



private static class ImportChecker {

   private Set<String> project_classes;
   private String for_project;
   private Set<String> explicit_imports;
   private Set<String> demand_imports;
   private Set<String> implicit_imports;

   ImportChecker(String proj) {
      for_project = proj;
      project_classes = new HashSet<String>();
      explicit_imports = new HashSet<String>();
      demand_imports = new HashSet<String>();
      implicit_imports = new HashSet<String>();
    }

   void loadProjectClasses() {
      BumpClient bc = BumpClient.getBump();
      Element e = bc.getProjectData(for_project,false,false,true,false,true);
      if (e == null) return;
      Element clss = IvyXml.getChild(e,"CLASSES");
      for (Element cls : IvyXml.children(clss,"TYPE")) {
	 String nm = IvyXml.getTextElement(cls,"NAME");
	 project_classes.add(nm);
       }
      for (Element refs : IvyXml.children(e,"REFERENCES")) {
	 String rproj = IvyXml.getText(refs);
	 ImportChecker nic = getImportCheckerForProject(rproj);
	 for (String s : nic.project_classes) {
	    project_classes.add(s);
	  }
       }
      for (Element imps : IvyXml.children(e,"IMPORT")) {
	 if (IvyXml.getAttrBool(imps,"STATIC")) continue;
	 String imp = IvyXml.getText(imps);
	 if (IvyXml.getAttrBool(imps,"DEMAND")) {
	    int idx = imp.indexOf(".*");
	    if (idx > 0) imp = imp.substring(0,idx);
	    demand_imports.add(imp);
	  }
	 else {
	    explicit_imports.add(imp);
	    int idx = imp.lastIndexOf(".");
	    if (idx >= 0) {
	       implicit_imports.add(imp.substring(0,idx));
	     }
	  }
       }
    }

   String findImport(String nm) {
      String pat = "." + nm;
      String match = null;
      for (String s : project_classes) {
	 if (s.endsWith(pat) || s.equals(nm)) {
	    if (match != null) return null;
	    match = s.replace("$", ".");
	  }
       }
      if (match != null) return match;

      String dmatch = null;
      String amatch = null;
      String imatch = null;

      BumpClient bc = BumpClient.getBump();
      List<BumpLocation> typlocs = bc.findAllTypes(nm);
      if (typlocs == null) return null;
      for (BumpLocation bl : typlocs) {
	 String tnm = bl.getSymbolName();
	 int idx = tnm.indexOf("<");
	 if (idx > 0) tnm = tnm.substring(0,idx).trim();
	 if (explicit_imports.contains(tnm)) {
	    if (match == null) match = tnm;
	    else match = "*";
	  }
	 for (String s : demand_imports) {
	    String dimp = s + "." + nm;
	    if (dimp.equals(tnm)) {
	       if (dmatch == null) dmatch = tnm;
	       else dmatch = "*";
	     }
	  }
	 for (String s : implicit_imports) {
	    String dimp = s + "." + nm;
	    if (dimp.equals(tnm)) {
	       if (imatch == null) imatch = tnm;
	       else imatch = "*";
	    }
	  }
	 if (amatch == null) amatch = tnm;
	 else amatch = "*";
       }
      if (match != null) {
	 if (match.equals("*")) return null;
	 return match;
       }
      if (dmatch != null) {
	 if (dmatch.equals("*")) return null;
	 return dmatch;
       }
      if (imatch != null) {
	 if (imatch.equals("*")) return null;
	 return imatch;
       }
      if (amatch != null) {
	 if (amatch.equals("*")) return null;
	 return amatch;
       }
      return null;
    }

}




/********************************************************************************/
/*										*/
/*	Class to find a good fix for syntax errors				*/
/*										*/
/********************************************************************************/

private class SyntaxFixer implements Runnable {

   private BumpProblem for_problem;
   private long initial_time;

   SyntaxFixer(BumpProblem bp) {
      for_problem = bp;
      initial_time = start_time;
    }

   @Override public void run() {
      String msg = for_problem.getMessage();
      int soff = for_problem.getStart();
      int eoff = for_problem.getEnd()+1;
      String ins = null;
      if (msg.startsWith("Syntax error, insert ")) {
         int i0 = msg.indexOf("\"");
         int i1 = msg.lastIndexOf("\"");
         ins = msg.substring(i0+1,i1);
         soff = eoff;
       }
      else if (msg.startsWith("Syntax error on token") && msg.contains("delete this token")) {
         ins = null;
       }
      else if (msg.startsWith("Syntax error on token") && msg.contains("expected")) {
         int idx = msg.indexOf("\", ");
         idx += 3;
         int idx1 = msg.indexOf(" ",idx);
         ins = msg.substring(idx,idx1);
       }
      else if (msg.startsWith("Invalid character constants")) {
         ins = ";";
       }
      else return;
      
      if (ins != null && ins.length() == 2 && eoff-soff == 1) {
         int idx = msg.indexOf("\"");
         int idx1 = msg.indexOf("\"",idx+1);
         String tok = msg.substring(idx+1,idx1);
         if (ins.startsWith(tok)) ++eoff;
      }
   
      String proj = for_document.getProjectName();
      File file = for_document.getFile();
      String filename = file.getAbsolutePath();
      BumpClient bc = BumpClient.getBump();
      String pid = bc.createPrivateBuffer(proj,filename,null);
      if (pid == null) return;
      BoardLog.logD("BALE","SPELL: using private buffer " + pid);
      try {
         Collection<BumpProblem> probs = bc.getPrivateProblems(filename,pid);
         if (probs == null) {
            BoardLog.logE("BALE","SPELL: Problem getting errors for " + pid);
            return;
          }
         int probct = getErrorCount(probs);
         if (!checkProblemPresent(for_problem,probs)) return;
   
         bc.beginPrivateEdit(filename,pid);
         BoardLog.logD("BALE","SPELL: Try syntax edit " + soff + "," + eoff + "," + ins);
         int edelta = soff-eoff;
         if (ins != null) edelta += ins.length();
         bc.editPrivateFile(proj,file,pid,soff,eoff,ins);
         probs = bc.getPrivateProblems(filename,pid);
         bc.beginPrivateEdit(filename,pid);		// undo and wait
         if (probs == null || getErrorCount(probs) >= probct) return;
         if (checkAnyProblemPresent(for_problem,probs,0,edelta)) return;
       }
      finally {
         bc.removePrivateBuffer(proj,filename,pid);
       }
      if (start_time != initial_time) return;
      BoardLog.logD("BALE","SPELL: DO syntax edit");
      BoardMetrics.noteCommand("BALE","SYNTAXFIX");
      SyntaxDoer sd = new SyntaxDoer(for_problem,soff,eoff,ins,initial_time);
      SwingUtilities.invokeLater(sd);
    }

}	// end of inner class SyntaxFixer




/********************************************************************************/
/*										*/
/*	Classes to do a fix							*/
/*										*/
/********************************************************************************/

private class SpellDoer implements Runnable {

   private BumpProblem for_problem;
   private SpellFix for_fix;
   private long initial_time;

   SpellDoer(BumpProblem bp,SpellFix fix,long time0) {
      for_problem = bp;
      for_fix = fix;
      initial_time = time0;
    }

   @Override public void run() {
      BumpClient bc = BumpClient.getBump();
      List<BumpProblem> probs = bc.getProblems(for_document.getFile());
      if (!checkProblemPresent(for_problem,probs)) return;
      if (start_time != initial_time) return;

      int soff = for_document.mapOffsetToJava(for_problem.getStart());
      // int eoff0 = for_document.mapOffsetToJava(for_problem.getEnd());
      int len = for_fix.getOriginalText().length();
      int eoff = soff+len-1;
      String txt = for_fix.getText();
      try {
	 for_document.replace(soff,eoff-soff+1,txt,null);
       }
      catch (BadLocationException e) { }
    }

}	// end of inner class SpellDoer



private class ImportDoer implements Runnable {

   private BumpProblem for_problem;
   private String import_type;
   private long initial_time;
   // private int import_position;

   ImportDoer(BumpProblem bp,String type,int pos,long time0) {
      for_problem = bp;
      import_type = type;
      // import_position = pos;
      initial_time = time0;
    }

   @Override public void run() {
      BumpClient bc = BumpClient.getBump();
      List<BumpProblem> probs = bc.getProblems(for_document.getFile());
      if (!checkProblemPresent(for_problem,probs)) return;
      if (start_time != initial_time) return;
      synchronized (imports_added) {
         if (!imports_added.add(import_type)) return;
       }
      
      Element edits = bc.fixImports(for_problem.getProject(),
            for_document.getFile(),null,0,0,import_type);
      if (edits != null) {
         BaleFactory.getFactory().applyEdits(for_document.getFile(),edits);
       }
      // BaleDocument base = for_document.getBaseEditDocument();
      // String imp = "import " + import_type + ";\n";
      // try {
         // base.insertString(import_position,imp,null);
       // }
      // catch (BadLocationException e) { }
    }

}



private class SyntaxDoer implements Runnable {

   private BumpProblem for_problem;
   private int edit_start;
   private int edit_end;
   private String insert_text;
   private long initial_time;

   SyntaxDoer(BumpProblem bp,int soff,int eoff,String txt,long t0) {
      for_problem = bp;
      edit_start = soff;
      edit_end = eoff;
      insert_text = txt;
      initial_time = t0;
    }

   @Override public void run() {
      BumpClient bc = BumpClient.getBump();
      List<BumpProblem> probs = bc.getProblems(for_document.getFile());
      if (!checkProblemPresent(for_problem,probs)) return;
      if (start_time != initial_time) return;
      int soff = for_document.mapOffsetToJava(edit_start);
      int eoff = for_document.mapOffsetToJava(edit_end);
      try {
	 for_document.replace(soff,eoff-soff,insert_text,null);
       }
      catch (BadLocationException e) { }
    }

}	// end of inner class SyntaxDoer




/********************************************************************************/
/*										*/
/*	Hold a potential quick fix						*/
/*										*/
/********************************************************************************/

private static class SpellFix implements Comparable<SpellFix> {

   private String old_text;
   private String new_text;
   private int text_delta;

   SpellFix(String orig,String txt,int d) {
      old_text = orig;
      new_text = txt;
      text_delta = d;
    }

   String getOriginalText()		{ return old_text; }
   String getText()			{ return new_text; }
   int getEditCount()			{ return text_delta; }

   @Override public int compareTo(SpellFix sf) {
      int d = getEditCount() - sf.getEditCount();
      if (d < 0) return -1;
      if (d > 0) return 1;
      return new_text.compareTo(sf.new_text);
    }
}




/********************************************************************************/
/*										*/
/*	Handle editor events							*/
/*										*/
/********************************************************************************/

private class DocHandler implements DocumentListener, CaretListener {

   @Override public void changedUpdate(DocumentEvent e) {
       int len = e.getLength();
       int dlen = e.getDocument().getLength();
       if (len != dlen) {
	  BoardLog.logD("BALE","SPELL: Clear for changed update");
	  clearRegion();
	}
    }

   @Override public void insertUpdate(DocumentEvent e) {
      int off = e.getOffset();
      int len = e.getLength();
      if (len == 0) return;
      else if (len == 1) {
	 handleTyped(off,1);
       }
      else {
	 try {
	    String s = for_document.getText(off,len).trim();
	    if (s.equals("") || s.equals("}")) {
	       SwingUtilities.invokeLater(new Checker());
	       handleTyped(off,len);
	       return;
	     }
	  }
	 catch (BadLocationException ex) { }
	 BoardLog.logD("BALE","SPELL: Clear for insert update");
	 clearRegion();
       }
    }

   @Override public void removeUpdate(DocumentEvent e) {
      int off = e.getOffset();
      int len = e.getLength();
      if (len == 1) {
	 handleBackspace(off);
       }
      else {
	 BoardLog.logD("BALE","SPELL: Clear for remove update");
	 clearRegion();
       }
    }

   @Override public void caretUpdate(CaretEvent e) {
      int off = e.getDot();
      if (off == caret_position) return;
      if (off >= start_offset && off <= end_offset) {
	 caret_position = off;
	 return;
       }
      BoardLog.logD("BALE","SPELL: Clear for caret update");
      clearRegion();
    }

}	// end of inner class DocHandler




/********************************************************************************/
/*										*/
/*	Handle Compilation Events						*/
/*										*/
/********************************************************************************/

private class ProblemHandler implements BumpConstants.BumpProblemHandler {

   @Override public void handleProblemAdded(BumpProblem bp) {
      addProblem(bp);
    }

   @Override public void handleProblemRemoved(BumpProblem bp) {
      removeProblem(bp);
    }

   @Override public void handleClearProblems() {
      active_problems.clear();
    }

   @Override public void handleProblemsDone() {
      SwingUtilities.invokeLater(new Checker());
    }

}	// end of inner class ProblemHandler



private class ProblemComparator implements Comparator<BumpProblem> {

   @Override public int compare(BumpProblem p1,BumpProblem p2) {
      int d = p1.getStart() - p2.getStart();
      if (d < 0) return -1;
      if (d > 0) return 1;
      return p1.getProblemId().compareTo(p2.getProblemId());
    }
}


private class Checker implements Runnable {

   @Override public void run() {
      checkForElementToFix();
   }

}	// end of inner class Checker




/********************************************************************************/
/*										*/
/*	Handle popup menu for spelling correction				*/
/*										*/
/********************************************************************************/

private void addPopupMenuItems(BaleContextConfig ctx,JPopupMenu menu)
{
   clearRegion();

   BaleDocument bd = (BaleDocument) ctx.getDocument();
   List<BumpProblem> probs = bd.getProblemsAtLocation(ctx.getOffset());
   if (probs == null) return;
   for (BumpProblem bp : probs) {
      List<String> txts = trySpellingProblem(bp);
      if (txts != null && txts.size() > 0) {
	 menu.add(new SpellTryer(bp,txts));
	 break;
       }
    }
}


private class SpellTryer extends AbstractAction {

   private BumpProblem for_problem;
   private List<String> for_texts;

   private static final long serialVersionUID = 1;

   SpellTryer(BumpProblem bp,List<String> txts) {
      super("Auto Fix '" + txts.get(0) + "'");
      for_problem = bp;
      for_texts = txts;
    }

   @Override public void actionPerformed(ActionEvent e) {
      int minsize = BALE_PROPERTIES.getInt("Bale.correct.spelling.user",5);
      for (String txt : for_texts) {
         SpellFixer sf = new SpellFixer(for_problem,txt,minsize);
         sf.run();
       }
    }

}	// end of inner class SpellTryer




private static class Contexter implements BaleContextListener {

   @Override public BudaBubble getHoverBubble(BaleContextConfig cfg) {
      return null;
    }

   @Override public String getToolTipHtml(BaleContextConfig cfg) {
      return null;
    }


   @Override public void addPopupMenuItems(BaleContextConfig ctx,JPopupMenu menu) {
      if (ctx.inAnnotationArea()) return;
   
      for (BaleCorrector bc : all_correctors.keySet()) {
         BudaBubble bbl = BudaRoot.findBudaBubble(bc.for_editor);
         if (bbl == ctx.getEditor()) {
            bc.addPopupMenuItems(ctx,menu);
            break;
          }
       }
    }

}



}	// end of class BaleCorrector




/* end of BaleCorrector.java */
