/********************************************************************************/
/*										*/
/*		BattTestCase.java						*/
/*										*/
/*	Bubble Automated Testing Tool representation of a test case		*/
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


/* SVN: $Id$ */


package edu.brown.cs.bubbles.batt;

import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.util.*;



class BattTestCase implements BattConstants, BattConstants.BattTest, Comparable<BattTestCase>
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		test_name;
private String		class_name;
private String		method_name;
private TestStatus	test_status;
private TestState	test_state;
private String		fail_message;
private String		fail_trace;
private CountData	count_data;
private long		update_time;
private String		test_class;
private Set<String>	annotation_types;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BattTestCase(String name)
{
   test_name = name;
   test_status = TestStatus.UNKNOWN;
   test_state = TestState.UNKNOWN;
   fail_message = null;
   fail_trace = null;
   update_time = System.currentTimeMillis();
   annotation_types = new HashSet<String>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public synchronized String getName()			{ return test_name; }
@Override public synchronized String getClassName()		{ return class_name; }
@Override public synchronized String getMethodName()		{ return method_name; }

synchronized TestStatus getStatus()				{ return test_status; }
synchronized TestState getState()				{ return test_state; }

synchronized void setStatus(TestStatus sts)
{
   if (sts == test_status) return;
   update();
   test_status = sts;
}

synchronized void setState(TestState st)
{
   if (st == test_state) return;
   update();
   test_state = st;
}




/********************************************************************************/
/*										*/
/*	Methods to take data from the tester					*/
/*										*/
/********************************************************************************/

synchronized boolean handleTestState(Element e)
{
   boolean chng = false;

   class_name = IvyXml.getAttrString(e,"CLASS");
   method_name = IvyXml.getAttrString(e,"METHOD");
   test_name = IvyXml.getAttrString(e,"NAME");
   test_class = IvyXml.getAttrString(e,"TCLASS");

   TestStatus osts = test_status;
   TestState ost = test_state;
   
   boolean ignore = false;
   annotation_types.clear();
   for (Element ane : IvyXml.children(e,"ANNOT")) {
      String ant = IvyXml.getText(ane);
      annotation_types.add(ant);
      if (ant.startsWith("@org.junit.Ignore")) ignore = true;
    }
   if (IvyXml.getAttrBool(e,"EMPTY")) ignore = true;
   
   String sts = IvyXml.getAttrString(e,"STATUS");
   if (sts.equals("FAILURE")) {
      test_status = TestStatus.FAILURE;
      if (test_state == TestState.RUNNING || test_state == TestState.UNKNOWN)
	 test_state = TestState.UP_TO_DATE;
    }
   else if (sts.equals("SUCCESS")) {
      test_status = TestStatus.SUCCESS;
      if (test_state == TestState.RUNNING || test_state == TestState.UNKNOWN)
	 test_state = TestState.UP_TO_DATE;
    }
   else {
      if (ignore) test_state = TestState.IGNORED;
      test_status = TestStatus.UNKNOWN;
      count_data = null;
    }
   if (osts != test_status) chng = true;
   
   if (test_status == TestStatus.FAILURE) {
      String omsg = fail_message;
      fail_message = IvyXml.getTextElement(e,"EXCEPTION");
      fail_trace = IvyXml.getTextElement(e,"TRACE");
      if (fail_trace != null && fail_message == null) {
	 int idx = fail_trace.indexOf("\n");
	 if (idx < 0) fail_message = fail_trace;
	 else fail_message = fail_trace.substring(0,idx);
       }
      if (omsg == null && fail_message != null) chng = true;
      else if (omsg != null && fail_message == null) chng = true;
      else if (omsg != null && !omsg.equals(fail_message)) chng = true;
    }
   else {
      fail_message = null;
      fail_trace = null;
    }

   String st = IvyXml.getAttrString(e,"STATE");
   if (st != null) {
      try {
	 test_state = TestState.valueOf(st);
       }
      catch (IllegalArgumentException ex) { }
    }

   if (ost != test_state) chng = true;

   Element xe = IvyXml.getChild(e,"COVERAGE");
   if (xe != null) count_data = new CountData(xe);

   if (chng) update();

   return chng;
}



synchronized void handleTestCounts(Element e)
{
   if (e == null) count_data = null;
   else count_data = new CountData(e);
}



/********************************************************************************/
/*										*/
/*	Check for class change							*/
/*										*/
/********************************************************************************/

synchronized FileState usesClasses(Map<String,FileState> clsmap)
{
   FileState fs = null;

   fs = clsmap.get(class_name);
   if (count_data == null) return fs;

   return count_data.usesClasses(clsmap,fs);
}



@Override synchronized public UseMode usesMethod(String mthd)
{
   if (count_data == null) return UseMode.UNKNOWN;

   return count_data.getMethodUsage(mthd);
}


long getUpdateTime()			{ return update_time; }


private void update()
{
   update_time = System.currentTimeMillis();
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

synchronized void shortReport(IvyXmlWriter xw)
{
   xw.begin("TEST");
   xw.field("NAME",test_name);
   xw.field("STATUS",test_status);
   xw.field("STATE",test_state);
   xw.field("CLASS",class_name);
   if (test_class != null) xw.field("TCLASS",test_class);
   xw.field("METHOD",method_name);
   xw.end("TEST");
}



synchronized void longReport(IvyXmlWriter xw)
{
   xw.begin("TEST");
   xw.field("NAME",test_name);
   xw.field("STATUS",test_status);
   xw.field("STATE",test_state);
   xw.field("CLASS",class_name);
   if (test_class != null) xw.field("TCLASS",test_class);
   xw.field("METHOD",method_name);

   if (fail_message != null) {
      xw.cdataElement("EXCEPTION",fail_message);
      xw.cdataElement("TRACE",fail_trace);
    }

   if (count_data != null) count_data.report(xw);

   for (String s : annotation_types) {
      xw.textElement("ANNOT",s);
    }

   xw.end("TEST");
}




/********************************************************************************/
/*										*/
/*	Tool tip methods							 */
/*										*/
/********************************************************************************/

synchronized String getToolTip()
{
   StringBuffer buf = new StringBuffer();
   buf.append("<html>");
   buf.append("<c><b>TEST ");
   buf.append(test_name);
   buf.append("</b></c><hr>");
   buf.append("<table cellpadding=0 cellspacing=1 align=left >");
   buf.append("<tr><td>STATUS&nbsp;</td><td>");
   buf.append(test_status.toString());
   buf.append("</td></tr>");
   buf.append("<tr><td>STATE</td><td>");
   buf.append(test_state.toString());
   buf.append("</td></tr>");
   if (fail_message != null) {
      buf.append("<tr><td>ERROR</td><td>");
      buf.append(fail_message);
      buf.append("</td></tr>");
    }
   if (fail_trace != null) {
      StringTokenizer tok = new StringTokenizer(fail_trace,"\n\t");
      String s1 = tok.nextToken();
      buf.append("<tr><td>TRACE</td><td>" + s1 + "</td></tr>");
      while (tok.hasMoreTokens()) {
	 String s = tok.nextToken();
	 buf.append("<tr><td></td><td>&nbsp;&nbsp;" + s + "</td></tr>");
      }
	 
    }
   buf.append("</table>");

   return buf.toString();
}






/********************************************************************************/
/*										*/
/*	Comparison methods							*/
/*										*/
/********************************************************************************/

@Override public int compareTo(BattTestCase btc)
{
   return getName().compareTo(btc.getName());
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return getName();
}




/********************************************************************************/
/*										*/
/*	CountData -- data representing test coverage information		*/
/*										*/
/********************************************************************************/

private static class CountData {

   private Map<String,MethodCountData>	  method_data;

   CountData(Element e) {
      method_data = new HashMap<String,MethodCountData>();

      for (Element me : IvyXml.children(e,"METHOD")) {
	 MethodCountData mcd = new MethodCountData(me);
	 method_data.put(mcd.getName(),mcd);
       }
    }

   FileState usesClasses(Map<String,FileState> clsset,FileState st) {
      for (MethodCountData mcd : method_data.values()) {
	 st = mcd.usesClasses(clsset,st);
       }
      return st;
    }

   UseMode getMethodUsage(String mthd) {
      MethodCountData mcd = method_data.get(mthd);
      if (mcd == null) {
	 int idx0 = mthd.indexOf("(");
	 if (idx0 < 0) return UseMode.NONE;
	 String mthd0 = mthd.substring(0,idx0);
	 String mthd1 = mthd.substring(idx0);
	 for (Map.Entry<String,MethodCountData> ent : method_data.entrySet()) {
	    String nm = ent.getKey();
	    nm = nm.replace('/','.');
	    int idx = nm.indexOf("(");
	    if (idx < 0) continue;
	    if (mthd0.equals(nm.substring(0,idx))) {
	       if (BumpLocation.compareParameters(mthd1,nm.substring(idx))) {
		  mcd = ent.getValue();
		  method_data.put(mthd,mcd);
		  break;
		}
	    }
	  }
       }

      if (mcd != null) {
	 if (mcd.getTopCount() > 0) return UseMode.DIRECT;
	 if (mcd.getCalledCount() > 0) return UseMode.INDIRECT;
       }
      return UseMode.NONE;
    }

   void report(IvyXmlWriter xw) {
      xw.begin("COVERAGE");
      for (MethodCountData mcd : method_data.values()) {
	 mcd.report(xw);
       }
      xw.end("COVERAGE");
    }

}	// end of inner class CountData


private static class MethodCountData {

   private String class_name;
   private String method_name;
   private int start_line;
   private int end_line;
   private int called_count;
   private int top_count;
   private Map<String,Integer> calls_counts;
   private Map<Integer,BlockCountData> block_data;

   MethodCountData(Element e) {
      class_name = null;
      method_name = computeMethodName(e);
      start_line = IvyXml.getAttrInt(e,"START");
      end_line = IvyXml.getAttrInt(e,"END");
      called_count = IvyXml.getAttrInt(e,"COUNT");
      top_count = IvyXml.getAttrInt(e,"TOP");
      calls_counts = new HashMap<String,Integer>();
      block_data = new HashMap<Integer,BlockCountData>();
      for (Element be : IvyXml.children(e,"CALLS")) {
	 int ct = IvyXml.getAttrInt(be,"CALLCOUNT");
	 String nm = computeMethodName(be);
	 calls_counts.put(nm,ct);
       }
      for (Element be : IvyXml.children(e,"BLOCK")) {
	 BlockCountData bcd = new BlockCountData(be);
	 block_data.put(bcd.getBlockIndex(),bcd);
       }
    }

   String getName()			{ return method_name; }
   int getCalledCount() 		{ return called_count; }
   int getTopCount()			{ return top_count; }

   private String computeMethodName(Element e) {
      String nm = IvyXml.getAttrString(e,"NAME");
      nm = nm.replace('/','.');
      String dsc = IvyXml.getAttrString(e,"SIGNATURE");
      if (dsc != null) {
	 int idx = dsc.lastIndexOf(")");
	 if (idx >= 0) dsc = dsc.substring(0,idx+1);
	 dsc = IvyFormat.formatTypeName(dsc);
	 nm = nm + dsc;
       }
      return nm;
    }

   FileState usesClasses(Map<String,FileState> clsset,FileState fs) {
      if (class_name == null) {
	 if (method_name == null) return fs;
	 int i1 = method_name.indexOf("(");
	 int i2 = method_name.lastIndexOf(".",i1);
	 if (i2 > 0) class_name = method_name.substring(0,i2);
	 else return fs;
       }
      FileState fs1 = clsset.get(class_name);
      if (fs1 == null) return fs;
      return fs1.merge(fs);
    }

   void report(IvyXmlWriter xw) {
      xw.begin("METHOD");
      xw.field("NAME",method_name);
      xw.field("START",start_line);
      xw.field("END",end_line);
      xw.field("COUNT",called_count);
      xw.field("TOP",top_count);
      for (Map.Entry<String,Integer> ent : calls_counts.entrySet()) {
	 xw.begin("CALLS");
	 xw.field("NAME",ent.getKey());
	 xw.field("COUNT",ent.getValue());
	 xw.end("CALLS");
       }
      for (BlockCountData bcd : block_data.values()) {
	 bcd.report(xw);
       }
      xw.end("METHOD");
    }

}	// end of inner class MethodCountData


private static class BlockCountData {

   private int block_index;
   private int start_line;
   private int end_line;
   private int enter_count;
   private Map<Integer,Integer> branch_counts;

   BlockCountData(Element e) {
      block_index = IvyXml.getAttrInt(e,"INDEX");
      start_line = IvyXml.getAttrInt(e,"START");
      end_line = IvyXml.getAttrInt(e,"END");
      enter_count = IvyXml.getAttrInt(e,"COUNT");
      branch_counts = new HashMap<Integer,Integer>();
      for (Element be : IvyXml.children(e,"BRANCH")) {
	 int to = IvyXml.getAttrInt(be,"TOBLOCK");
	 int ct = IvyXml.getAttrInt(be,"COUNT");
	 branch_counts.put(to,ct);
       }
    }

   int getBlockIndex()				{ return block_index; }

   void report(IvyXmlWriter xw) {
      xw.begin("BLOCK");
      xw.field("ID",block_index);
      xw.field("START",start_line);
      xw.field("END",end_line);
      xw.field("COUNT",enter_count);
      for (Map.Entry<Integer,Integer> ent : branch_counts.entrySet()) {
	 xw.begin("BRANCH");
	 xw.field("TOBLOCK",ent.getKey());
	 xw.field("COUNT",ent.getValue());
	 xw.end("BRANCH");
       }
      xw.end("BLOCK");
    }

}	// end of inner class BlockCountData



}	// end of class BattTestCase




/* end of BattTestCase.java */
