/********************************************************************************/
/*                                                                              */
/*              NobaseDebugResponse.java                                        */
/*                                                                              */
/*      Response from a command                                                 */
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

import org.json.JSONArray;
import org.json.JSONObject;


class NobaseDebugResponse implements NobaseConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private NobaseDebugRefMap               ref_map;
private JSONObject                      response_object;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

NobaseDebugResponse(JSONObject resp)
{
   response_object = resp;
   ref_map = null;
   if (!isSuccess()) {
      NobaseMain.logE("Bad response: " + getError() + " : " + resp);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

boolean isSuccess() 
{
   return response_object.optBoolean("success");
}


String getError()
{
   if (!response_object.optBoolean("success")) 
      return response_object.optString("message");
   
   return null;
}

JSONObject getBody()
{
   return response_object.optJSONObject("body");
}


JSONArray getBodyArray()
{
   return response_object.optJSONArray("body");
}



NobaseDebugRefMap getRefMap()
{
   if (ref_map == null) {
      JSONArray jrefs = response_object.optJSONArray("refs");
      ref_map = NobaseDebugValue.createRefs(jrefs);
    }
   return ref_map;
}



}       // end of class NobaseDebugResponse




/* end of NobaseDebugResponse.java */

