/********************************************************************************/
/*                                                                              */
/*              NobaseDebugStackFrame.java                                      */
/*                                                                              */
/*      Representation of a javascript stack frame                              */
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


import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.TreeMap;

class NobaseDebugStackFrame implements NobaseConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private int     frame_index;
private String  frame_function;
private String  frame_file;
private int     source_offset;
private int     source_line;
private int     source_column;
private boolean is_constructor;
private boolean is_internal;

private Map<String,NobaseDebugValue> local_values;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

NobaseDebugStackFrame(JSONObject json,NobaseDebugRefMap refmap)
{
   frame_index = json.optInt("index");
   frame_file = json.optString("script");
   source_offset = json.optInt("position");
   source_line = json.optInt("line");
   source_column = json.optInt("column");
   is_constructor = json.optBoolean("constructCall");
   is_internal = json.optBoolean("debuggerFrame");
   local_values = new TreeMap<String,NobaseDebugValue>();
   JSONArray jarr = json.optJSONArray("arguments");
   if (jarr != null) {
      for (int i = 0; i < jarr.length(); ++i) {
         JSONObject nval = jarr.getJSONObject(i);
         String nm = nval.optString("name");
         if (nm == null) nm = "$ARG$_" + (i+1);
         NobaseDebugValue val = NobaseDebugValue.getValue(nval.opt("value"),refmap);
         local_values.put(nm,val);
       }
    }
   jarr = json.optJSONArray("locals");
   if (jarr != null) {
      for (int i = 0; i < jarr.length(); ++i) {
         JSONObject nval = jarr.getJSONObject(i);
         String nm = nval.getString("name");
         Object vval = nval.opt("value");
         NobaseDebugValue val = NobaseDebugValue.getValue(vval,refmap);
         local_values.put(nm,val);
       }
    }
}   
   



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

void outputXml(IvyXmlWriter xw,int ctr,int lvl)
{
   xw.begin("STACKFRAME");
   xw.field("NAME",frame_function);
   xw.field("ID",frame_index);
   xw.field("LINENO",source_line);
   xw.field("LEVEL",frame_index);
   xw.field("FILE",frame_file);
   xw.field("FILETYPE","JS");
   if (is_constructor) xw.field("CONSTRUCTOR",true);
   if (is_internal) xw.field("SYNTHETIC",true);
   for (Map.Entry<String,NobaseDebugValue> ent : local_values.entrySet()) {
      NobaseDebugValue val = ent.getValue();
      if (val != null) val.outputXml(ent.getKey(),lvl,xw);
    }
   xw.end("STACKFRAME");
}



}       // end of class NobaseDebugStackFrame




/* end of NobaseDebugStackFrame.java */

