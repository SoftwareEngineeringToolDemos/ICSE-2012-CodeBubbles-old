/********************************************************************************/
/*										*/
/*		BattProject.java						*/
/*										*/
/*	Bubble Automated Testing Tool project information			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


package edu.brown.cs.bubbles.batt;


import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.util.*;




class BattProject implements BattConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String			project_name;
private Map<String,List<ProjClass>> file_classes;

private Map<String,ProjClass>	class_data;
private Set<String>		class_paths;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BattProject(Element xml)
{
   project_name = IvyXml.getAttrString(xml,"NAME");
   class_data = new HashMap<String,ProjClass>();
   class_paths = new LinkedHashSet<String>();
   file_classes = new HashMap<String,List<ProjClass>>();

   Element clss = IvyXml.getChild(xml,"CLASSES");
   for (Element ce : IvyXml.children(clss,"TYPE")) {
      ProjClass pc = new ProjClass(ce);
      class_data.put(pc.getName(),pc);
      String fnm = pc.getSource();
      if (fnm != null) {
	 List<ProjClass> clsl = file_classes.get(fnm);
	 if (clsl == null) {
	    clsl = new ArrayList<ProjClass>();
	    file_classes.put(fnm,clsl);
	  }
	 // System.err.println("BATT: Add class " + pc.getName() + " TO " + fnm);
	 clsl.add(pc);
       }
    }

   String ignore = null;
   Element pths = IvyXml.getChild(xml,"CLASSPATH");
   for (Element pe : IvyXml.children(pths,"PATH")) {
      String bn = null;
      String ptyp = IvyXml.getAttrString(pe,"TYPE");
      if (ptyp != null && ptyp.equals("SOURCE"))
	 bn = IvyXml.getTextElement(pe,"OUTPUT");
      else
	 bn = IvyXml.getTextElement(pe,"BINARY");
      if (bn == null) continue;
      if (bn.endsWith("/lib/rt.jar")) {
	 int idx = bn.lastIndexOf("rt.jar");
	 ignore = bn.substring(0,idx);
       }
      class_paths.add(bn);
    }
   if (ignore != null) {
      for (Iterator<String> it = class_paths.iterator(); it.hasNext(); ) {
	 String nm = it.next();
	 if (nm.startsWith(ignore)) it.remove();
       }
    }
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getName()			{ return project_name; }

List<String> getClassPath()
{
   return new ArrayList<String>(class_paths);
}

List<String> getClassNames()
{
   List<String> rslt = new ArrayList<String>();
   for (ProjClass pc : class_data.values()) {
      if (pc.getSource() != null && pc.getBinary() != null) {
	 rslt.add(pc.getName());
       }
    }
   return rslt;
}


Set<String> getClassesForFile(String fnm,Set<String> rslt)
{
   List<ProjClass> pcf = file_classes.get(fnm);

   if (rslt == null) rslt = new HashSet<String>();

   if (pcf == null) return rslt;
   for (ProjClass pc : pcf) {
      rslt.add(pc.getName());
    }
   return rslt;
}




/********************************************************************************/
/*										*/
/*	Representation of class data						*/
/*										*/
/********************************************************************************/

private static class ProjClass {

   private String class_name;
   private String class_file;
   private String source_file;

   ProjClass(Element e) {
      class_name = IvyXml.getAttrString(e,"NAME");
      source_file = IvyXml.getAttrString(e,"SOURCE");
      class_file = IvyXml.getAttrString(e,"BINARY");
    }

   String getName()			{ return class_name; }
   String getSource()			{ return source_file; }
   String getBinary()			{ return class_file; }

}	// end of inner class ProjClass




}	// end of class BattProject




/* end of BattProject.java */
