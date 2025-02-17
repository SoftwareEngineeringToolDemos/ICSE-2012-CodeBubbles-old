/********************************************************************************/
/*										*/
/*		BassNamePattern.java						*/
/*										*/
/*	Bubble Augmented Search Strategies pattern representation		*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bass;

import java.lang.reflect.Modifier;
import java.util.*;


class BassNamePattern implements BassConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private List<String> project_match;
private List<String> package_match;
private List<String> class_match;
private List<String> method_match;
private List<String> argument_match;
private List<String> any_match;
private boolean is_type;
private boolean is_interface;
private boolean is_method;
private boolean is_field;
private boolean public_only;
private boolean protected_only; 	// includes public
private boolean package_only;		// includes public/protected
private boolean static_only;
private boolean case_sensitive;
private boolean is_empty;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BassNamePattern(String txt,boolean usecase)
{
   project_match = null;
   package_match = null;
   class_match = null;
   any_match = new ArrayList<String>();
   is_type = false;
   is_interface = false;
   is_method = false;
   is_field = false;
   case_sensitive = usecase;
   public_only = false;
   protected_only = false;
   package_only = false;
   static_only = false;
   is_empty = true;

   buildPattern(txt);
}





/********************************************************************************/
/*										*/
/*	Pattern building aids							*/
/*										*/
/********************************************************************************/

private void buildPattern(String txt)
{
   if (txt == null) return;

   StringTokenizer tok = new StringTokenizer(txt);
   while (tok.hasMoreTokens()) {
      String elt = tok.nextToken();
      is_empty = false;
      if (elt.length() >= 2 && elt.charAt(1) == ':') {
	 switch (Character.toUpperCase(elt.charAt(0))) {
	    case 'E' :
	       if (elt.length() > 2) {
		  if (project_match == null) project_match = new ArrayList<String>();
		  String xtxt = elt.substring(2);
		  if (!case_sensitive) xtxt = xtxt.toLowerCase();
		  project_match.add(xtxt);
		}
	       break;
	    case 'P' :
	       if (elt.length() > 2) {
		  if (package_match == null) package_match = new ArrayList<String>();
		  String xtxt = elt.substring(2);
		  if (!case_sensitive) xtxt = xtxt.toLowerCase();
		  package_match.add(xtxt);
		}
	       break;
	    case 'C' :
	       // is_type = true;
	       if (elt.length() > 2) {
		  if (class_match == null) class_match = new ArrayList<String>();
		  String xtxt = elt.substring(2);
		  if (!case_sensitive) xtxt = xtxt.toLowerCase();
		  class_match.add(xtxt);
		}
	       break;
	    case 'I' :
	       // is_interface = true;
	       if (elt.length() > 2) {
		  if (class_match == null) class_match = new ArrayList<String>();
		  String xtxt = elt.substring(2);
		  if (!case_sensitive) xtxt = xtxt.toLowerCase();
		  class_match.add(xtxt);
		}
	       break;
	    case 'M' :
	       is_method = true;
	       if (method_match == null) method_match = new ArrayList<String>();
	       if(elt.length() > 2) {
		  String xtxt = elt.substring(2);
		  if (!case_sensitive) xtxt = xtxt.toLowerCase();
		  method_match.add(xtxt);
	       }
	       break;
	    case 'A' :
	       is_method = true;
	       if (argument_match == null) argument_match = new ArrayList<String>();
	       if (elt.length() > 2) {
		  String xtxt = elt.substring(2);
		  if (!case_sensitive) xtxt = xtxt.toLowerCase();
		  argument_match.add(xtxt);
	       }
	       break;
	    case 'F' :
	       is_field = true;
	       addMatch(elt.substring(2));
	       break;
	    default :
	       addMatch(elt.substring(2));
	       break;
	  }
       }
      else if (elt.charAt(0) == '!') {
	 public_only = true;
	 protected_only = false;
	 package_only = false;
	 addMatch(elt.substring(1));
       }
      else if (elt.charAt(0) == '#') {
	 public_only = false;
	 protected_only = true;
	 package_only = false;
	 addMatch(elt.substring(1));
       }
      else if (elt.charAt(0) == '^') {
	 public_only = false;
	 protected_only = false;
	 package_only = true;
	 addMatch(elt.substring(1));
       }
      else if (elt.charAt(0) == '&') {
	 static_only = true;
	 addMatch(elt.substring(1));
       }
      else addMatch(elt);
    }
}




private void addMatch(String txt) {
   if (txt != null && txt.length() > 0) {
      if (case_sensitive) any_match.add(txt);
      else any_match.add(txt.toLowerCase());
    }
}



/********************************************************************************/
/*										*/
/*	Matching methods							*/
/*										*/
/********************************************************************************/

int match(BassName bn)
{
   if (is_type || is_interface || is_method || is_field) {
      switch (bn.getNameType()) {
	 case PACKAGE :
	 case NONE :
         case MODULE :
	    return -1;
	 case ENUM :
	 case CLASS :
	 case THROWABLE :
	    if (is_method || is_interface || is_field) return -1;
	    break;
	 case INTERFACE :
	    if (is_method || is_field) return -1;
	    break;
	 case FIELDS :
	    if (is_method || is_interface || is_type) return -1;
	    break;
	 case METHOD :
	 case CONSTRUCTOR :
	 case STATICS :
	    if (is_type || is_interface || is_field) return -1;
	    break;
	 default:
	    break;
       }
    }

   int mods = bn.getModifiers();
   if (mods > 0) {
      if (public_only && !Modifier.isPublic(mods)) return -1;
      else if (protected_only && (!Modifier.isPublic(mods) && !Modifier.isProtected(mods))) return -1;
      else if (package_only && Modifier.isPrivate(mods)) return -1;
      if (static_only && !Modifier.isStatic(mods)) return -1;
    }

   int val = 0;

   if (project_match != null) {
      if (bn.getProject() != null) {
	 int v = checkString(bn.getProject(),project_match);
	 if (v < 0) return -1;
	 val += v;
       }
    }

   if (package_match != null) {
      int v = checkString(bn.getPackageName(),package_match);
      if (v < 0) return -1;
      val += v;
    }

   if (class_match != null) {
      int v = checkString(bn.getClassName(),class_match);
      if (v < 0) return -1;
      val += v;
    }

   if (method_match != null) {
      String fn = bn.getFullName();
      if (fn.lastIndexOf("(") > 0) fn = fn.substring(0, fn.lastIndexOf("("));
      if (fn.lastIndexOf(".") > 0 && fn.lastIndexOf(".") < fn.length()) fn = fn.substring(fn.lastIndexOf(".")+1);
      int v = checkString(fn, method_match);
      if (v < 0) return -1;
      val += v;
   }

   if (argument_match != null) {
      String fn = bn.getFullName();
      if (fn.lastIndexOf("(") > 0) fn = fn.substring(fn.lastIndexOf("("));
      int v = checkString(fn, argument_match);
      if (v < 0) return -1;
      val += v;
   }

   if (any_match.size() > 0) {
      String fn = bn.getFullName();
      if (fn.lastIndexOf("(") > 0) fn = fn.substring(0, fn.lastIndexOf("("));
      int v = checkString(fn,any_match);
      if (v < 0) return -1;
      val += v;
    }

   return val;
}




boolean isEmpty()				{ return is_empty; }




/********************************************************************************/
/*										*/
/*	Component string matcher						*/
/*										*/
/********************************************************************************/

private int checkString(String txt,List<String> mtch)
{
   if (mtch == null || mtch.size() == 0) return 0;
   if (txt == null || txt.length() == 0) return -1;

   // String mtxt = txt;
   // if (!case_sensitive) mtxt = txt.toLowerCase();
   int mtln = txt.length();

   for (String s : mtch) {
      //  if (!mtxt.contains(s)) return -1;
      int sln = s.length();
      boolean fndfg = false;
      for (int i = 0; !fndfg && i <= mtln - sln; ++i) {
	 boolean mfg = true;
	 for (int j = 0; mfg && j < sln; ++j) {
	    char c = txt.charAt(i+j);
	    char c1 = s.charAt(j);
	    if (c == c1) continue;
	    if (!case_sensitive && Character.toLowerCase(c) == c1) continue;
	    mfg = false;
	  }
	 fndfg = mfg;
       }
      if (!fndfg) return -1;
    }

   // TODO: compute a match score based on location, camel case, etc.

   return 0;
}



}	// end of class BassNamePattern




/* end of BassNamePattern.java */
