/********************************************************************************/
/*										*/
/*		BuenoFactory.java						*/
/*										*/
/*	BUbbles Environment New Objects creator factory 			*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.swing.SwingEventListenerList;

import java.awt.Point;
import java.util.*;



public class BuenoFactory implements BuenoConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private SwingEventListenerList<BuenoInserter> insertion_handlers;
private Map<BuenoMethod,BuenoCreator> creation_map;
private BuenoCreator		cur_creator;
private BuenoMethodCreatorInstance method_creator;
private BuenoClassCreatorInstance class_creator;
private BuenoClassMethodFinder	method_finder;

private static BuenoFactory	the_factory = null;



public static synchronized BuenoFactory getFactory()
{
   if (the_factory == null) {
      the_factory = new BuenoFactory();
    }

   return the_factory;
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BuenoFactory()
{
   insertion_handlers = new SwingEventListenerList<BuenoInserter>(BuenoInserter.class);
   creation_map = new HashMap<BuenoMethod,BuenoCreator>();
   creation_map.put(BuenoMethod.METHOD_SIMPLE,new BuenoCreatorSimple());
   creation_map.put(BuenoMethod.METHOD_ECLIPSE,new BuenoCreatorEclipse());
   creation_map.put(BuenoMethod.METHOD_TEMPLATE,new BuenoCreatorTemplate());
   creation_map.put(BuenoMethod.METHOD_USER,new BuenoCreatorUser());

   method_creator = null;
   class_creator = null;
   method_finder = null;

   BoardProperties bp = BoardProperties.getProperties("Bueno");
   BuenoMethod mthd = bp.getEnum(BUENO_CREATION_METHOD,"METHOD_",BuenoMethod.METHOD_TEMPLATE);
   setCreationMethod(mthd);
}



/********************************************************************************/
/*										*/
/*	Initialization methods							*/
/*										*/
/********************************************************************************/

public static void setup()
{
   new BuenoProjectMakerNew();		// default
   new BuenoProjectMakerSource();
   new BuenoProjectMakerTemplate();
   // new BuenoProjectMakerAnt();

   BudaRoot.registerMenuButton("Create New Project",new CreateListener());
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public void addInsertionHandler(BuenoInserter bi)
{
   insertion_handlers.add(bi);
}



public void removeInsertionHandler(BuenoInserter bi)
{
   insertion_handlers.remove(bi);
}


public void setCreationMethod(BuenoMethod bm)
{
   cur_creator = creation_map.get(bm);
}


BuenoCreator getCreator()
{
   return cur_creator;
}



/********************************************************************************/
/*										*/
/*	Location creation methods						*/
/*										*/
/********************************************************************************/

public BuenoLocation createLocation(BumpLocation l,boolean before)
{
   return new BuenoLocationBump(l,before);
}


public BuenoLocation createLocation(String proj,String pkgcls,String insert,boolean after)
{
   return new BuenoLocationStatic(proj,pkgcls,insert,after);
}



/********************************************************************************/
/*										*/
/*	Dialog setup methods							*/
/*										*/
/********************************************************************************/

public void setMethodDialog(BuenoMethodCreatorInstance bmc)
{
   method_creator = bmc;
}

public void setClassDialog(BuenoClassCreatorInstance bcc)
{
   class_creator = bcc;
}


public void setClassMethodFinder(BuenoClassMethodFinder fdr)
{
   method_finder = fdr;
}



public void createMethodDialog(BudaBubble src,Point loc,BuenoProperties known,
				  BuenoLocation insert,String lbl,
				  BuenoBubbleCreator newer)
{
   if (method_creator != null) {
      if (method_creator.showMethodDialogBubble(src,loc,known,insert,lbl,newer))
	 return;
    }

   BuenoMethodDialog md = new BuenoMethodDialog(src,loc,known,insert,newer);
   if (lbl != null) md.setLabel(lbl);
   md.showDialog();
}




public void createClassDialog(BudaBubble src,Point loc,BuenoType type,
      BuenoProperties known,
      BuenoLocation insert,String lbl,BuenoBubbleCreator newer)
{
   if (class_creator != null) {
      if (class_creator.showClassDialogBubble(src,loc,type,known,insert,lbl,newer))
	 return;
    }

   BuenoClassDialog cd = new BuenoClassDialog(src,loc,type,known,insert,newer);
   if (lbl != null) cd.setLabel(lbl);
   cd.showDialog();
}



public boolean useSeparateTypeButtons()
{
   if (class_creator == null) return false;

   return class_creator.useSeparateTypeButtons();
}


List<BumpLocation> findClassMethods(String name)
{
   if (method_finder == null) return  null;

   return method_finder.findClassMethods(name);
}




/********************************************************************************/
/*										*/
/*	Methods for doing the creation						*/
/*										*/
/********************************************************************************/

public void createNew(BuenoType what,BuenoLocation where,BuenoProperties props)
{
   if (props == null) props = new BuenoProperties();

   String prj = props.getStringProperty(BuenoKey.KEY_PROJECT);
   if (prj == null && where.getProject() != null) {
      props.put(BuenoKey.KEY_PROJECT,where.getProject());
    }
   String pkg = props.getStringProperty(BuenoKey.KEY_PACKAGE);
   if (pkg == null && where.getPackage() != null) {
      props.put(BuenoKey.KEY_PACKAGE,where.getPackage());
    }
   String fil = props.getStringProperty(BuenoKey.KEY_FILE);
   if (fil == null && where.getFile() != null) {
      switch (what) {
	 case NEW_PACKAGE :
	 case NEW_TYPE :
	 case NEW_CLASS :
	 case NEW_INTERFACE :
	 case NEW_ENUM :
	 case NEW_ANNOTATION :
	 case NEW_MODULE :
	    break;
	 default :
	    props.put(BuenoKey.KEY_FILE,where.getFile().getPath());
	    props.put(BuenoKey.KEY_FILETAIL,where.getFile().getName());
	    break;
       }
    }

   switch (what) {
      case NEW_PACKAGE :
	 cur_creator.createPackage(where,props);
	 break;
      case NEW_MODULE :
	 cur_creator.createModule(where,props);
	 break;
      case NEW_TYPE :
      case NEW_CLASS :
      case NEW_INTERFACE :
      case NEW_ENUM :
      case NEW_ANNOTATION :
	 BuenoClassSetup bcs = new BuenoClassSetup(props);
	 bcs.extendProperties();
	 cur_creator.createType(what,where,props);
	 break;
      case NEW_INNER_TYPE :
      case NEW_INNER_CLASS :
      case NEW_INNER_INTERFACE :
      case NEW_INNER_ENUM :
	 cur_creator.createInnerType(what,where,props);
	 break;
      case NEW_CONSTRUCTOR :
      case NEW_METHOD :
      case NEW_GETTER :
      case NEW_SETTER :
      case NEW_GETTER_SETTER :
	 cur_creator.createMethod(what,where,props);
	 break;
      case NEW_FIELD :
	 cur_creator.createField(where,props);
	 break;
      case NEW_MARQUIS_COMMENT :
      case NEW_BLOCK_COMMENT :
      case NEW_JAVADOC_COMMENT :
	 cur_creator.createComment(what,where,props);
	 break;
    }
}


/********************************************************************************/
/*										*/
/*	Insertion methods							*/
/*										*/
/********************************************************************************/

boolean insertText(BuenoLocation loc,String text)
{
   if (text == null || text.length() == 0) return false;

   for (BuenoInserter bi : insertion_handlers) {
      if (bi.insertText(loc,text)) return true;
    }

   return false;
}


/********************************************************************************/
/*										*/
/*	Methods for doing create project					*/
/*										*/
/********************************************************************************/

public BudaBubble getCreateProjectBubble()
{
   switch (BoardSetup.getSetup().getLanguage()) {
      case JAVA :
	 BuenoProjectCreator bpc = new BuenoProjectCreator();
	 return bpc.createProjectCreationBubble();
      case PYTHON :
	 return BuenoPythonProject.createNewPythonProjectBubble();
      case JS :
	 return BuenoJsProject.createNewJsProjectBubble();
      case REBUS :
	 return null;
      default :
	 return null;
    }
}


private static class CreateListener implements BudaConstants.ButtonListener {

   public void buttonActivated(BudaBubbleArea bba,String id,Point pt) {
      BuenoFactory bf = getFactory();
      BudaBubble bb = bf.getCreateProjectBubble();
      if (bb == null) return;
      bba.addBubble(bb,null,pt,BudaConstants.PLACEMENT_LOGICAL|
		       BudaConstants.PLACEMENT_MOVETO |
		       BudaConstants.PLACEMENT_USER);
    }

}	// end of inner class CreateListener



}	// end of class BuenoFactory




/* end of BuenoFactory.java */
