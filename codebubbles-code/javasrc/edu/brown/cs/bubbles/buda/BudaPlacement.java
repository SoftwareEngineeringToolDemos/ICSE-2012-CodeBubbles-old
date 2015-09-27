/********************************************************************************/
/*										*/
/*		BudaPlacement.java						*/
/*										*/
/*	BUblles Display Area common code for locating bubbles			*/
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

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.buda;



import javax.swing.Timer;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;



/**
 *	This class holds a variety of enumeration types and constants that define
 *	the basic properties of the bubble display.  It can be changed and then
 *	the system recompiled.	Most of these constants should be made into
 *	properties and read from a property file so they can be changed by the
 *	user without recompiling.
 *
 **/

class BudaPlacement implements BudaConstants, BudaConstants.BubbleViewCallback {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaBubbleArea			bubble_area;
private BudaBubble			last_placement;
private Map<BudaBubble,List<BudaBubble>> related_bubbles;
private BudaBubble			last_focus;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaPlacement(BudaBubbleArea bba)
{
   bubble_area = bba;
   last_placement = null;
   related_bubbles = new HashMap<BudaBubble,List<BudaBubble>>();
   last_focus = null;
   BudaRoot.addBubbleViewCallback(this);
}



/********************************************************************************/
/*										*/
/*	Actually placement routines						*/
/*										*/
/********************************************************************************/

void placeBubble(BudaBubble bbl,Component rcom,Point relpt,int place,BudaBubblePosition pos)
{
   BudaBubble rel = null;
   BudaBubble grpb = null;
   if (rcom != null) rel = BudaRoot.findBudaBubble(rcom);

   if ((place & PLACEMENT_EXPLICIT) == 0) {
      int dflt = 0;
      String pnm = BUDA_PROPERTIES.getProperty("Buda.placement.default");
      if (pnm.contains("USER")) dflt |= PLACEMENT_USER;
      if (pnm.contains("GROUP")) dflt |= PLACEMENT_ADGROUP;
      if (pnm.contains("WINDOW")) dflt |= PLACEMENT_ADJACENT;
      if (dflt == 0) dflt = PLACEMENT_USER | PLACEMENT_ADJACENT;
      if (dflt == PLACEMENT_USER) dflt = PLACEMENT_USER | PLACEMENT_ADJACENT;
      
      if ((dflt & PLACEMENT_USER) != 0) {
	 switch (pos) {
	    case DIALOG :
	    case DOCKED :
	    case FIXED :
	    case HOVER :
	    case FLOAT :
	    case STATIC :
	       dflt &= ~PLACEMENT_USER;
	       break;
	    default :
	       break;
	  }
       }
 
      if ((place & (PLACEMENT_ADJACENT | PLACEMENT_ADGROUP)) == 0) place |= dflt;
      else if ((dflt & PLACEMENT_USER) != 0) place |= PLACEMENT_USER;
    }

   Rectangle r = null;
   if (rel != null) {
      r = BudaRoot.findBudaLocation(rel);
      grpb = rel;
    }

   if (r == null) {
      r = new Rectangle();
      if (relpt != null) {
	 r.setLocation(relpt);
       }
      else if (last_focus != null) {
	 r = BudaRoot.findBudaLocation(last_focus);
	 if (r != null)  grpb = last_focus;
       }
      if (r == null) {
	 if (last_placement != null) {
	    r = BudaRoot.findBudaLocation(last_placement);
	    grpb = last_placement;
	  }
       }
      if (r == null) {
	 r = bubble_area.getViewport();
	 // where in the viewport should we put the bubble: center, left, right
	 // also, adjust for any fixed bubbles
      }
      if (r == null) {
	 r = new Rectangle(300,100);
       }
    }

   Rectangle r0 = new Rectangle(r);

   if ((place & PLACEMENT_ADGROUP) != 0) {
      expandForGroup(r,grpb);
    }

   Rectangle r1 = null;
   if (rel != null) {
      synchronized (related_bubbles) {
	 List<BudaBubble> ls = related_bubbles.get(rel);
	 if (ls != null && !ls.isEmpty()) {
	    BudaBubble bbx = ls.get(ls.size()-1);
	    r1 = BudaRoot.findBudaLocation(bbx);
	  }
       }
    }

   int delta;
   if ((place & PLACEMENT_GROUPED) != 0) delta = BUBBLE_CREATION_NEAR_SPACE;
   else delta = BUBBLE_CREATION_SPACE;

   if ((place & PLACEMENT_LOGICAL) != 0) {
      Rectangle r2 = bubble_area.getViewport();
      if (r.x - delta - bbl.getWidth() < r2.x) {
	 if (r.x + r.width + delta + bbl.getWidth() < r2.x + r2.width) {
	    place &= ~PLACEMENT_LEFT;
	    place |= PLACEMENT_RIGHT;
	  }
       }
      if (r.x + r.width + delta + bbl.getWidth() > r2.x + r2.width) {
	 if (r.x - delta - bbl.getWidth() > r2.x) {
	    place &= ~PLACEMENT_RIGHT;
	    place |= PLACEMENT_LEFT;
	  }
       }
      if (r.y - delta - bbl.getHeight() < r2.y) {
	 if (r.y + r.height + delta + bbl.getHeight() < r2.y + r2.height) {
	    place &= ~PLACEMENT_ABOVE;
	    place |= PLACEMENT_BELOW;
	  }
       }
      if (r.y + r.height + delta + bbl.getHeight() > r2.y + r2.height) {
	 if (r.y - delta - bbl.getHeight() > r2.y) {
	    place &= ~PLACEMENT_BELOW;
	    place |= PLACEMENT_ABOVE;
	  }
       }
    }

   Dimension sz = bbl.getSize();
   int x0;
   int y0;

   if (rel == null) {
      x0 = r.x;
      y0 = r.y;
    }
   else if ((place & PLACEMENT_RIGHT) != 0) {
      x0 = r.x + r.width + delta;
      y0 = r0.y;
      if (relpt != null) y0 = relpt.y;
    }
   else if ((place & PLACEMENT_LEFT) != 0) {
      x0 = r.x - sz.width - delta;
      y0 = r0.y;
      if (relpt != null) y0 = relpt.y;
    }
   else if ((place & PLACEMENT_BELOW) != 0) {
      x0 = r0.x;
      y0 = r.y + r.height + delta;
      if (relpt != null) x0 = relpt.x;
    }
   else if ((place & PLACEMENT_ABOVE) != 0) {
      x0 = r0.x;
      y0 = r.y - sz.height - delta;
      if (relpt != null) x0 = relpt.x;
    }
   else {
      x0 = r.x + r.width + delta;
      y0 = r0.y;
      if (relpt != null) y0 = relpt.y;
    }

   if (r1 != null) {
      if (x0 == r1.x && y0 == r1.y) {
	 if ((place & (PLACEMENT_RIGHT|PLACEMENT_LEFT)) != 0) y0 += delta;
	 else if ((place & (PLACEMENT_ABOVE|PLACEMENT_BELOW)) != 0) x0 += delta;
	 else {
	    x0 += delta;
	    y0 += delta;
	  }
       }
    }

   if (rel != null) {
      synchronized (related_bubbles) {
	 List<BudaBubble> ls = related_bubbles.get(rel);
	 if (ls == null) {
	    ls = new ArrayList<BudaBubble>();
	    related_bubbles.put(rel,ls);
	  }
	 ls.add(bbl);
       }
    }


   UserUpdater uu = null;
   if ((place & PLACEMENT_USER) != 0) {
      uu = new UserUpdater(bbl);
      pos = BudaBubblePosition.USERPOS;
      bbl.setUserPos(true);
    }

   bubble_area.addBubble(bbl,pos,x0,y0);

   if ((place & PLACEMENT_MOVETO) != 0) {
      bubble_area.scrollBubbleVisible(bbl);
    }

   if ((place & PLACEMENT_NEW) != 0) {
      bbl.markBubbleAsNew();
    }

   if (uu != null) uu.start();
}




/********************************************************************************/
/*										*/
/*	Methods for dealing with groups 					*/
/*										*/
/********************************************************************************/

private static final int	GROUP_SPACE = 100;
private static final int	GROUP_LEEWAY = 40;


private void expandForGroup(Rectangle r,BudaBubble grpb)
{
   if (grpb == null) return;

   Set<BudaBubble> used = new HashSet<BudaBubble>();
   used.add(grpb);

   BudaBubbleGroup grp = grpb.getGroup();
   if (grp == null) return;

   boolean chng = true;
   while (chng) {
      chng = false;
      Rectangle rhor = new Rectangle(r.x - GROUP_SPACE,r.y + GROUP_LEEWAY,
					r.width + 2*GROUP_SPACE,r.height - 2*GROUP_LEEWAY);
      Rectangle rver = new Rectangle(r.x + GROUP_LEEWAY,r.y - GROUP_SPACE,
					r.width - 2*GROUP_LEEWAY,r.height + 2*GROUP_SPACE);

      for (BudaBubble gb : grp.getBubbles()) {
	 if (used.contains(gb)) continue;
	 Rectangle r1 = BudaRoot.findBudaLocation(gb);
	 if (r1 == null) continue;
	 if (r1.intersects(rhor)) {
	    int lx = Math.min(r.x,r1.x);
	    int rx = Math.max(r.x + r.width, r1.x + r1.width);
	    r.x = lx;
	    r.width = rx-lx;
	    chng = true;
	    used.add(gb);
	    break;
	  }
	 else if (r1.intersects(rver)) {
	    int ty = Math.min(r.y,r1.y);
	    int by = Math.max(r.y + r.height, r1.y + r1.height);
	    r.y = ty;
	    r.height = by-ty;
	    chng = true;
	    used.add(gb);
	    break;
	  }
       }
    }

   // if bubble is supposed to be grouped, then we should limit the expansion
   // to the y space of the bubble
}



/********************************************************************************/
/*										*/
/*	Bubble area callback methods						*/
/*										*/
/********************************************************************************/

@Override public void focusChanged(BudaBubble bb,boolean set)
{
   if (BudaRoot.findBudaBubbleArea(bb) == bubble_area && set)
      last_focus = bb;
}



@Override public void bubbleAdded(BudaBubble bb)		{ }
@Override public boolean bubbleActionDone(BudaBubble bb)	{ return false; }

@Override public void bubbleRemoved(BudaBubble bb)
{
   if (BudaRoot.findBudaBubbleArea(bb) == bubble_area) {
      if (bb == last_focus) last_focus = null;
      synchronized (related_bubbles) {
	 related_bubbles.remove(bb);
	 for (List<BudaBubble> ls : related_bubbles.values()) {
	    ls.remove(bb);
	  }
       }
    }
}

@Override public void workingSetAdded(BudaWorkingSet ws)	{ }
@Override public void workingSetRemoved(BudaWorkingSet ws)	{ }

@Override public void doneConfiguration()			{ }
@Override public void copyFromTo(BudaBubble f,BudaBubble t)	{ }




/********************************************************************************/
/*										*/
/*	Routine to handle user bubble positions 				*/
/*										*/
/********************************************************************************/

private static final int USER_POSITION_UPDATE_TIME = 2000;
private static final int USER_POSITION_RESTART_TIME = 500;


private static class UserUpdater implements ActionListener, ComponentListener {

   private BudaBubble for_bubble;
   private Timer swing_timer;
   private int move_count;

   UserUpdater(BudaBubble bb) {
      for_bubble = bb;
      move_count = 0;
      int t0 = BUDA_PROPERTIES.getInt("Buda.placement.user.initial",USER_POSITION_UPDATE_TIME);
      swing_timer = new Timer(t0,this);
      // System.err.println("UPDATE: START " + t0);
      swing_timer.setRepeats(false);
      bb.addComponentListener(this);
    }

   void start() {
       // swing_timer.start();
    }

   @Override public void actionPerformed(ActionEvent e) {
      // System.err.println("UPDATE: DONE");
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(for_bubble);
      if (bba != null && bba.isMoving(for_bubble)) {
	 swing_timer.restart();
	 return;
       }
      for_bubble.removeComponentListener(this);
      if (bba != null) bba.setBubbleFloating(for_bubble,false);
    }

   @Override public void componentHidden(ComponentEvent e) {
      // System.err.println("UPDATE: HIDE");
      for_bubble.removeComponentListener(this);
      swing_timer.stop();
    }

   @Override public void componentMoved(ComponentEvent e) {
      // System.err.println("UPDATE: MOVE");
      if (move_count++ >= 4) {
	 int t1 = BUDA_PROPERTIES.getInt("Buda.placement.user.moved",USER_POSITION_RESTART_TIME);
	 // System.err.println("UPDATE: RESTART " + t1);
	 swing_timer.setInitialDelay(t1);
       }
      swing_timer.restart();
    }

   @Override public void componentResized(ComponentEvent e) {
      // System.err.println("UPDATE: RESIZE");
      swing_timer.restart();
    }

   @Override public void componentShown(ComponentEvent e) {
      // System.err.println("UPDATE: SHOW");
    }

}	// end of inner class UserUpdater



}	// end of class BudaPlacement




/* end of BudaPlacement.java */
