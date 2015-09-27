/********************************************************************************/
/*										*/
/*		BvcrVersionGIT.java						*/
/*										*/
/*	Bubble Version Collaboration Repository interface to GIT		*/
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


package edu.brown.cs.bubbles.bvcr;

import edu.brown.cs.bubbles.board.BoardProperties;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;



class BvcrVersionGIT extends BvcrVersionManager implements BvcrConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private File		git_root;
private String		git_command;
private String		current_version;

private static BoardProperties bvcr_properties = BoardProperties.getProperties("Bvcr");

private static SimpleDateFormat GIT_DATE = new SimpleDateFormat("EEE MMM dd kk:mm:ss yyyy ZZZZZ");

private static String GIT_LOG_FORMAT = "%H%x09%h%x09%an%x09%ae%x09%ad%x09%P%x09%d%x09%s%n%b%n***EOF";
private static String GIT_PRIOR_FORMAT = "%H%x09%P%x09%d%n";



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BvcrVersionGIT(BvcrProject bp)
{
   super(bp);

   git_command = bvcr_properties.getProperty("bvcr.git.command","git");
   current_version = bvcr_properties.getProperty("bvcr.git." + bp.getName() + ".origin");

   findGitRoot();

   if (current_version == null) {
      current_version = "HEAD";
      String cmd = git_command + " branch --all";
      StringCommand sc = new StringCommand(cmd);
      String vers = sc.getContent();
      StringTokenizer tok = new StringTokenizer(vers," \r\n\t");
      boolean star = false;
      while (tok.hasMoreTokens()) {
	 String v = tok.nextToken();
	 if (v.equals("*")) star = true;
	 else {
	    if (star) {
	       System.err.println("BVCR: FOUND BRANCH " + v);
	       current_version = v;
	     }
	    star = false;
	  }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/

static BvcrVersionManager getRepository(BvcrProject bp,File srcdir)
{
   if (srcdir == null) return null;

   File fp = srcdir;
   while (fp != null && fp.exists() && fp.isDirectory()) {
      File f2 = new File(fp,".git");
      if (f2.exists() && f2.isDirectory()) {
	 System.err.println("BVCR: HANDLE GIT REPOSITORY " + srcdir);
	 return new BvcrVersionGIT(bp);
       }
      fp = fp.getParentFile();
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

File getRootDirectory() 		{ return git_root; }

String getRepositoryName()
{
   return super.getRepositoryName();
}


void getDifferences(BvcrDifferenceSet ds)
{
   String cmd = git_command  + " diff -b -r";

   String v0 = ds.getStartVersion();
   String v1 = ds.getEndVersion();
   if (v0 != null) {
      cmd += " " + v0;
      if (v1 != null) cmd += " " + v1;
    }

   List<File> diffs = ds.getFilesToCompute();
   if (diffs == null) {
      // cmd += " " + git_root.getPath();
    }
   else if (diffs.size() == 0) return;
   else {
      cmd += " --";
      for (File f : diffs) {
	 try {
	    f = f.getCanonicalFile();
	  }
	 catch (IOException e) { }
	 cmd += " " + f.getAbsolutePath();
       }
    }

   DiffAnalyzer da = new DiffAnalyzer(ds);
   runCommand(cmd,da);
}




/********************************************************************************/
/*										*/
/*	Find the top of the GIT repository					*/
/*										*/
/********************************************************************************/

private void findGitRoot()
{
   File f = new File(for_project.getSourceDirectory());
   for ( ; ; ) {
      File fp = f.getParentFile();
      File fp1 = new File(fp,".git");
      if (!fp1.exists()) break;
      f = fp;
    }

   System.err.println("BVCR: GIT root = " + f);

   git_root = f;
}



/********************************************************************************/
/*										*/
/*	History gathering for a file						*/
/*										*/
/********************************************************************************/

void findHistory(File f,IvyXmlWriter xw)
{
   String head = null;
   Map<String,List<String>> priors = new HashMap<String,List<String>>();
   String cmd1 = git_command + " log --reverse '--pretty=format:" + GIT_PRIOR_FORMAT + "'";
   StringCommand cmd = new StringCommand(cmd1);
   StringTokenizer tok = new StringTokenizer(cmd.getContent(),"\n\r");
   while (tok.hasMoreTokens()) {
      String ent = tok.nextToken();
      try {
	 String [] ldata = ent.split("\t",3);
	 String rev = ldata[0];
	 String prev = ldata[1];
	 String alts = ldata[2];
	 List<String> prevs = priors.get(rev);
	 if (prevs == null) {
	    prevs = new ArrayList<String>();
	    priors.put(rev,prevs);
	  }
	 for (StringTokenizer ltok = new StringTokenizer(prev,"(, )"); ltok.hasMoreTokens(); ) {
	    String pid = ltok.nextToken();
	    prevs.add(pid);
	  }
	 if (head == null && alts != null && alts.length() > 0) {
	    for (StringTokenizer ltok = new StringTokenizer(alts,"(, )"); ltok.hasMoreTokens(); ) {
	       String nm = ltok.nextToken();
	       if (nm.equals("HEAD")) head = rev;
	     }
	  }
       }
      catch (Throwable t) {
	 System.err.println("BVCR: Problem parsing priors log entry: " + t);
       }
    }

   String cmds = git_command + " log --reverse '--pretty=format:" + GIT_LOG_FORMAT + "'";
   try {
      f = f.getCanonicalFile();
    }
   catch (IOException e) { }
   cmds += " -- " + f.getAbsolutePath();
   // parent version information is inaccurate in this case

   cmd = new StringCommand(cmds);

   Map<String,BvcrFileVersion> fvs = new LinkedHashMap<String,BvcrFileVersion>();

   tok = new StringTokenizer(cmd.getContent(),"\n\r");
   while (tok.hasMoreTokens()) {
      String ent = tok.nextToken();
      try {
	 String [] ldata = ent.split("\t",8);
	 String rev = ldata[0];
	 String srev = ldata[1];
	 String auth = ldata[2];
	 String email = ldata[3];
	 Date d = GIT_DATE.parse(ldata[4]);
	 String prev = ldata[5];
	 String alts = ldata[6];
	 String msg = ldata[7];
	 String bdy = "";

	 if (email != null) {
	    if (auth != null) auth += " (" + email + ")";
	    else auth = email;
	  }

	 BvcrFileVersion fv = new BvcrFileVersion(f,rev,d,auth,msg);
	 Set<String> done = new HashSet<String>();
	 for (StringTokenizer ltok = new StringTokenizer(prev,"(, )"); ltok.hasMoreTokens(); ) {
	    String pid = ltok.nextToken();
	    addPriors(fv,pid,fvs,priors,done);
	  }
	 if (srev != null && srev.length() > 0) fv.addAlternativeId(srev,null);
	 if (alts != null && alts.length() > 0) {
	    for (StringTokenizer ltok = new StringTokenizer(alts,"(, )"); ltok.hasMoreTokens(); ) {
	       String nm = ltok.nextToken();
	       fv.addAlternativeName(nm);
	     }
	  }
	 fvs.put(rev,fv);
	 while (tok.hasMoreTokens()) {
	    String bdl = tok.nextToken();
	    if (bdl.equals("***EOF")) break;
	    bdy += bdl + "\n";
	   }
	 bdy = bdy.trim();
	 if (bdy.length() > 2) fv.addVersionBody(bdy);
       }
      catch (Throwable e) {
	 System.err.println("BVCR: Problem parsing log entry: " + e);
       }
    }

   if (head != null && !fvs.containsKey(head)) {
      BvcrFileVersion fv = new BvcrFileVersion(f,head,null,null,null);
      Set<String> done = new HashSet<String>();
      addPriors(fv,head,fvs,priors,done);
      for (BvcrFileVersion pv : fv.getPriorVersions(null)) {
	 pv.addAlternativeId("HEAD",null);
	 break;
       }
    }

   xw.begin("HISTORY");
   xw.field("FILE",f.getPath());
   for (BvcrFileVersion fv : fvs.values()) {
      fv.outputXml(xw);
    }
   xw.end("HISTORY");
}



private void addPriors(BvcrFileVersion fv,String id,Map<String,BvcrFileVersion> known,
			  Map<String,List<String>> priors,
			  Set<String> done)
{
   if (id == null) return;
   if (done.contains(id)) return;
   done.add(id);

   BvcrFileVersion pv = known.get(id);
   if (pv != null) {
      fv.addPriorVersion(pv);
      return;
    }

   List<String> prs = priors.get(id);
   if (prs == null) {
      System.err.println("BVCR: Can't find prior version " + id);
      return;
    }

   for (String s : prs) {
      if (s == null || s.equals(id)) continue;
      addPriors(fv,s,known,priors,done);
    }
}




}	// end of class BvcrVersionGIT



/* end of BvcrVersionGIT.java */
