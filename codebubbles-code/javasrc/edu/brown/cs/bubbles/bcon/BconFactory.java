/********************************************************************************/
/*										*/
/*		BconFactory.java						*/
/*										*/
/*	Bubbles Environment Context Viewer factory and setup class		*/
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



package edu.brown.cs.bubbles.bcon;

import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.buda.*;

import java.awt.Component;
import java.awt.Point;
import java.io.File;
import java.util.HashMap;
import java.util.Map;


/**
 *	This class provides the entries for setting up and providing access to
 *	the various context view bubbles.
 **/

public class BconFactory implements BconConstants, BudaConstants.ButtonListener
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<BudaBubbleArea,BconOverviewPanel> current_panel;


private static BconFactory	the_factory = null;




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

/**
 *	This routine is called automatically at startup to initialize the module.
 **/

public static void setup()
{
   BudaRoot.addBubbleConfigurator("BCON",new BconConfigurator());
   BudaRoot.registerMenuButton(BCON_BUTTON,getFactory());
   BconRepository br = new BconRepository();
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_CODE,br);
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_EXPLORER,br);
}



/**
 *	Return the singleton instance of the context viewer factory.
 **/

public static synchronized BconFactory getFactory()
{
   if (the_factory == null) {
      the_factory = new BconFactory();
    }
   return the_factory;
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BconFactory()
{
   current_panel = new HashMap<BudaBubbleArea,BconOverviewPanel>();
}




/********************************************************************************/
/*										*/
/*	Factory methods 							*/
/*										*/
/********************************************************************************/

/**
 *	Create an overview bubble to show the files that are currently active
 *	and their regions and bubbles.
 **/

public BudaBubble createOverviewBubble(Component source,Point pt)
{
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(source);
   if (bba == null) return null;

   BconOverviewPanel pnl = new BconOverviewPanel(bba,pt);
   current_panel.put(bba,pnl);

   return new BconBubble(pnl);
}



/**
 *	Create a class panel bubble showing all class information
 **/

public BudaBubble createClassBubble(Component source,String proj,File f,String cls,boolean inner)
{
   BconClassPanel pnl = new BconClassPanel(proj,f,cls,inner);

   if (!pnl.isValid()) return null;

   return new BconBubble(pnl);
}



/**
 *	Create a package panel bubble showing package relationships
 **/

public BudaBubble createPackageBubble(Component source,String proj,String pkg)
{
   BconPackagePanel pnl = new BconPackagePanel(proj,pkg);

   return new BconBubble(pnl);
}



/********************************************************************************/
/*										*/
/*	Menu button handling							*/
/*										*/
/********************************************************************************/

@Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt)
{
   BudaBubble bb = null;

   if (id.equals(BCON_BUTTON)) {
      bb = createOverviewBubble(bba,pt);
    }

   if (bb != null && bba != null) {
      bba.addBubble(bb,null,pt,BudaConstants.PLACEMENT_LOGICAL|BudaConstants.PLACEMENT_MOVETO|
		       BudaConstants.PLACEMENT_NEW|BudaConstants.PLACEMENT_USER);
    }
}




}	// end of class BconFactory



/* end of BconFactory.java */
