/********************************************************************************/
/*										*/
/*		BdynProcess.java						*/
/*										*/
/*	Maintain dynamic information for a single process			*/
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



package edu.brown.cs.bubbles.bdyn;

import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.util.*;


class BdynProcess implements BdynConstants, BumpConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BumpProcess	for_process;
private TrieNodeImpl	root_node;
private double		base_samples;
private double		total_samples;
private double		base_time;
private BdynEventTrace	event_trace;

private int		event_seq;
private int		trace_seq;

private PriorityQueue<Element> queued_events;
private PriorityQueue<Element> trace_events;
private Map<Integer,ThreadData> thread_data;
private Map<Integer,TrieNodeImpl> trie_data;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BdynProcess(BumpProcess bp)
{
   for_process = bp;
   root_node = null;
   base_samples = 0;
   total_samples = 0;
   base_time = 0;
   event_seq = 0;
   trace_seq = 0;
   queued_events = new PriorityQueue<Element>(10,new EventComparator());
   trace_events = new PriorityQueue<Element>(10,new EventComparator());
   thread_data = new HashMap<Integer,ThreadData>();
   trie_data = new HashMap<Integer,TrieNodeImpl>();

   event_trace = new BdynEventTrace(bp);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

TrieNode getTrieRoot()			{ return root_node; }

double getBaseSamples() 		{ return base_samples; }
double getTotalSamples()		{ return total_samples; }
double getBaseTime()			{ return base_time; }
BdynEventTrace getEventTrace()		{ return event_trace; }


void finish()
{
   event_trace = null;
}



/********************************************************************************/
/*										*/
/*	Event Handlers								*/
/*										*/
/********************************************************************************/

synchronized void handleTrieEvent(Element xml)
{
   // System.err.println("TRIE: " + IvyXml.convertXmlToString(xml));
   
   int seqid = IvyXml.getAttrInt(xml,"SEQ");
   if (seqid == event_seq+1) {
      processTrieEvent(xml);
      while (!queued_events.isEmpty()) {
	 Element e1 = queued_events.element();
	 int sq = IvyXml.getAttrInt(e1,"SEQ");
	 if (sq != event_seq+1) break;
	 e1 = queued_events.remove();
	 processTrieEvent(e1);
       }
    }
   else {
      queued_events.add(xml);
    }
}



synchronized void handleTraceEvent(Element xml)
{
   int seqid = IvyXml.getAttrInt(xml,"SEQ");
   if (seqid == trace_seq+1) {
      processTraceEvent(xml);
      while (!trace_events.isEmpty()) {
	 Element e1 = trace_events.element();
	 int sq = IvyXml.getAttrInt(e1,"SEQ");
	 if (sq != trace_seq+1) break;
	 e1 = trace_events.remove();
	 processTraceEvent(e1);
       }
    }
   else {
      trace_events.add(xml);
    }
}



private static class EventComparator implements Comparator<Element> {

   @Override public int compare(Element e1,Element e2) {
      int i1 = IvyXml.getAttrInt(e1,"SEQ");
      int i2 = IvyXml.getAttrInt(e2,"SEQ");
      if (i1 < i2) return -1;
      if (i1 > i2) return 1;
      return 0;
    }

}	// end of inner class TrieComparator




/********************************************************************************/
/*										*/
/*	Trie Event Processing							*/
/*										*/
/********************************************************************************/

private void processTrieEvent(Element xml)
{
   base_samples = IvyXml.getAttrDouble(xml,"ACTIVE",0);
   total_samples = IvyXml.getAttrDouble(xml,"SAMPLES",0);
   base_time = IvyXml.getAttrDouble(xml,"TIME",0);
   event_seq = IvyXml.getAttrInt(xml,"SEQ",event_seq+1);

   for (Element thel : IvyXml.children(xml,"THREAD")) {
      handleThreadData(thel);
    }
   for (Element trel : IvyXml.children(xml,"TRIENODE")) {
      handleTrieNode(trel);
    }
}


private void handleThreadData(Element xml)
{
   int id = IvyXml.getAttrInt(xml,"ID");
   if (thread_data.get(id) == null) {
      ThreadData td = new ThreadData(xml);
      thread_data.put(id,td);
    }
}



private void handleTrieNode(Element xml)
{
   int id = IvyXml.getAttrInt(xml,"ID");
   TrieNodeImpl tn = trie_data.get(id);
   if (tn == null) {
      tn = new TrieNodeImpl(xml);
      trie_data.put(id,tn);
      if (IvyXml.getAttrBool(xml,"ROOT")) {
	 root_node = tn;
       }
    }
   else tn.update(xml);
}



/********************************************************************************/
/*										*/
/*	Trace event processing							*/
/*										*/
/********************************************************************************/

private void processTraceEvent(Element xml)
{
   trace_seq = IvyXml.getAttrInt(xml,"SEQ",trace_seq+1);
   String trace = IvyXml.getText(xml);
   if (trace == null) return;
   StringTokenizer tok = new StringTokenizer(trace,"\r\n");
   while (tok.hasMoreTokens()) {
      String ln = tok.nextToken();
      processTraceData(ln);
    }
}


private void processTraceData(String s)
{
   // BoardLog.logD("BDYN","TRACE: " + s);

   event_trace.addEntry(s);
}



/********************************************************************************/
/*										*/
/*	Thread Information							*/
/*										*/
/********************************************************************************/

private class ThreadData {

   private String thread_name;
   private String thread_id;
   private BumpThread for_thread;

   ThreadData(Element xml) {
      thread_name = IvyXml.getAttrString(xml,"NAME");
      thread_id = IvyXml.getAttrString(xml,"TID");
      for (BumpThread th : for_process.getThreads()) {
	 if (th.getId().equals(thread_id) && th.getName().equals(thread_name)) {
	    for_thread = th;
	    break;
	  }
       }
    }

   BumpThread getBumpThread()			{ return for_thread; }

}	// end of inner class ThreadData



/********************************************************************************/
/*										*/
/*	Trie Node								*/
/*										*/
/********************************************************************************/

private class TrieNodeImpl implements TrieNode {

   private TrieNodeImpl parent_node;
   private List<TrieNodeImpl> child_nodes;
   private String class_name;
   private String method_name;
   private int	  line_number;
   private String file_name;
   private int [] count_data;
   private int [] total_data;
   private Map<BumpThread,int []> thread_counts;

   TrieNodeImpl(Element xml) {
      parent_node = null;
      child_nodes = null;
      class_name = null;
      method_name = null;
      line_number = 0;
      file_name = null;
      count_data = null;
      total_data = null;
      thread_counts = null;
      setValues(xml);
      updateCounts(xml);
    }

   @Override public TrieNode getParent()	{ return parent_node; }
   @Override public Collection<TrieNode> getChildren() {
      List<TrieNode> rslt = new ArrayList<TrieNode>();
      if (child_nodes != null) rslt.addAll(child_nodes);
      return rslt;
    }
   @Override public int [] getCounts()		{ return count_data; }
   @Override public Collection<BumpThread> getThreads() {
      List<BumpThread> rslt = new ArrayList<BumpThread>();
      if (thread_counts != null) {
	 rslt.addAll(thread_counts.keySet());
       }
      return rslt;
    }
   @Override public int [] getThreadCounts(BumpThread th) {
      return thread_counts.get(th);
    }
   @Override public String getClassName()		{ return class_name; }
   @Override public String getMethodName()		{ return method_name; }
   @Override public int getLineNumber() 		{ return line_number; }
   @Override public String getFileName()		{ return file_name; }

   @Override public int [] getTotals()                  { return total_data; }
   
   void update(Element xml) {
      if (parent_node == null && IvyXml.getAttrPresent(xml,"PARENT")) {
	 setValues(xml);
       }
      updateCounts(xml);
    }

   private void setValues(Element xml) {
      if (parent_node == null) {
	 int pid = IvyXml.getAttrInt(xml,"PARENT");
	 if (pid > 0) {
	    parent_node = trie_data.get(pid);
	    if (parent_node != null) parent_node.addChild(this);
	  }
       }
      class_name = IvyXml.getAttrString(xml,"CLASS",class_name);
      method_name = IvyXml.getAttrString(xml,"METHOD",method_name);
      line_number = IvyXml.getAttrInt(xml,"LINE",line_number);
      file_name = IvyXml.getAttrString(xml,"FILE",file_name);
    }

   private void addChild(TrieNodeImpl ch) {
      if (child_nodes == null) child_nodes = new ArrayList<TrieNodeImpl>(4);
      child_nodes.add(ch);
    }

   private void updateCounts(Element xml) {
      count_data = getCounts(xml,count_data);
      for (Element th : IvyXml.children(xml,"THREAD")) {
         int tid = IvyXml.getAttrInt(th,"ID");
         ThreadData td = thread_data.get(tid);
         BumpThread bt = (td == null ? null : td.getBumpThread());
         if (bt != null) {
            if (thread_counts == null) thread_counts = new HashMap<BumpThread,int []>();
            int [] cts = thread_counts.get(bt);
            if (cts == null) {
               cts = getCounts(th,cts);
               if (cts != null) thread_counts.put(bt,cts);
             }
            else getCounts(th,cts);
          }
       }
    }

   private int [] getCounts(Element xml,int [] cts) {
      int rct = IvyXml.getAttrInt(xml,"RUN",0);
      int ict = IvyXml.getAttrInt(xml,"IO",0);
      int wct = IvyXml.getAttrInt(xml,"WAIT",0);
      if (rct > 0 || ict > 0 || wct > 0) {
	 if (cts == null) cts = new int[OP_COUNT];
	 cts[OP_RUN] = rct;
	 cts[OP_IO] = ict;
	 cts[OP_WAIT] = wct;
       }
      return cts;
    }

   @Override public void computeTotals() {
      if (count_data == null && child_nodes == null) {
         total_data = null;
       }
      else {
         boolean ok = false;
         total_data = new int[OP_COUNT];
         if (count_data != null) {
            ok = true;
            for (int i = 0; i < OP_COUNT; ++i) total_data[i] += count_data[i];
          }
         if (child_nodes != null) {
            for (TrieNodeImpl tni : child_nodes) {
               tni.computeTotals();
               int [] tots = tni.total_data;
               if (tots != null) {
                  ok = true;
                  for (int i = 0; i < OP_COUNT; ++i) total_data[i] += tots[i];
                }
             }
          }
         if (!ok) total_data = null;
       }
    }
   
   @Override public String toString() {
      String s = "<";
      if (getClassName() == null) s += "^";
      else {
         s += getClassName() + "." + getMethodName() + "@" + getLineNumber();
       }
      if (count_data != null) {
         s += " " + count_data;
       }
      s += ">";
      return s;
    }
	 
}	// end of inner class TrieNode



}	// end of class BdynProcess




/* end of BdynProcess.java */

