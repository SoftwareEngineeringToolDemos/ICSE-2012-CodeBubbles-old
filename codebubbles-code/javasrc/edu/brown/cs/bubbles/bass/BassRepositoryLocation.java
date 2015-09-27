/********************************************************************************/
/*										*/
/*		BassRepositoryLocation.java					*/
/*										*/
/*	Bubble Augmented Search Strategies store for all possible names 	*/
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


package edu.brown.cs.bubbles.bass;

import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.bump.*;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class BassRepositoryLocation implements BassConstants.BassUpdatingRepository,
		BassConstants, BumpConstants.BumpChangeHandler
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Set<BassName>	all_names;
private boolean 	is_ready;
private List<BassUpdatableRepository> update_repos;

private Pattern 	anonclass_pattern = Pattern.compile("\\$[0-9]");




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BassRepositoryLocation()
{
   all_names = new HashSet<BassName>();
   is_ready = false;
   update_repos = new ArrayList<BassUpdatableRepository>();

   initialize();

   BumpClient.getBump().addChangeHandler(this);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public Iterable<BassName> getAllNames()
{
   synchronized (this) {
      while (!is_ready) {
	 try {
	    wait(2000);
	  }
	 catch (InterruptedException e) { }
       }
      return new ArrayList<BassName>(all_names);
    }
}



@Override public boolean includesRepository(BassRepository br)	{ return br == this; }


@Override public void addUpdateRepository(BassUpdatableRepository br)
{
   update_repos.add(br);
}

@Override public void removeUpdateRepository(BassUpdatableRepository br)
{
   update_repos.remove(br);
}


void waitForNames()
{
   synchronized (this) {
      while (!is_ready) {
	 try {
	    wait(2000);
	  }
	 catch (InterruptedException e) { }
       }
    }
}




BassName findBubbleName(File f,int eclipsepos)
{
   BassNameLocation best = null;
   int bestlen = 0;
   boolean inclbest = false;
   int maxdelta0 = 0;
   int maxdelta1 = 2;

   waitForNames();

   synchronized (this) {
      for (BassName bn : all_names) {
	 BassNameLocation bnl = (BassNameLocation) bn;
	 if (bnl.getFile().equals(f)) {
	    int spos = bnl.getEclipseStartOffset();
	    int epos = bnl.getEclipseEndOffset();
	    boolean incl = (spos <= eclipsepos && epos > eclipsepos);
	    if (best != null && incl && !inclbest && best.getNameType() == bnl.getNameType()) best = null;
	    if (best == null || epos - spos <= bestlen) {
	       if (best != null && epos - spos == bestlen) {
		  if (best.getNameType() == BassNameType.HEADER && bnl.getNameType() == BassNameType.CLASS) ;
		  else continue;
		}
	       if (spos-maxdelta0 <= eclipsepos && epos+maxdelta1 > eclipsepos) {	// allow for indentations
		  best = bnl;
		  bestlen = epos - spos;
		  inclbest = incl;
		}
	     }
	  }
       }
    }

   // TODO: handle fields, prefix ??

   return best;
}



List<BumpLocation> findClassMethods(String cls)
{
   List<BumpLocation> rslt = new ArrayList<BumpLocation>();
   boolean fndcls = false;

   waitForNames();

   int clsln = cls.length();

   synchronized (this) {
      for (BassName bn : all_names) {
	 BassNameLocation bnl = (BassNameLocation) bn;
	 String fnm = bnl.getFullName();
	 if (fnm == null) continue;
	 if (fnm.equals(cls) || (fnm.startsWith(cls) && fnm.charAt(clsln) == '.')) {
	    switch (bnl.getNameType()) {
	       case CLASS :
	       case ENUM :
	       case THROWABLE : 	
		  fndcls = true;
		  break;
	       case INTERFACE :
		  fndcls = true;
		  break;
	       case METHOD :	
		  String cnm = bnl.getNameHead();
		  if (cls.equals(cnm)) rslt.add(bnl.getLocation());
		  break;
	       default :
		  break;
	    }

	  }
       }
    }

   if (!fndcls) return null;

   return rslt;
}



File findActualFile(File f)
{
   waitForNames();

   synchronized (this) {
      for (BassName bn : all_names) {
	 BassNameLocation bnl = (BassNameLocation) bn;
	 if (bnl.getFile().equals(f)) return f;
	 if (bnl.getFile().getName().equals(f.getName())) return bnl.getFile();
       }
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private void initialize()
{	
   synchronized (this) {
      all_names.clear();
      is_ready = false;
    }

   Searcher s = new Searcher();
   BoardThreadPool.start(s);
}



private synchronized void loadNames()
{
   Map<String,BassNameLocation> usedmap = new HashMap<String,BassNameLocation>();

   BumpClient bc = BumpClient.getBump();
   Collection<BumpLocation> locs = bc.findAllNames(null);
   if (locs != null) {
      for (BumpLocation bl : locs) {
	 addLocation(bl,usedmap);
       }
    }

   is_ready = true;
   notifyAll();
}



private void addLocation(BumpLocation bl,Map<String,BassNameLocation> usedmap)
{
   if (!isRelevant(bl)) return;

   BassNameLocation bn = new BassNameLocation(bl);
   String key = null;

   switch (bn.getNameType()) {
      case FIELDS :
	 key = "FIELD@@@" + bn.getNameHead();
	 BassNameLocation fbn = usedmap.get(key);
	 if (fbn != null) {
	    fbn.addLocation(bl);
	    bn = null;
	  }
	 else usedmap.put(key,bn);
	 break;
      case MAIN_PROGRAM :
	 key = "MAIN@@@" + bn.getNameHead();
	 BassNameLocation mbn = usedmap.get(key);
	 if (mbn != null) {
	    mbn.addLocation(bl);
	    bn = null;
	 }
	 else usedmap.put(key,bn);
	 break;
      case STATICS :
	 key = "STATIC@@@" + bn.getNameHead();
	 BassNameLocation sbn = usedmap.get(key);
	 if (sbn != null) {
	    sbn.addLocation(bl);
	    bn = null;
	  }
	 else usedmap.put(key,bn);
	 break;
      case CLASS :
      case ENUM :
      case INTERFACE :
      case THROWABLE :
	 all_names.add(bn);
	 if (showClassFile(bn)) {
	    BassNameLocation fnm = new BassNameLocation(bl,BassNameType.FILE);
	    all_names.add(fnm);
	  }
	 bn = new BassNameLocation(bl,BassNameType.HEADER);
	 break;
      case PROJECT :
	 // if (all_names.size() != 0) bn = null;
	 break;
      case MODULE :
	 BassNameLocation fnm = new BassNameLocation(bl,BassNameType.MODULE);
	 all_names.add(fnm);
	 BassNameLocation inm = new BassNameLocation(bl,BassNameType.HEADER);
	 all_names.add(inm);
	 break;
      default:
	 break;
    }

   if (bn != null) all_names.add(bn);
}



private boolean showClassFile(BassNameLocation bn)
{
   if (bn.getKey().contains("$")) return false;
   switch (BoardSetup.getSetup().getLanguage()) {
      case PYTHON :
	 return false;
      default:
	 break;
    }

   return true;
}




private boolean isRelevant(BumpLocation bl)
{
   switch (bl.getSymbolType()) {
      case PACKAGE :
      case LOCAL :
      case UNKNOWN :
	 return false;
      default:
	 break;
    }

   if (bl.getKey() == null) return false;

   Matcher m = anonclass_pattern.matcher(bl.getKey());
   if (m.find()) return false;

   return true;
}



private class Searcher implements Runnable {

   @Override public void run() {
      loadNames();
    }

   @Override public String toString()		{ return "BASS_LocationSearcher"; }

}	// end of inner class Searcher




/********************************************************************************/
/*										*/
/*	Change detection methods						*/
/*										*/
/********************************************************************************/

@Override public void handleFileChanged(String proj,String file)
{
   addNamesForFile(proj,file,true);

   handleUpdated();
}



@Override public void handleFileAdded(String proj,String file)
{
   addNamesForFile(proj,file,false);

   handleUpdated();
}



@Override public void handleFileRemoved(String proj,String file)
{
   removeNamesForFile(proj,file);

   handleUpdated();
}


@Override public void handleProjectOpened(String proj)
{
   addNamesForFile(proj,null,true);

   handleUpdated();
}

@Override public void handleFileStarted(String proj,String file)		{ }


private void removeNamesForFile(String proj,String file)
{
   synchronized (this) {
      for (Iterator<BassName> it = all_names.iterator(); it.hasNext(); ) {
	 BassName bn = it.next();
	 BumpLocation bl = bn.getLocation();
	 if (bl != null && fileMatch(file,bl.getFile()) &&
	       (proj == null || proj.equals(bl.getProject())))
	    it.remove();
       }
    }
}

private boolean fileMatch(String file,File blf)
{
   if (file == null) return true;
   if (file.equals(blf.getPath())) return true;
   if (blf.getPath().endsWith(file)) return true;
   if (blf.exists()) return false;
   int idx = file.indexOf("/",1);
   if (idx > 0) {
      String f1 = file.substring(idx);
      if (blf.getPath().endsWith(f1)) return true;
   }
   int idx1 = file.indexOf("/",idx+1);
   if (idx1 > 0) {
      String f2 = file.substring(idx1);
      if (blf.getPath().endsWith(f2)) return true;
   }

   return false;
}



private void addNamesForFile(String proj,String file,boolean rem)
{
   Map<String,BassNameLocation> usedmap = new HashMap<String,BassNameLocation>();
   List<String> fls = null;
   if (file != null) {
      fls = new ArrayList<String>();
      fls.add(file);
    }

   Collection<BumpLocation> locs = BumpClient.getBump().findAllNames(proj,fls,true);

   synchronized (this) {
      if (rem) removeNamesForFile(proj,file);
      if (locs != null) {
	 for (BumpLocation bl : locs) {
	    addLocation(bl,usedmap);
	  }
       }
    }
   // BoardLog.logD("BASS","AFTER " + proj + " " + file + " " + all_names.size());
}


private void handleUpdated()
{
   for (BassUpdatableRepository br : update_repos) {
      br.reloadRepository();
    }

   BassFactory.reloadRepository(this);
}


}	// end of class BassRepositoryLocation




/* end of BassRepositoryLocation.java */
