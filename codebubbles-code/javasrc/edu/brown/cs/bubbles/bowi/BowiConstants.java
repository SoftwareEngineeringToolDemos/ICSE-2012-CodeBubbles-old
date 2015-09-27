/********************************************************************************/
/*										*/
/*		BowiConstants.java						*/
/*										*/
/*	Bowi Constants								*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Ian Strickman			*/
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

public class BowiConstants {

   /**
    *
    * Enum to determine what task is being run or stopped
    *
    */
   public enum BowiTaskType {
      SAVE,
      RUN,
      REFRESH,
      BUILD,
      EXPAND_ELLISONS,
      CREATE_BUBBLE,
      LOGIN_TO_CHAT,
      FIND_ALL_REFERENCES,
      GO_TO_DEFINITION,
      TEXT_SEARCH,
      GO_TO_IMPLEMENTATION,
      SEARCH_TREE,
      CTRL_F_SEARCH,
      RENAME
   }

}
