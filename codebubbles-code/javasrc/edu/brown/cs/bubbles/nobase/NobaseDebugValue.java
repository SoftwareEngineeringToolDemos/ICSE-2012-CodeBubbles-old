/********************************************************************************/
/*                                                                              */
/*              NobaseDebugValue.java                                           */
/*                                                                              */
/*      Representation of a JS value                                            */
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

import java.util.HashMap;
import java.util.Map;

abstract class NobaseDebugValue implements NobaseConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static NobaseDebugValue undef_value = new JSUndef();
private static NobaseDebugValue null_value = new JSNull();
private static NobaseDebugValue true_value = new JSBool(true);
private static NobaseDebugValue false_value = new JSBool(false);
private static NobaseDebugValue unknown_value = new JSUnknown();

private static final int MAX_VALUE_SIZE = 40960;




/********************************************************************************/
/*                                                                              */
/*      Static methods                                                          */
/*                                                                              */
/********************************************************************************/

static NobaseDebugValue getValue(Object val,NobaseDebugRefMap refs)
{
   if (val == null) return null_value;
   if (val instanceof Number) return new JSNumber((Number) val);
   if (val instanceof String) return new JSString(val.toString());
   if (val instanceof Boolean) {
      if ((Boolean) val) return true_value;
      else return false_value;
    }
   else if (val instanceof JSONObject) {
      JSONObject json = (JSONObject) val;
      int rid = json.optInt("ref",-1);
      if (rid < 0) return createInitialRef(json);
      NobaseDebugValue rval = null;
      if (refs != null) rval = refs.get(rid);
      if (rval == null) rval = unknown_value;
      return rval;
    }
   else if (val instanceof JSONArray) {
      return unknown_value;
    }
   
   return unknown_value;
}



static NobaseDebugRefMap createRefs(JSONArray refs)
{
   NobaseDebugRefMap rslt = new NobaseDebugRefMap();

   if (refs == null) return rslt;
   
   for (int i = 0; i < refs.length(); ++i) {
      JSONObject ref = refs.getJSONObject(i);
      int hdl = ref.getInt("handle");
      rslt.put(hdl,createInitialRef(ref));
    }
   
   for (int i = 0; i < refs.length(); ++i) {
      JSONObject ref = refs.getJSONObject(i);
      int hdl = ref.getInt("handle");
      NobaseDebugValue val = rslt.get(hdl);
      if (val != null) val.complete(ref,rslt);
    }
   return rslt;
}




private static NobaseDebugValue createInitialRef(JSONObject ref)
{
   String typ = ref.getString("type");
   switch (typ) {
      case "undefined" :
         return undef_value;
      case "null" :
         return null_value;
      case "boolean" :
         if (ref.optBoolean("value")) return true_value;
         return false_value;
      case "number" :
         Number numval = (Number) ref.get("value");
         return new JSNumber(numval);
      case "string" :
         return new JSString(ref.getString("value"));
      case "object " :
         return new JSObject(ref);
      case "function" :
         return new JSFunction(ref);
      case "frame" :
         return new JSFrame(ref);
      case "script" :
         break;
    }
   
   return null;
}




private static NobaseDebugValue getRef(JSONObject ref,String fld,
      NobaseDebugRefMap refmap)
{
   return getRef(ref.optJSONObject(fld),refmap);
}


private static NobaseDebugValue getRef(JSONObject ref,NobaseDebugRefMap refmap)
{
   if (ref == null) return null;
   int hdl = ref.optInt("handle",-1);
   if (hdl < 0) return null;
   return refmap.get(hdl);
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected NobaseDebugValue()
{
   
}




/********************************************************************************/
/*                                                                              */
/*      Creation methods                                                        */
/*                                                                              */
/********************************************************************************/

protected void complete(JSONObject ref,NobaseDebugRefMap refmap) { }




/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

void outputXml(String name,int lvl,IvyXmlWriter xw) 
{
   xw.begin("VALUE");
   if (name != null) xw.field("NAME",name);
   outputLocalXml(lvl,xw);
   String txt = toString(lvl);
   if (txt.length() > MAX_VALUE_SIZE) {
      txt = txt.substring(0,MAX_VALUE_SIZE) + "...";
    }
   xw.cdataElement("DESCRIPTION",txt);
   xw.end("VALUE");
}


protected void outputLocalXml(int lvl,IvyXmlWriter xw) { }

protected String toString(int lvl)
{
   return toString();
}


/********************************************************************************/
/*                                                                              */
/*      Null values                                                             */
/*                                                                              */
/********************************************************************************/

private static class JSNull extends NobaseDebugValue {
   
   JSNull() { }
   
   @Override protected void outputLocalXml(int lvl,IvyXmlWriter xw) {
      xw.field("KIND","PRIVITIVE");
    }
   
   @Override public String toString()           { return "null"; }
   
}       // end of inner class JSNull



/********************************************************************************/
/*                                                                              */
/*      Undefined values                                                        */
/*                                                                              */
/********************************************************************************/

private static class JSUndef extends NobaseDebugValue {
   
   JSUndef() { }
   
   @Override protected void outputLocalXml(int lvl,IvyXmlWriter xw) {
      xw.field("KIND","PRIVITIVE");
    }
   
   @Override public String toString()           { return "undefined"; }
     
}       // end of inner class JSUndef




/********************************************************************************/
/*                                                                              */
/*      Boolean values                                                          */
/*                                                                              */
/********************************************************************************/

private static class JSBool extends NobaseDebugValue {
   
   private boolean bool_value;
   
   JSBool(boolean b) {
      bool_value = b;
    }
   
    @Override protected void outputLocalXml(int lvl,IvyXmlWriter xw) {
      xw.field("KIND","PRIVITIVE");
    }
   
   @Override public String toString()           { return Boolean.toString(bool_value); }
    
}       // end of inner class JSBool



/********************************************************************************/
/*                                                                              */
/*      Numeric values                                                          */
/*                                                                              */
/********************************************************************************/

private static class JSNumber extends NobaseDebugValue {
   
   private Number number_value;
   
   JSNumber(Number n) {
      number_value = n;
    }
   
   @Override protected void outputLocalXml(int lvl,IvyXmlWriter xw) {
      xw.field("KIND","PRIVITIVE");
    }
   
   @Override public String toString()           { return number_value.toString(); }
   
}       // end of inner class JSNumber



/********************************************************************************/
/*                                                                              */
/*      String values                                                           */
/*                                                                              */
/********************************************************************************/

private static class JSString extends NobaseDebugValue {
   
   private String string_value;
   
   JSString(String s) {
      string_value = s;
    }
   
   @Override protected void outputLocalXml(int lvl,IvyXmlWriter xw) {
      xw.field("KIND","STRING");
    }
   
   @Override public String toString()           { return string_value; }
   
}       // end of inner class JSString




/********************************************************************************/
/*                                                                              */
/*      Object values                                                           */
/*                                                                              */
/********************************************************************************/

private static class JSObject extends NobaseDebugValue {
   
   private Map<String,NobaseDebugValue> field_map;
   private String class_name;
   private NobaseDebugValue proto_object;
   private NobaseDebugValue prototype_object;
   private NobaseDebugValue constructor_function;
   
   JSObject(JSONObject ref) {
      field_map = new HashMap<String,NobaseDebugValue>();
      class_name = ref.optString("className");
    }
   
   @Override protected void complete(JSONObject ref,NobaseDebugRefMap refmap) {
      proto_object = getRef(ref,"protoObject",refmap);
      prototype_object = getRef(ref,"prototypeObjuect",refmap);
      constructor_function = getRef(ref,"constructorFunction",refmap);
      JSONArray props = ref.optJSONArray("properties");
      if (props != null) {
         for (int i = 0; i < props.length(); ++i) {
            JSONObject prop = props.getJSONObject(i);
            String name = prop.get("name").toString();
            int rid = prop.optInt("ref",-1);
            if (rid >= 0) {
               NobaseDebugValue val = refmap.get(i);
               if (val == null) val = unknown_value;
               field_map.put(name,val);
             }
          }
       }
    }
   
   @Override protected void outputLocalXml(int lvl,IvyXmlWriter xw) {
      xw.field("KIND","OBJECT");
      if (class_name != null) xw.field("TYPE",class_name);
      outputFields(lvl,xw);
    }
   
   protected void outputFields(int lvl,IvyXmlWriter xw) {
      if (lvl < 0) return;
      for (Map.Entry<String,NobaseDebugValue> ent : field_map.entrySet()) {
         NobaseDebugValue val = ent.getValue();
         if (val != null) {
            val.outputXml(ent.getKey(),lvl-1,xw);
          }
       }
    }
   
   @Override public String toString(int lvl) {
      if (lvl < 0) return "<object>";
      StringBuffer buf = new StringBuffer();
      buf.append("{ ");
      int ct = 0;
      for (Map.Entry<String,NobaseDebugValue> ent : field_map.entrySet()) {
         NobaseDebugValue val = ent.getValue();
         if (ct++ > 0) buf.append(",");
         if (val != null) {
            buf.append(ent.getKey());
            buf.append(":");
            buf.append(val.toString(lvl-1));
          }
       } 
      return buf.toString();
    }
   
   
}       // end of inner class JSObject




/********************************************************************************/
/*                                                                              */
/*      Function values                                                         */
/*                                                                              */
/********************************************************************************/

private static class JSFunction extends JSObject {
   
   private String function_name;
   private String inferred_name;
   private String function_source;
   private int script_id;
   private int script_position;
   private int script_line;
   private int script_column;
   
   JSFunction(JSONObject ref) {
      super(ref);
      function_name = ref.optString("name");
      inferred_name = ref.optString("inferredName");
      function_source = ref.optString("source");
      script_id = ref.optInt("scriptId");
      script_position = ref.optInt("position");
      script_line = ref.optInt("line");
      script_column = ref.optInt("column");
    }
   
}       // end of inner class JSFunction



/********************************************************************************/
/*                                                                              */
/*      Frame values                                                            */
/*                                                                              */
/********************************************************************************/

private static class JSFrame extends JSObject {
   
   JSFrame(JSONObject ref) {
      super(ref); 
    }
   
}       // end of inner class JSFrame


/********************************************************************************/
/*                                                                              */
/*      Frame values                                                            */
/*                                                                              */
/********************************************************************************/

private static class JSUnknown extends NobaseDebugValue {
   
   JSUnknown() {
    }
   
  @Override protected void outputLocalXml(int lvl,IvyXmlWriter xw) {
      xw.field("KIND","UNKNNOWN");
    }
   
   @Override public String toString()           { return "<???>"; }
      
}       // end of inner class JSUnknown




}       // end of class NobaseDebugValue




/* end of NobaseDebugValue.java */

