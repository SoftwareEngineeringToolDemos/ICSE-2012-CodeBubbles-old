/********************************************************************************/
/*										*/
/*		NobaseDebugTarget.java						*/
/*										*/
/*	Interface to a running process to debug 				*/
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

import edu.brown.cs.ivy.xml.IvyJsonReader;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.*;


class NobaseDebugTarget implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Socket	comm_socket;
private DebugReader debug_reader;
private DebugWriter debug_writer;
private int	sequence_number;
private NobaseMain nobase_main;
private Process  run_process;
private NobaseLaunchConfig launch_config;
private NobaseDebugManager debug_manager;

private File	debug_file;
private NobaseDebugThread thread_data;
private boolean is_disconnected;
private String	target_id;
private OutputStream console_input;

private Map<String,ScriptData> script_map;
private Map<NobaseDebugBreakpoint,BreakData> break_map;

private static IdCounter       target_counter = new IdCounter();

private static final Charset CHAR_SET = Charset.forName("UTF-8");            // NOI18N
private static final String CONTENT_LENGTH_STR = "Content-Length: ";         // NOI18N



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseDebugTarget(NobaseDebugManager mgr,NobaseLaunchConfig cfg)
{
   launch_config = cfg;
   debug_manager = mgr;

   comm_socket = null;
   debug_reader = null;
   debug_writer = null;
   run_process = null;

   target_id = "TARGET_" + Integer.toString(target_counter.nextValue());

   sequence_number = 0;
   nobase_main = NobaseMain.getNobaseMain();
   
   script_map = new HashMap<String,ScriptData>();
   break_map = new HashMap<NobaseDebugBreakpoint,BreakData>();

   is_disconnected = false;
   thread_data = null;
   debug_file = null;
   console_input = null;
}




/********************************************************************************/
/*										*/
/*	Setup Methods								*/
/*										*/
/********************************************************************************/

void startDebug() throws NobaseException
{
   int port = findPort();
   
   thread_data = new NobaseDebugThread(this);

   try {
      List<String> cmd = launch_config.getCommandLine(debug_manager,port);
      ProcessBuilder px = new ProcessBuilder(cmd);
      File wd = launch_config.getWorkingDirectory();
      if (wd != null && wd.exists()) px.directory(wd);
      run_process = px.start();
    }
   catch (IOException e) {
      throw new NobaseException("Problem starting debug process",e);
    }

   for (int i = 0; i < 10; ++i) {
      try {
	 Socket s = new Socket("localhost",port);
	 createClient(s);
	 break;
       }
      catch (IOException e) {
	 System.err.println("Got status" + e);
       }
      try {
	 Thread.sleep(50);
       }
      catch (InterruptedException e) { }
    }

   ConsoleReader cr = new ConsoleReader(run_process.getInputStream(),false);
   cr.start();
   cr = new ConsoleReader(run_process.getErrorStream(),false);
   cr.start();
   console_input = run_process.getOutputStream();
}



private int findPort()
{
   for ( ; ; ) {
      int portid = ((int)(Math.random() * 50000)) + 4000;
      try {
	 Socket s = new Socket("localhost",portid);
         s.close();
       }
      catch (ConnectException e) {
	 return portid;
       }
      catch (IOException e) { }
    }
}




private void createClient(Socket s)
{
   comm_socket = s;
   is_disconnected = false;
   try {
      debug_reader = new DebugReader(s,this);
      debug_reader.start();
      debug_writer = new DebugWriter(s);
      debug_writer.start();
    }
   catch (IOException e) {
      NobaseMain.logE("Problem creating debug reader/writer",e);
    }
   
   generateProcessEvent("CREATE");
   generateThreadEvent("CREATE",null,thread_data);
   
   try {
      Thread.sleep(2000);
    }
   catch (InterruptedException e) { }
   
   NobaseDebugCommand cmd = new NobaseDebugCommand.Version(this);
   postCommand(cmd);
   
   cmd = new NobaseDebugCommand.ListBreakpoints(this);
   postCommand(cmd);
   
   cmd = new NobaseDebugCommand.Threads(this);
   postCommand(cmd);
   
   cmd = new NobaseDebugCommand.Scripts(this);
   postCommand(cmd);
   NobaseDebugResponse scrret = cmd.getResponse();
   if (scrret != null) {
      JSONArray body = scrret.getBodyArray();
      for (int i = 0; i  < body.length(); ++i) {
         JSONObject script = body.optJSONObject(i);
         addScript(script);
       }
    }
   
   for (NobaseDebugBreakpoint bpt : debug_manager.getBreakpoints()) {
      breakpointAdded(bpt);
    }
   
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

int getNextSequence()
{
   sequence_number += 1;
   return sequence_number;
}



boolean canTerminate()				
{ 
   if (thread_data == null) return false;
   return thread_data.canTerminate();
}
boolean isTerminated()					{ return thread_data.isTerminated(); }
boolean isSuspended()					{ return thread_data.isSuspended(); }

boolean canResume()
{
   return thread_data.canResume();
}


boolean canDropToFrame()
{
   return false;
}


boolean canSuspend()
{
   if (thread_data == null) return false;
   return thread_data.canSuspend();
}


File getFile()						{ return debug_file; }
String getId()						{ return target_id; }


NobaseDebugThread findThreadById(String tid)
{
   if (tid == null) return thread_data;
   if (thread_data.getLocalId().equals(tid)) return thread_data;
   return null;
}


boolean canDisconnect()			{ return !is_disconnected; }
boolean isDisconnected() 		{ return is_disconnected; }
void disconnect()
{
   terminate();
}


Process getProcess()				{ return run_process; }



/********************************************************************************/
/*										*/
/*	Command methods							*/
/*										*/
/********************************************************************************/

void postCommand(NobaseDebugCommand cmd)
{
   if (debug_reader != null) {
      debug_reader.addToResponseQueue(cmd);
    }

   if (debug_writer != null) {
      debug_writer.postCommand(cmd);
    }
}




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

public void processCommand(String scode,String seq,String payload)
{
   // NobaseMain.logD("DEBUG Command: " + scode + " " + seq + " " + payload);
//
   // try {
      // int cmdcode = Integer.parseInt(scode);
//
      // switch (cmdcode) {
	 // case CMD_THREAD_CREATED :
	    // processThreadCreated(payload);
	    // break;
	 // case CMD_THREAD_KILL :
	    // processThreadKilled(payload);
	    // break;
	 // case CMD_THREAD_SUSPEND :
	    // processThreadSuspended(payload);
	    // break;
	 // case CMD_THREAD_RUN :
	    // processThreadRun(payload);
	    // break;
	 // default :
	    // NobaseMain.logW("Unexpected debugger command " + scode + " " + seq + " " + payload);
	    // break;
       // }
    // }
   // catch (Exception e) {
      // NobaseMain.logE("Error processing: " + scode + " payload: "+ payload, e);
    // }
}





void resume() throws NobaseException
{
   NobaseDebugCommand cmd = new NobaseDebugCommand.Continue(this,null,0);
   thread_data.setContinue(null);
   postCommand(cmd);
}


void stepInto() 
{
   NobaseDebugCommand cmd = new NobaseDebugCommand.Continue(this,"in",1);
   thread_data.setContinue("STEP_INTO");
   postCommand(cmd);
}


void stepOver()
{
   NobaseDebugCommand cmd = new NobaseDebugCommand.Continue(this,"next",1);
   thread_data.setContinue("STEP_OVER");
   postCommand(cmd); 
}

void stepReturn()
{
   NobaseDebugCommand cmd = new NobaseDebugCommand.Continue(this,"out",0);
   thread_data.setContinue("STEP_RETURN");
   postCommand(cmd);
}


void dropToFrame() throws NobaseException
{
   throw new NobaseException("Drop to frame not supported");
}




public void suspend() throws NobaseException
{
   NobaseDebugCommand cmd = new NobaseDebugCommand.Suspend(this);
   thread_data.setContinue("SUSPEND");
   postCommand(cmd);
}


public List<NobaseDebugThread> getThreads() throws NobaseException
{
   List<NobaseDebugThread> rslt = new ArrayList<NobaseDebugThread>();
   rslt.add(thread_data);
   return rslt;
}



public synchronized void terminate()
{
   if (comm_socket != null) {
      try {
	 comm_socket.shutdownInput(); // trying to make my pydevd notice that the socket is gone
       }
      catch (Exception e) { }
      try {
	 comm_socket.shutdownOutput();
       }
      catch (Exception e) { }
      try {
	 comm_socket.close();
       }
      catch (Exception e) { }
    }
   comm_socket = null;
   is_disconnected = true;

   if (debug_writer != null) {
      debug_writer.done();
      debug_writer = null;
    }
   if (debug_reader != null) {
      debug_reader.done();
      debug_reader = null;
    }

   generateThreadEvent("TERMINATE",null,thread_data);
   thread_data = null;
   
   run_process.destroy();
   
   generateProcessEvent("TERMINATE");
}




/********************************************************************************/
/*										*/
/*	Evaluation commands							*/
/*										*/
/********************************************************************************/

void evaluateExpression(String bid,String eid,String expr,int frame,boolean brk,
      String rid,IvyXmlWriter xw)
{
   NobaseDebugCommand.Evaluate cmd = new NobaseDebugCommand.Evaluate(
         this,expr,frame,true,brk);
   EvalRunner er = new EvalRunner(bid,eid,null,cmd);
   nobase_main.startTask(er);
}



private class EvalRunner implements Runnable {

   private String bubble_id;
   private String eval_id;
   private String locator_id;
   private NobaseDebugCommand.Evaluate eval_command;

   EvalRunner(String bid,String id,String loc,NobaseDebugCommand.Evaluate cmd) {
      bubble_id = bid;
      eval_id = id;
      locator_id = loc;
      eval_command = cmd;
    }
   
   @Override public void run() {
      postCommand(eval_command);
      NobaseDebugResponse resp = eval_command.getResponse();
      if (resp != null) {
         IvyXmlWriter xw = nobase_main.beginMessage("EVALUATION",bubble_id);
         xw.field("ID",eval_id);
         xw.field("STATUS","OK");
         // need to output values here
         nobase_main.finishMessage(xw);
       }
      else {
         // handle error
       }
    }

}	// end of inner class EvalRunner




// private NobaseDebugVariable createVariable(String locator,Element xml)
// {
   // NobaseDebugVariable var = null;
//
   // String name = IvyXml.getAttrString(xml,"name");
   // String type = IvyXml.getAttrString(xml,"type");
   // String value = IvyXml.getAttrString(xml,"value");
   // boolean contain = IvyXml.getAttrBool(xml,"isContainer");
//
   // if (contain) {
      // var = new NobaseDebugVariableCollection(this,name,type,value,locator);
    // }
   // else {
      // var = new NobaseDebugVariable(this,name,type,value,locator);
    // }
//
   // return var;
// }
//


/********************************************************************************/
/*										*/
/*	Breakpoint methods							*/
/*										*/
/********************************************************************************/

void breakpointAdded(NobaseDebugBreakpoint bp)
{
   NobaseDebugCommand cmd = new NobaseDebugCommand.SetBreakpoint(this,bp);
   postCommand(cmd);
   
   BreakData bd = getBreakData(bp);
   
   NobaseDebugResponse rply = cmd.getResponse();
   if (rply != null) {
      if (rply.isSuccess()) bd.setInserted(true);
      JSONObject body = rply.getBody();
      if (body != null) {
         int id = body.optInt("breakpoint",-1);
         if (id > 0) bd.setId(id);
       }
    }
}



void breakpointRemoved(NobaseDebugBreakpoint b)
{
   BreakData bd = getBreakData(b);
   
   if (bd.isInserted()) {
      NobaseDebugCommand cmd = new NobaseDebugCommand.ClearBreakpoint(this,b,bd.getId());
      postCommand(cmd);}
      break_map.remove(b);
}


public void breakpointChanged(NobaseDebugBreakpoint breakpoint)
{
   breakpointRemoved(breakpoint);
   breakpointAdded(breakpoint);
}



/********************************************************************************/
/*										*/
/*	Execution methods							*/
/*										*/
/********************************************************************************/

private void handleDebugMessage(JSONObject msg)
{
   String typ = msg.getString("type");
   if (typ.equals("event")) {
      String event = msg.getString("event");
      JSONObject body = msg.optJSONObject("body");
      switch (event) {
         case "afterCompile" :
            if (body != null) addScript(body.optJSONObject("script"));
            break;
         case "exception" :
            thread_data.setContinue("EXCEPTION");
            break;
         case "break" :
            thread_data.setContinue("BREAK");
            break;
       }
    }
   
   boolean running = msg.optBoolean("running");
   thread_data.setRunning(running);
}




private void processThreadSuspended(String payload)
{
   // Element e = IvyXml.convertStringToXml(payload);
   // Element te = IvyXml.getElementByTag(e,"thread");
   // String tid = IvyXml.getAttrString(te,"id");
   // NobaseDebugThread t = findThreadById(tid);
   // if (t == null) {
      // NobaseMain.logE("Problem reading thread suspended data: " + payload);
      // return;
    // }
   // int sr = IvyXml.getAttrInt(te,"stop_reason");
   // DebugReason reason = DebugReason.UNSPECIFIED;
   // switch (sr) {
      // case CMD_STEP_OVER :
      // case CMD_STEP_INTO :
      // case CMD_STEP_RETURN :
      // case CMD_RUN_TO_LINE :
      // case CMD_SET_NEXT_STATEMENT :
	 // reason = DebugReason.STEP_END;
	 // break;
      // case CMD_THREAD_SUSPEND :
	 // reason = DebugReason.CLIENT_REQUEST;
	 // break;
      // case CMD_SET_BREAK :
	 // reason = DebugReason.BREAKPOINT;
	 // break;
      // default :
	 // NobaseMain.logE("Unexpected reason for suspension: " + sr);
	 // reason = DebugReason.UNSPECIFIED;
	 // break;
    // }
//
   // if (t != null) {
      // modification_checker.onlyLeaveThreads(thread_data);
      // List<NobaseDebugStackFrame> frms = new ArrayList<NobaseDebugStackFrame>();
      // for (Element fe : IvyXml.children(te,"frame")) {
	 // String fid = IvyXml.getAttrString(fe,"id");
	 // String fnm = IvyXml.getAttrString(fe,"name");
	 // String fil = IvyXml.getAttrString(fe,"file");
	 // int lno = IvyXml.getAttrInt(fe,"line");
	 // File file = null;
	 // if (fil != null) {
	    // try {
	       // fil = URLDecoder.decode(fil,"UTF-8");
	     // }
	    // catch (UnsupportedEncodingException ex) { }
	    // file = new File(fil);
	    // if (file.exists()) file = file.getAbsoluteFile();
	  // }
	 // NobaseDebugStackFrame sf = t.findStackFrameByID(fid);
	 // if (sf == null) {
	    // sf = new NobaseDebugStackFrame(t,fid,fnm,file,lno,this);
	  // }
	 // else {
	    // sf.setName(fnm);
	    // sf.setFile(file);
	    // sf.setLine(lno);
	  // }
	 // frms.add(sf);
       // }
      // t.setSuspended(true,frms);
      // generateThreadEvent("SUSPEND",reason,t);
    // }
}





private void processThreadRun(String payload)
{
   // try {
      // String [] threadIdAndReason = getThreadIdAndReason(payload);
      // DebugReason resumereason = DebugReason.UNSPECIFIED;
      // try {
	 // int raw_reason = Integer.parseInt(threadIdAndReason[1]);
	 // switch (raw_reason) {
	    // case CMD_STEP_OVER :
	       // resumereason = DebugReason.STEP_OVER;
	       // break;
	    // case CMD_STEP_RETURN :
	       // resumereason = DebugReason.STEP_RETURN;
	       // break;
	    // case CMD_STEP_INTO :
	       // resumereason = DebugReason.STEP_INTO;
	       // break;
	    // case CMD_RUN_TO_LINE :
	       // resumereason = DebugReason.UNSPECIFIED;
	       // break;
	    // case CMD_SET_NEXT_STATEMENT :
	       // resumereason = DebugReason.UNSPECIFIED;
	       // break;
	    // case CMD_THREAD_RUN :
	       // resumereason = DebugReason.CLIENT_REQUEST;
	       // break;
	    // default :
	       // NobaseMain.logE("Unexpected resume reason code " + resumereason);
	       // resumereason = DebugReason.UNSPECIFIED;
	    // }
       // }
      // catch (NumberFormatException e) {
   // expected, when pydevd reports "None"
	 // resumereason = DebugReason.UNSPECIFIED;
       // }
//
      // String threadID = threadIdAndReason[0];
      // NobaseDebugThread t = findThreadById(threadID);
      // if (t != null) {
	 // t.setSuspended(false, null);
	 // generateThreadEvent("RESUME",resumereason,t);
       // }
      // else {
	 // NobaseMain.logE("Unable to find thread " + threadID);
       // }
    // }
   // catch (Exception e1) {
      // NobaseMain.logE("Problem processing thread run",e1);
    // }
}




/********************************************************************************/
/*										*/
/*	Console methods 							*/
/*										*/
/********************************************************************************/

public void addConsoleInputListener()
{
}


void consoleInput(String txt) throws IOException
{
   console_input.write(txt.getBytes());
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

private void generateProcessEvent(String kind)
{
    IvyXmlWriter xw = nobase_main.beginMessage("RUNEVENT");
    xw.field("TIME",System.currentTimeMillis());
    xw.begin("RUNEVENT");
    xw.field("TYPE","PROCESS");
    xw.field("KIND",kind);
    outputProcessXml(xw);
    xw.end("RUNEVENT");
    nobase_main.finishMessage(xw);
}


void generateThreadEvent(String kind,String reason,NobaseDebugThread dt)
{
   IvyXmlWriter xw = nobase_main.beginMessage("RUNEVENT");
   xw.field("TIME",System.currentTimeMillis());
   xw.begin("RUNEVENT");
   xw.field("TYPE","THREAD");
   xw.field("KIND",kind);
   if (reason != null) xw.field("DETAIL",reason);
   if (dt != null) dt.outputXml(xw);
   else xw.field("NOTHREAD",true);
   xw.end("RUNEVENT");
   nobase_main.finishMessage(xw);
}



private void outputProcessXml(IvyXmlWriter xw)
{
   xw.begin("PROCESS");
   xw.field("PID",target_id);
   xw.field("TERMINATED",(comm_socket == null));
   // remote_debugger.outputXml(xw);
   xw.end("PROCESS");
}




/********************************************************************************/
/*										*/
/*	DebugReader implementation						*/
/*										*/
/********************************************************************************/

private static class DebugReader extends Thread {

    private Socket read_socket;
    private volatile boolean is_done;
    private Map<Integer,NobaseDebugCommand> response_queue;
    private InputStream in_reader;
    private NobaseDebugTarget remote_target;

    DebugReader(Socket s,NobaseDebugTarget r) throws IOException {
       super("DebugReader_" + s.toString());
       remote_target = r;
       read_socket = s;
       is_done = false;
       response_queue = new HashMap<Integer,NobaseDebugCommand>();
       InputStream sin = read_socket.getInputStream();
       in_reader = new BufferedInputStream(sin);
     }

    void done() 			{ is_done = true; }

    void addToResponseQueue(NobaseDebugCommand cmd) {
       int sequence = cmd.getSequence();
       synchronized (response_queue) {
	  response_queue.put(new Integer(sequence),cmd);
	}
     }

    private void processCommand(String jsonstr) {
       try {
          NobaseMain.logD("DEBUG RESPONSE: " + jsonstr);
    
          JSONObject json = new JSONObject(jsonstr);
          String type = json.getString("type");
          remote_target.handleDebugMessage(json);
          if (type.equals("event")) {
             // process event
           }
          else {
             int seqno = json.optInt("request_seq");
             NobaseDebugCommand cmd;
             synchronized (response_queue) {
        	cmd = response_queue.remove(new Integer(seqno));
              }
             if (cmd != null) {
        	cmd.processResponse(json);
              }
           }
        }
       catch (Exception e) {
          NobaseMain.logE("Error processing debug command",e);
          e.printStackTrace();
          throw new RuntimeException(e);
        }
     }

    @Override public void run() {
       while (!is_done) {
          try {
             String json = readJson();
             if (json == null) {
        	is_done = true;
        	break;
              }
             else if(json.trim().length() > 0) {
        	processCommand(json);
              }
           }
          catch (IOException e) {
             is_done = true;
           }
          // there was a 50ms delay here.  why?
        }
    
       if (is_done || read_socket == null || !read_socket.isConnected() ) {
          NobaseDebugTarget target = remote_target;
          if (target != null) {
             target.terminate();
           }
          is_done = true;
        }
     }

    private String readJson() throws IOException {
       StringBuffer hdr = new StringBuffer();
       int state = 0;
       in_reader.mark(100);
       for ( ; ; ) {
          int ch = in_reader.read();
          if (ch < 0) return null;
          hdr.append(((char) ch));
          switch (state) {
             case 0 :
        	if (Character.isWhitespace(ch)) ;
        	else if (ch == '{') {
        	   in_reader.reset();
        	   return readFlexJson();
        	 }
        	else state = 1;
        	break;
             case 1 :
        	if (ch == '\r') state = 2;
        	break;
             case 2 :
        	if (ch == '\n') state = 3;
        	else state = 1;
        	break;
             case 3 :
        	if (ch == '\r') state = 4;
        	else state = 1;
        	break;
             case 4 :
        	if (ch == '\n') {
        	   Map<String,String> head = processHeader(hdr.toString());
        	   String lenstr = head.get("Content-Length");
        	   int len = -1;
        	   if (lenstr != null) {
        	      try {
        		 len = Integer.parseInt(lenstr);
        	       }
        	      catch (NumberFormatException e) { }
        	    }
        	   if (len < 0) {
        	      return readFlexJson();
        	    }
        	   if (len == 0) {
        	      state = 0;
        	      hdr.setLength(0);
        	      break;
        	    }
        	   byte [] rbuf = new byte[len];
        	   int rlen = 0;
        	   while (rlen < len) {
        	      int nlen = in_reader.read(rbuf,rlen,len-rlen);
        	      if (nlen < 0) return null;
        	      rlen += nlen;
        	    }
        	   return new String(rbuf,CHAR_SET);
        	 }
        	else state = 1;
        	break;
             }
        }
     }

    private Map<String,String> processHeader(String cnts) {
       Map<String,String> rslt = new HashMap<String,String>();
       StringTokenizer tok = new StringTokenizer(cnts,"\r\n");
       while (tok.hasMoreTokens()) {
          String line = tok.nextToken();
          int idx = line.indexOf(":");
          if (idx < 0) continue;
          String key = line.substring(0,idx).trim();
          String value = line.substring(idx+1).trim();
          rslt.put(key,value);
        }
       return rslt;
     }

    @SuppressWarnings("resource") 
    private String readFlexJson() throws IOException {
       IvyJsonReader jr = new IvyJsonReader(in_reader);
       return jr.readJson();
     }

}	// end of inner class DebugReader




/********************************************************************************/
/*										*/
/*	DebugWriter implementation						*/
/*										*/
/********************************************************************************/

private static class DebugWriter extends Thread {

   private Socket write_socket;
   private List<NobaseDebugCommand> cmd_queue;
   private OutputStream out_writer;
   private volatile boolean is_done;

   DebugWriter(Socket s) throws IOException {
      super("DebugWriter_" + "_" + s);
      write_socket = s;
      cmd_queue = new ArrayList<NobaseDebugCommand>();
      out_writer = s.getOutputStream();
      is_done = false;
    }

   void postCommand(NobaseDebugCommand cmd) {
      synchronized (cmd_queue) {
	 cmd_queue.add(cmd);
	 cmd_queue.notifyAll();
       }
    }

   public void done() {
      synchronized (cmd_queue) {
	 is_done = true;
	 cmd_queue.notifyAll();
       }
    }

   @Override public void run() {
      while (!is_done) {
	 NobaseDebugCommand cmd = null;
	 synchronized (cmd_queue) {
	    while (cmd_queue.size() == 0 && !is_done) {
	       try {
		  cmd_queue.wait();
		}
	       catch (InterruptedException e) { }
	     }
	    if (is_done) break;
	    cmd = cmd_queue.remove(0);
	  }
	 try {
	    if (cmd != null) {
	       String c = cmd.getOutgoing();
	       NobaseMain.logD("DEBUG COMMAND " + c);
	       if (c != null) {
		  byte [] bytes = c.getBytes(CHAR_SET);
		  String cntlength = CONTENT_LENGTH_STR + bytes.length + "\r\n\r\n";
		  out_writer.write(cntlength.getBytes(CHAR_SET));
		  out_writer.write(bytes);
		  out_writer.flush();
		}
	     }
	  }
	 catch (IOException e1) {
	    is_done = true;
	  }
	 if ((write_socket == null) || !write_socket.isConnected()) {
	    is_done = true;
	  }
       }
   }

}	// end of inner class DebugWriter



/********************************************************************************/
/*										*/
/*	Handle console I/O							*/
/*										*/
/********************************************************************************/

private class ConsoleReader extends Thread {

   private Reader input_stream;
   private boolean is_stderr;

   ConsoleReader(InputStream is,boolean isstderr) {
      super("Console_" + (isstderr ? "Err" : "Out") + "_" + target_id);
      input_stream = new InputStreamReader(is);
      is_stderr = isstderr;
    }

   @Override public void run() {
      char [] buf = new char[4096];
      try {
	 for ( ; ; ) {
	    int ln = input_stream.read(buf);
	    if (ln < 0) break;
	    String txt = new String(buf,0,ln);
	    NobaseMain.logD("CONSOLE WRITE: " + is_stderr + " " + txt);
	    IvyXmlWriter xw = nobase_main.beginMessage("CONSOLE");
	    xw.field("PID",target_id);
	    xw.field("STDERR",is_stderr);
	    xw.cdataElement("TEXT",txt);
	    nobase_main.finishMessage(xw);
	  }
       }
      catch (IOException e) {
	 NobaseMain.logD("Error reading from process: " + e);
       }
      IvyXmlWriter xw = nobase_main.beginMessage("CONSOLE");
      xw.field("PID",target_id);
      xw.field("STDERR",is_stderr);
      xw.field("EOF",true);
      nobase_main.finishMessage(xw);
    }
}



/********************************************************************************/
/*                                                                              */
/*      ScriptData representation                                               */
/*                                                                              */
/********************************************************************************/

private void addScript(JSONObject obj)
{
   if (obj == null) return;
   
   int id = obj.optInt("id");
   if (id <= 0) id = obj.getInt("handle");
   
   ScriptData sd = script_map.get(Integer.toString(id));
   if (sd == null) {
      sd = new ScriptData(id,obj);
      script_map.put(Integer.toString(id),sd);
      if (sd.getFile() != null) script_map.put(sd.getFile(),sd);
    }
}



private class ScriptData {
   
   private int script_id;
   private String script_file;
   
   ScriptData(int id,JSONObject obj) {
      script_id = id;
      script_file = obj.optString("name");
      // can get other script information from obj at this point
    }
   
   int getId()                          { return script_id; }
   String getFile()                     { return script_file; }
   
}       // end of inner class ScriptData



/********************************************************************************/
/*                                                                              */
/*      Breakpoint Data                                                         */
/*                                                                              */
/********************************************************************************/

private BreakData getBreakData(NobaseDebugBreakpoint bp)
{
   synchronized (break_map) {
      BreakData bd = break_map.get(bp);
      if (bd == null) {
         bd = new BreakData(bp);
         break_map.put(bp,bd);
       }
      return bd;
    }
}



private class BreakData {
   
   private NobaseDebugBreakpoint break_point;
   private int break_id;
   private boolean is_inserted;
   
   BreakData(NobaseDebugBreakpoint bp) {
      break_point = bp;
      break_id = 0;
      is_inserted = false;
    }
   
   NobaseDebugBreakpoint getBreakpoint()                { return break_point; }
   boolean isInserted()                                 { return is_inserted; }
   int getId()                                          { return break_id; }
   
   void setInserted(boolean fg)                         { is_inserted = fg; }
   void setId(int id)                                   { break_id = id; }
   
}       // end of inner class BreakData





}	// end of class NobaseDebugTarget




/* end of NobaseDebugTarget.java */

