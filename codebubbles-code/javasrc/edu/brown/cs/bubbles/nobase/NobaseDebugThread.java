/********************************************************************************/
/*										*/
/*		NobaseDebugThread.java						*/
/*										*/
/*	Information for the current javascript thread				*/
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



package edu.brown.cs.bubbles.nobase;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class NobaseDebugThread implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		thread_id;
private boolean 	is_running;
private boolean 	is_terminated;
private String		continue_reason;
private List<NobaseDebugStackFrame> cur_stack;
private NobaseDebugCommand stack_command;

private NobaseDebugTarget for_target;

private static IdCounter       thread_counter = new IdCounter();


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseDebugThread(NobaseDebugTarget tgt)
{
   for_target = tgt;
   is_running = false;
   is_terminated = false;
   continue_reason = null;
   cur_stack = null;
   stack_command = null;

   thread_id = "THREAD_" + thread_counter.nextValue();
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

boolean canResume()
{
   return !is_running && !is_terminated;
}


boolean canSuspend()
{
   return !is_terminated && is_running;
}


boolean canTerminate()
{
   return !is_terminated;
}


boolean isTerminated()			{ return is_terminated; }

boolean isSuspended()		
{
   return !is_terminated && !is_running;
}

boolean isRunning()
{
   return !is_terminated && is_running;
}


String getLocalId()			{ return thread_id; }
String getName()			{ return "*MAIN*"; }


void setContinue(String reason)
{
   continue_reason = reason;
}


void setRunning(boolean running)
{
   if (is_running != running) {
      is_running = running;
      if (running) {
	 for_target.generateThreadEvent("RESUME",continue_reason,this);
	 cur_stack = null;
	 stack_command = null;
       }
      else {
	 for_target.generateThreadEvent("SUSPEND",continue_reason,this);
	 stack_command = new NobaseDebugCommand.Backtrace(for_target,0,1000,false);
	 for_target.postCommand(stack_command);
       }
    }
   if (!running) continue_reason = null;
}




/********************************************************************************/
/*										*/
/*	Stack frame maintenance 						*/
/*										*/
/********************************************************************************/

boolean hasStackFrames()
{
   return (cur_stack != null && cur_stack.size() > 0);
}


List<NobaseDebugStackFrame> getStackFrames()
{
   NobaseDebugCommand cmd = stack_command;

   if (isSuspended() && cur_stack != null) {
      return cur_stack;
    }
   else if (isSuspended() && cmd != null) {
      NobaseDebugResponse rply = cmd.getResponse();
      synchronized (this) {
	 if (cur_stack == null) {
	    if (rply != null) {
	       JSONObject body = rply.getBody();
               NobaseDebugRefMap refmap = rply.getRefMap();
	       if (body != null) {
		  JSONArray frms = body.optJSONArray("frames");
                  List<NobaseDebugStackFrame> rslt = new ArrayList<NobaseDebugStackFrame>();
                  if (frms != null) {
                     for (int i = 0; i < frms.length(); ++i) {
                        JSONObject frm = frms.getJSONObject(i);
                        if (frm != null) rslt.add(new NobaseDebugStackFrame(frm,refmap));
                      }
                   }
		  cur_stack = rslt;
		}
	     }
	  }
	 if (cur_stack == null) {
	    cur_stack = new ArrayList<NobaseDebugStackFrame>();
	  }
       }
      return cur_stack;
    }
   return new ArrayList<NobaseDebugStackFrame>();
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw)
{
   xw.begin("THREAD");
   xw.field("ID",thread_id);
   xw.field("NAME","User Thread");
   xw.field("SYSTEM",false);
   xw.field("SUSPENDED",isSuspended());
   xw.field("TERMINATED",isTerminated());
   if (isSuspended() & hasStackFrames()) {
      xw.field("STACK",true);
      xw.field("FRAMES",cur_stack.size());
    }
   xw.end("THREAD");
}





}	// end of class NobaseDebugThread




/* end of NobaseDebugThread.java */

