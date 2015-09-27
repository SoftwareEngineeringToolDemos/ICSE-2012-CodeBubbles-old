/********************************************************************************/
/*                                                                              */
/*              NobaseDebugCommand.java                                         */
/*                                                                              */
/*      Command to send to the debugger                                         */
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

import org.json.JSONObject;

import java.util.List;
import java.util.Map;

abstract class NobaseDebugCommand implements NobaseConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private int     sequence_number;
private NobaseDebugTarget for_target;
private NobaseDebugResponse response_object;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected NobaseDebugCommand(NobaseDebugTarget tgt)
{
   for_target = tgt;
   sequence_number = tgt.getNextSequence();
   response_object = null;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

int getSequence()                       { return sequence_number; }

String getOutgoing()  
{
   JSONObject obj = new JSONObject();
   obj.put("seq",sequence_number);
   obj.put("type","request");
   obj.put("command",getCommandName());
   JSONObject args = getCommandArguments();
   if (args != null) {
      obj.put("arguments",args);
    }
   
   return obj.toString();
}


protected String getCommandName()
{
   String nm = getClass().getName();
   int idx = nm.lastIndexOf("$");
   nm = nm.substring(idx+1).toLowerCase();
   return nm;
}


protected JSONObject getCommandArguments()
{
   return null;
}



void processResponse(JSONObject response) 
{
   synchronized(this) {
      response_object = new NobaseDebugResponse(response);
      notifyAll();
    }
}

NobaseDebugResponse getResponse()
{
   synchronized (this) {
      while (response_object == null) {
         try {
            wait(1000);
          }
         catch (InterruptedException e) { }
       }
    }
   return response_object;
}




/********************************************************************************/
/*                                                                              */
/*      Backtrace Command                                                       */
/*                                                                              */
/********************************************************************************/

static class Backtrace extends NobaseDebugCommand {
   
   private int from_frame;
   private int to_frame;
   private boolean use_bottom;
   private boolean inline_refs;
   
   Backtrace(NobaseDebugTarget tgt,int from,int to,boolean btm) {
      super(tgt);
      from_frame = from;
      to_frame = to;
      use_bottom = btm;
      inline_refs = false;
    }
   
   @Override protected JSONObject getCommandArguments() {
      if (from_frame <= 0 && to_frame <= 0 && !use_bottom) return null;
      JSONObject rslt = new JSONObject();
      if (from_frame > 0) rslt.put("fromFrame",from_frame);
      if (to_frame > 0) rslt.put("toFrame",to_frame);
      if (use_bottom) rslt.put("bottom",use_bottom);
      if (inline_refs) rslt.put("inlineRefs",inline_refs);
      return rslt;
    }

}       // end of inner class Backtrace



/********************************************************************************/
/*                                                                              */
/*      ChangeBreakpoint Command                                                */
/*                                                                              */
/********************************************************************************/

static class ChangeBreakpoint extends NobaseDebugCommand {
   
   ChangeBreakpoint(NobaseDebugTarget tgt,NobaseDebugBreakpoint bpt) {
      super(tgt);
    }
   
}       // end of inner class ChangeBreakpoint


/********************************************************************************/
/*                                                                              */
/*      ChangeLive command (not documented)                                     */
/*                                                                              */
/********************************************************************************/

static class ChangeLive extends NobaseDebugCommand {
   
   ChangeLive(NobaseDebugTarget tgt,int scriptid,String src) {
      super(tgt);
    }
   
}       // end of inner class ChangeLive



/********************************************************************************/
/*                                                                              */
/*      ClearBreakpoint Command                                                 */
/*                                                                              */
/********************************************************************************/

static class ClearBreakpoint extends NobaseDebugCommand {
   
   private NobaseDebugBreakpoint break_point;
   private int break_id;
   
   ClearBreakpoint(NobaseDebugTarget tgt,NobaseDebugBreakpoint bpt,int id) {
      super(tgt);
      break_point = bpt;
      break_id = id;
    }
   
   @Override protected String getCommandName() {
      if (break_point.getType() == BreakType.EXCEPTION) 
         return "setexceptionbreak";
      return super.getCommandName(); 
    }
   
    @Override protected JSONObject getCommandArguments() {
     JSONObject rslt = new JSONObject();
     switch (break_point.getType()) {
        case LINE :
           rslt.put("breakpoint",break_id);
           break;
        case EXCEPTION :
           rslt.put("enabled",false);
           break;
        case NONE :
           return null;
      }
     return rslt;
   }
   
}       // end of inner class ClearBreakpoint




/********************************************************************************/
/*                                                                              */
/*      ClearBreakpointGroup command (not documented                            */
/*                                                                              */
/********************************************************************************/

static class ClearBreakpointGroup extends NobaseDebugCommand {
   
   ClearBreakpointGroup(NobaseDebugTarget tgt,int grp) {
      super(tgt);
    }
   
}       // end of inner class ClearBreakpointGroup




/********************************************************************************/
/*                                                                              */
/*      Continue Command                                                        */
/*                                                                              */
/********************************************************************************/

static class Continue extends NobaseDebugCommand {
   
   private String step_action;
   private int step_count;
   
   Continue(NobaseDebugTarget tgt,String action,int ct) {
      super(tgt);
      step_action = action;
      step_count = ct;
    }
   
   @Override protected JSONObject getCommandArguments() {
      if (step_action == null) return null;
      JSONObject args = new JSONObject();
      args.put("stepaction",step_action);
      if (step_count > 1) args.put("stepcount",step_count);
      return args;
    }
   
}       // end of inner class Continue




/********************************************************************************/
/*                                                                              */
/*      Disconnect Command                                                      */
/*                                                                              */
/********************************************************************************/

static class Disconnect extends NobaseDebugCommand {
   
   Disconnect(NobaseDebugTarget tgt) {
      super(tgt);
    }
   
}       // end of inner class Disconnect




/********************************************************************************/
/*                                                                              */
/*      Evaluate Command                                                        */
/*                                                                              */
/********************************************************************************/

static class Evaluate extends NobaseDebugCommand {
   
   private String eval_expression;
   private int frame_id;
   private boolean is_global;
   private boolean disable_break;
   
   Evaluate(NobaseDebugTarget tgt,String expr,int frame,boolean global,boolean nobreak) {
      super(tgt);
      eval_expression = expr;
      frame_id = frame;
      is_global = global;
      disable_break = nobreak;
    }
   
}       // end of inner class Evaluate




/********************************************************************************/
/*                                                                              */
/*      Flags Command (not documented)                                          */
/*                                                                              */
/*      Possible flags: breakPointsActive, breakOnCaughtException,              */
/*         breakOnUncaughtExceptoin                                             */
/*                                                                              */
/********************************************************************************/

static class Flags extends NobaseDebugCommand {
   
   Flags(NobaseDebugTarget tgt,Map<String,String> flags) {
      super(tgt);
    }
   
}       // end of inner class Flags




/********************************************************************************/
/*                                                                              */
/*      Frame Command                                                           */
/*                                                                              */
/********************************************************************************/

static class Frame extends NobaseDebugCommand {
   
   Frame(NobaseDebugTarget tgt,int frame) {
      super(tgt);
    }
   
}       // end of inner class Frame



/********************************************************************************/
/*                                                                              */
/*      GC Command                                                              */
/*                                                                              */
/********************************************************************************/

static class GC extends NobaseDebugCommand {
   
   
   GC(NobaseDebugTarget tgt,String type) {
      super(tgt);
    }
   
   
      
}       // end of inner class GC




/********************************************************************************/
/*                                                                              */
/*      ListBreakpoints Command                                                 */
/*                                                                              */
/********************************************************************************/

static class ListBreakpoints extends NobaseDebugCommand {
   
   ListBreakpoints(NobaseDebugTarget tgt) {
      super(tgt);
    }
  
    
   
}       // end of inner class ListBreakpoints




/********************************************************************************/
/*                                                                              */
/*      Lookup Command                                                          */
/*                                                                              */
/********************************************************************************/

static class Lookup extends NobaseDebugCommand {
   
   
   Lookup(NobaseDebugTarget tgt,List<Long> handles,boolean source) {
      super(tgt);
    }
   
}       // end of inner class Lookup



/********************************************************************************/
/*                                                                              */
/*      References command (undocumented)                                       */
/*                                                                              */
/********************************************************************************/

enum ReferenceType { REFERENCED_BY, CONSTRUCTED_BY };

static class References extends NobaseDebugCommand {
   
   References(NobaseDebugTarget tgt,ReferenceType typ,long hdl) {
      super(tgt);
    }
   
}




/********************************************************************************/
/*                                                                              */
/*      RestartFrame command (not documented)                                   */
/*                                                                              */
/********************************************************************************/

static class RestartFrame extends NobaseDebugCommand {
   
   private int frame_number;
   
   RestartFrame(NobaseDebugTarget tgt,int frame) {
      super(tgt);
      frame_number = frame;
    }
   
}       // end of inner class RestartFrame



/********************************************************************************/
/*                                                                              */
/*      Scope Command                                                           */
/*                                                                              */
/********************************************************************************/

static class Scope extends NobaseDebugCommand {
   
   private int scope_number;
   private int frame_number;
   
   Scope(NobaseDebugTarget tgt,int frame,int scope) {
      super(tgt);
      frame_number = frame;
      scope_number = scope;
    }
   
}       // end of inner class Scope




/********************************************************************************/
/*                                                                              */
/*      Scopes Command                                                          */
/*                                                                              */
/********************************************************************************/

static class Scopes extends NobaseDebugCommand {
   
   private int frame_number;
   
   Scopes(NobaseDebugTarget tgt,int frame) {
      super(tgt);
      frame_number = frame;
    }
   
}       // end of inner class Scopes




/********************************************************************************/
/*                                                                              */
/*      Scripts Command                                                         */
/*                                                                              */
/********************************************************************************/

static class Scripts extends NobaseDebugCommand {
   
   Scripts(NobaseDebugTarget tgt) {
      super(tgt);
    }
      
}       // end of inner class Scripts




/********************************************************************************/
/*                                                                              */
/*      SetBreakpoint Command (also SetExceptionBreakpoint(                     */
/*                                                                              */
/********************************************************************************/

static class SetBreakpoint extends NobaseDebugCommand {
   
  private NobaseDebugBreakpoint break_point;
  
  SetBreakpoint(NobaseDebugTarget tgt,NobaseDebugBreakpoint bpt) {
     super(tgt);
     break_point = bpt;
   }
  
  @Override protected String getCommandName() {
     if (break_point.getType() == BreakType.EXCEPTION) 
        return "setexceptionbreak";
      return super.getCommandName();
   }
  
  @Override protected JSONObject getCommandArguments() {
     JSONObject rslt = new JSONObject();
     switch (break_point.getType()) {
        case LINE :
           rslt.put("type","script");
           rslt.put("target",break_point.getFile().getPath());
           rslt.put("line",break_point.getLine());
           rslt.put("enabled",break_point.isEnabled());
           if (break_point.getCondition() != null) {
              rslt.put("condition",break_point.getCondition());
            }
           break;
        case EXCEPTION :
           if (!break_point.isEnabled() || 
                 (!break_point.isCaught() && !break_point.isUncaught())) {
              rslt.put("type","all");
              rslt.put("enabled",false);
            }
           else if (break_point.isCaught()) {
              rslt.put("type","all");
            }
           else {
              rslt.put("type","uncaught");
            }
           break;
        case NONE :
           return null;
      }
     return rslt;
   }
  
}       // end of inner class SetBreakpoint



/********************************************************************************/
/*                                                                              */
/*      SetVariableValue command                                                */
/*                                                                              */
/********************************************************************************/

static class SetVariableValue extends NobaseDebugCommand {
   
   SetVariableValue(NobaseDebugTarget tgt) {
      super(tgt);
    }
   
}       // end of inner class SetVariableValue




/********************************************************************************/
/*                                                                              */
/*      Source command                                                          */
/*                                                                              */
/********************************************************************************/

static class Source extends NobaseDebugCommand {
   
   Source(NobaseDebugTarget tgt) {
      super(tgt);
    }
   
}       // end of inner class Source




/********************************************************************************/
/*                                                                              */
/*      Suspend command (not documented)                                        */
/*                                                                              */
/********************************************************************************/

static class Suspend extends NobaseDebugCommand {
   
   Suspend(NobaseDebugTarget tgt) {
      super(tgt);
    }
   
}       // end of inner class Suspend



/********************************************************************************/
/*                                                                              */
/*      Threads command (Not documented)                                        */
/*                                                                              */
/********************************************************************************/

static class Threads extends NobaseDebugCommand {
   
   Threads(NobaseDebugTarget tgt) {
      super(tgt);
    }
   
}       // end of inner class Threads




/********************************************************************************/
/*                                                                              */
/*      V8Flags command                                                         */
/*                                                                              */
/********************************************************************************/

static class V8Flags extends NobaseDebugCommand {
   
   V8Flags(NobaseDebugTarget tgt) {
      super(tgt);
    }
   
}       // end of inner class Threads



/********************************************************************************/
/*                                                                              */
/*      Version Command                                                         */
/*                                                                              */
/********************************************************************************/

static class Version extends NobaseDebugCommand {
   
   Version(NobaseDebugTarget tgt) {
      super(tgt);
    }
   
   
   
}       // end of inner class Version




}       // end of class NobaseDebugCommand




/* end of NobaseDebugCommand.java */

