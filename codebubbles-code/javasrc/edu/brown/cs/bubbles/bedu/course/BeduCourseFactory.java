/********************************************************************************/
/*										*/
/*		BeduCourseFactory.java						*/
/*										*/
/*	Bubbles for Education :: course-related options 			*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven Reiss 			*/
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

package edu.brown.cs.bubbles.bedu.course;


import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.*;

import edu.brown.cs.ivy.exec.IvyExec;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;



public class BeduCourseFactory {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BoardProperties 		bedu_props;
private String				course_name;

private static BeduCourseFactory	the_factory = null;

		

/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public static synchronized BeduCourseFactory getFactory()
{
   if (the_factory == null) {
      the_factory = new BeduCourseFactory();
    }
   return the_factory;
}


private BeduCourseFactory()
{
   bedu_props = BoardProperties.getProperties("Bedu");
   course_name = BoardSetup.getSetup().getCourseName();
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

public static void setup()
{
   // add menu buttons
}



public static void initialize(BudaRoot br)
{
   getFactory().initializeOptions();
}



private void initializeOptions()
{
   if (course_name == null) return;

   addSubmitButtons();

   String helpurl = bedu_props.getProperty("Bedu.help." + course_name);
   if (helpurl != null) {
      BudaRoot.registerMenuButton("Bubble.Help.Help for " + course_name,new ShowPage(helpurl));
    }
}



/********************************************************************************/
/*										*/
/*	Methods to handle homework submissions					*/
/*										*/
/********************************************************************************/

private void addSubmitButtons()
{
   String anm = BoardSetup.getSetup().getCourseAssignment();
   if (anm != null) {
      String prop = bedu_props.getProperty("Bedu.submit." + course_name + "." + anm);
      if (prop != null) {
	 String nm = "Bubble." + course_name + ".Submit " + anm;
	 BudaRoot.registerMenuButton(anm,new Submitter(nm,prop));
       }
      return;
    }

   Map<String,String> cmds = new TreeMap<String,String>();
   String pfx = "Bedu.submit." + course_name + ".";
   int pln = pfx.length();

   for (String s : bedu_props.stringPropertyNames()) {
      if (s.startsWith(pfx)) {
	 String nm = s.substring(pln);
	 if (nm == null || nm.length() == 0) continue;
	 cmds.put(nm,bedu_props.getProperty(s));
       }
    }

   if (cmds.size() == 0) return;

   String bnm = "Bubble." + course_name + ".";
   for (Map.Entry<String,String> ent : cmds.entrySet()) {
      String nm = bnm + "Submit " + ent.getKey();
      BudaRoot.registerMenuButton(nm,new Submitter(ent.getKey(),ent.getValue()));
    }
}



private class Submitter implements BudaConstants.ButtonListener {

   private String project_name;
   private String command_text;

   Submitter(String proj,String cmd) {
      project_name = proj;
      command_text = cmd;
    }

   @Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt) {
      StringBuffer buf = new StringBuffer();
      buf.append("<html>");

      try {
	 IvyExec ex = new IvyExec(command_text,IvyExec.READ_OUTPUT);
	 InputStream ins = ex.getInputStream();
	 for ( ; ; ) {
	    int b = ins.read();
	    if (b < 0) break;
	    b &= 0xff;
	    char c = (char) b;
	    buf.append(c);
	    if (c == '\n') buf.append("<br>");
	  }
	 ex.waitFor();
       }
      catch (IOException e) {
	 buf.append("<br><b>Problem doing submit: " + e);
       }

      BudaErrorBubble bbl = new BudaErrorBubble(buf.toString(),Color.BLACK);
      bba.addBubble(bbl,null,pt,BudaConstants.PLACEMENT_MOVETO);

      showSurvey();
    }

   private void showSurvey() {
      String pnm = "Bedu.survey." + course_name + "." + project_name + ".done";
      if (bedu_props.getBoolean(pnm)) return;
      String snm = "Bedu.survey." + course_name + "." + project_name;
      String url = bedu_props.getProperty(snm);
      if (url == null) {
	 snm = "Bedu.survey." + course_name;
	 url = bedu_props.getProperty(snm);
       }
      if (url == null) return;
      bedu_props.setProperty(pnm,true);
      try {
	 bedu_props.save();
       }
      catch (IOException e) { }
      try {
	 URI u = new URI(url);
	 Desktop.getDesktop().browse(u);
       }
      catch (Throwable t) {
	 BoardLog.logE("BEDU","Problem accessing url " + url);
       }
    }

}	// end of inner class Submitter




/********************************************************************************/
/*										*/
/*	Methods to show the help page						*/
/*										*/
/********************************************************************************/

private static class ShowPage implements BudaConstants.ButtonListener {

   private String url_name;

   ShowPage(String url) {
      url_name = url;
    }

   @Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt) {
      try {
	 URI u = new URI(url_name);
	 Desktop.getDesktop().browse(u);
       }
      catch (Throwable t) {
	 BoardLog.logE("BEDU","Problem accessing url " + url_name);
       }
    }

}	// end of inner class ShowPage



}	// end of class BeduCourseFactory



/* end of BeduCourseFactory.java */
