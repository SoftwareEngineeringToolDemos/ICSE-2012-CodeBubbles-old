/********************************************************************************/
/*                                                                              */
/*              NobasePathSpec.java                                             */
/*                                                                              */
/*      Information about a project path                                        */
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

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.File;

class NobasePathSpec implements NobaseConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private File directory_file;
private boolean is_user;
private boolean is_exclude;
   



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

NobasePathSpec(Element xml)
 {
   directory_file = new File(IvyXml.getTextElement(xml,"DIR"));
   is_user = IvyXml.getAttrBool(xml,"USER");
   is_exclude = IvyXml.getAttrBool(xml,"EXCLUDE");
}


NobasePathSpec(File f,boolean u,boolean e)
{
   directory_file = f;
   is_user = u;
   is_exclude = e;
}



/********************************************************************************/
/*                                                                              */
/*      AccessMethods                                                           */
/*                                                                              */
/********************************************************************************/

File getFile()		                { return directory_file; }

boolean isUser()		        { return is_user; }

boolean isExclude()                     { return is_exclude; }

void setProperties(boolean usr,boolean exc) 
{
   is_user = usr;
   is_exclude = exc;
}



/********************************************************************************/
/*                                                                              */
/*      Matching methods                                                        */
/*                                                                              */
/********************************************************************************/

boolean match(File path)
{
   if (!directory_file.isAbsolute()) {
      String par = directory_file.getParent();
      if (par == null || par.equals("*") || par.equals("**")) {
         if (path.getName().equals(directory_file.getName())) return true;
       }
    }
   else if (path == null) return false;
   else if (path.equals(directory_file)) return true;
     
   return false;
}




/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

public void outputXml(IvyXmlWriter xw) 
{
   xw.begin("PATH");
   xw.field("DIR",directory_file.getPath());
   xw.field("USER",is_user);
   xw.field("EXCLUDE",is_exclude);
   xw.end("PATH");
}




}       // end of class NobasePathSpec




/* end of NobasePathSpec.java */

