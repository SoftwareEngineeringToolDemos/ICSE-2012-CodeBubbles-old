/********************************************************************************/
/*										*/
/*		BowiTaskManager.java						*/
/*										*/
/*	Task manager								*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Ian Strickman		      */
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

package edu.brown.cs.bubbles.bowi;

import edu.brown.cs.bubbles.bowi.BowiConstants.BowiTaskType;
import edu.brown.cs.bubbles.buda.BudaRoot;

import java.util.HashSet;

class BowiTaskManager {

   private BudaRoot buda_root;
   private int wait_cursor;
   private HashSet<BowiTaskType> is_happening;

   BowiTaskManager(BudaRoot br) {
      buda_root = br;
      is_happening = new HashSet<BowiTaskType>();
      for(BowiTaskType btt : BowiTaskType.values()){
         is_happening.add(btt);
      }
      wait_cursor = 0;
   }

   void startTask(BowiTaskType btt) {
      //if(!is_happening.contains(btt)) {
      switch(btt) {
	case SAVE:
	case EXPAND_ELLISONS:
	case CREATE_BUBBLE:
	case LOGIN_TO_CHAT:	
	case FIND_ALL_REFERENCES:
	case GO_TO_DEFINITION:
	case TEXT_SEARCH:
	case GO_TO_IMPLEMENTATION:
	default:
	   if(wait_cursor == 0) buda_root.startWaitCursor();
	      wait_cursor++;
	      break;
      }
	 //is_happening.put(btt, Boolean.TRUE);
      //}
   }

   void stopTask(BowiTaskType btt) {
      //if(is_happening.get(btt)) {
      switch(btt) {
	case SAVE:
	case EXPAND_ELLISONS:
	case CREATE_BUBBLE:
	case LOGIN_TO_CHAT:	
	case FIND_ALL_REFERENCES:
	case GO_TO_DEFINITION:
	case TEXT_SEARCH:
	case GO_TO_IMPLEMENTATION:
	default:
	   wait_cursor--;
	   if(wait_cursor == 0) buda_root.stopWaitCursor();
	      break;
      }
	 //is_happening.put(btt, Boolean.FALSE);
      //}
   }

}
