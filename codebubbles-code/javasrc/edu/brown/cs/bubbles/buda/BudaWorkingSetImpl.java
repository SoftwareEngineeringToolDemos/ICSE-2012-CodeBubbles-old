/********************************************************************************/
/*										*/
/*		BudaWorkingSetImpl.java 					*/
/*										*/
/*	BUblles Display Area working set implementation 			*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.buda;

import edu.brown.cs.bubbles.board.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;



public class BudaWorkingSetImpl implements BudaConstants, BudaConstants.BudaWorkingSet, BoardConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaBubbleArea bubble_area;
private String	set_label;
private Rectangle set_region;
private int	preferred_y;
private Color	border_color;
private Color	top_color;
private Color	bottom_color;
private Color	text_color;
private boolean being_changed;
private long	create_time;
private boolean is_shared;
//private File	my_pdf;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaWorkingSetImpl(BudaBubbleArea bba,String lbl,Rectangle rgn,int y)
{
   bubble_area = bba;
   set_label = lbl;
   set_region = new Rectangle(rgn);
   preferred_y = y;
   being_changed = false;
   create_time = System.currentTimeMillis();
   is_shared = false;
   //my_pdf = null;

   setColor(BudaRoot.getRandomColor(1.0));
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getLabel()			{ return set_label; }

void setLabel(String s)
{
   set_label = s;
}



@Override public Rectangle getRegion()
{
   Dimension r = bubble_area.getSize();
   set_region.height = r.height;

   return new Rectangle(set_region);
}


@Override public BudaBubbleArea getBubbleArea()
{
   return bubble_area;
}


void setRegion(Rectangle r)
{
   set_region = new Rectangle(r);
   BudaRoot br = BudaRoot.findBudaRoot(bubble_area);
   if (br != null) br.repaint();
}



Color getTopColor()			{ return top_color; }
Color getBottomColor()			{ return bottom_color; }
Color getBorderColor()
{
   if (being_changed) return Color.BLACK;
   return border_color;
}


Color getTextColor()			{ return text_color; }


void setColor(Color c)
{
   if (c == null) c = Color.GREEN;

   border_color = c;
   top_color = BudaRoot.getPaleColor(c,0.5);
   bottom_color = BudaRoot.getPaleColor(top_color);

   int tot = c.getRed() + c.getGreen() + c.getBlue();
   if (tot > 128*3) text_color = Color.BLACK;
   else text_color = Color.WHITE;
}



void setBeingChanged(boolean fg)	{ being_changed = fg; }
boolean isBeingChanged()		{ return being_changed; }

void setCreateTime(long when)		{ create_time = when; }

boolean isShared()			{ return is_shared; }
void setShared(boolean fg)		{ is_shared = fg; }



/********************************************************************************/
/*										*/
/*	Bubble methods								*/
/*										*/
/********************************************************************************/

void removeBubbles()
{
   for (BudaBubble bb : bubble_area.getBubblesInRegion(set_region)) {
      if (!bb.isFloating()) bubble_area.userRemoveBubble(bb);
    }
}



/********************************************************************************/
/*										*/
/*	Save methods								*/
/*										*/
/********************************************************************************/

@Override public File getDescription() throws IOException
{
   File f = File.createTempFile("BudaWorkingSet",".xml");
   BudaXmlWriter xw = new BudaXmlWriter(f);
   createTask(xw);
   xw.close();

   return f;
}




void saveAs(File result) throws IOException
{
   BudaXmlWriter xw = new BudaXmlWriter(result);

   createTask(xw);

   xw.close();
}



BudaTask createTask()
{
   if (set_label == null) return null;

   /*******************
   File f1 = BoardSetup.getPropertyBase();
   File dir = new File(f1,"TaskImages");
   if (!dir.exists()) dir.mkdir();
   if (my_pdf != null && my_pdf.exists()) my_pdf.delete();
   try {
      my_pdf = File.createTempFile(set_label, ".gif", dir);
      BudaRoot.findBudaRoot(bubble_area).exportAsPdf(my_pdf, set_region);
    }
   catch (IOException e) { System.out.println("couldn't make the temp file");}
   catch (Exception e) { System.out.println("couldn't pdf!"); }
   ***********8****/

   BudaXmlWriter xw = new BudaXmlWriter();
   createTask(xw);

   return new BudaTask(set_label,xw.toString());
}



void sendMail(String to)
{
   try {
      File f = File.createTempFile("BudaWorkingSet",".xml");
      BudaXmlWriter xw = new BudaXmlWriter(f);
      createTask(xw);
      xw.close();
      BoardUpload bup = new BoardUpload(f);

      String msg = "Here is a working set to share:\n\n" + bup.getFileURL() + "\n";
      BoardMailMessage bmm = BoardMail.createMessage(to);
      bmm.setSubject("Bubbles working set to share");
      bmm.addBodyText(msg);
      bmm.send();
    }
   catch (IOException e) {
      BoardLog.logE("BUDA","Problem emailing working set",e);
    }
}




void sendPDF(String to)
{
   try {
      File f = File.createTempFile("BudaWorkingSet",".pdf");
      BudaRoot br = BudaRoot.findBudaRoot(bubble_area);
      br.exportAsPdf(f,getRegion());

      BoardUpload bup = new BoardUpload(f);

      String msg = "Here is the working set image:\n\n" + bup.getFileURL() + "\n";
      BoardMailMessage bmm = BoardMail.createMessage(to);
      bmm.setSubject("Bubbles working set image");
      bmm.addBodyText(msg);
      bmm.send();
    }
   catch (Exception e) {
      BoardLog.logE("BUDA","Problem emailing working set image",e);
    }
}




void createTask(BudaXmlWriter xw)
{
   xw.begin("TASK");
   xw.field("TIME",System.currentTimeMillis());
   if (set_label != null) xw.field("NAME",set_label);
   else xw.field("NAME","Unnamed Task");
   //xw.field("PDF", my_pdf.getName());

   outputXml(xw);
   
   BudaBubbleScaler bsc = bubble_area.getUnscaler();
   Set<BudaBubble> bbls = new HashSet<BudaBubble>(bubble_area.getBubblesInRegion(set_region));
   Set<BudaBubbleGroup> grps = new HashSet<BudaBubbleGroup>();

   xw.begin("BUBBLES");
   for (BudaBubble bb : bbls) {
      if (bb.isFloating()) continue;
      bb.outputBubbleXml(xw,bsc);
      BudaBubbleGroup bbg = bb.getGroup();
      if (bbg != null && bbg.getTitle() != null) grps.add(bbg);
      //TODO: only output movable bubbles
    }
   xw.end("BUBBLES");

   xw.begin("GROUPS");
   for (BudaBubbleGroup bg : grps) {
      bg.outputXml(xw);
    }
   xw.end("GROUPS");

   xw.begin("LINKS");
   for (BudaBubbleLink bl : bubble_area.getLinks(bbls)) {
      bl.outputXml(xw);
    }
   xw.end("LINKS");

   xw.end("TASK");
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(BudaXmlWriter xw)
{
   xw.begin("WORKINGSET");
   xw.field("TOPCOLOR",top_color);
   xw.field("BOTTOMCOLOR",bottom_color);
   xw.field("BORDERCOLOR",border_color);
   xw.field("YLOC",preferred_y);
   xw.field("CREATE",create_time);
   xw.element("REGION",set_region);
   if (set_label != null) xw.element("NAME",set_label);
   xw.end("WORKINGSET");
}




}	// end of class BudaWorkingSetImpl




/* end of BudaWorkingSetImpl.java */
