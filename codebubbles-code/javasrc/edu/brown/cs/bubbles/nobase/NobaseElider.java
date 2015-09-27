/********************************************************************************/
/*										*/
/*		NobaseElider.java						*/
/*										*/
/*	description of class							*/
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

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.*;

class NobaseElider implements NobaseConstants, NobaseAst
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<ElidePriority> elide_pdata;
private List<ElideRegion> elide_rdata;

private static Prioritizer down_priority;

private static final double	UP_DEFAULT_SCALE = 0.8;
private static final double	DOWN_DEFAULT_SCALE = 0.8;
private static final double	DOWN_DEFAULT_COUNT = 0.95;
private static final double	DOWN_DEFAULT_ITEM  = 0.99;
private static final double	SWITCH_BLOCK_SCALE = 0.90;

static {
   Prioritizer dflt = new DefaultPrioritizer(DOWN_DEFAULT_SCALE,DOWN_DEFAULT_COUNT,DOWN_DEFAULT_ITEM);
   Prioritizer same = new DefaultPrioritizer(1.0,1.0,1.0);
   NodePrioritizer p1 = new NodePrioritizer(dflt);
   p1.addPriority(NobaseAst.IfStatement.class,same);
   StructuralPrioritizer p0 = new StructuralPrioritizer(dflt);
   p0.addPriority(NobaseAst.IfStatement.class,2,p1);
   down_priority = p0;
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseElider()
{
   elide_pdata = new ArrayList<ElidePriority>();
   elide_rdata = new ArrayList<ElideRegion>();
}



/********************************************************************************/
/*										*/
/*	Methods for maintaining elision information				*/
/*										*/
/********************************************************************************/


void clearElideData()
{
   elide_pdata.clear();
   elide_rdata.clear();
}



void addElidePriority(int soff,int eoff,double pri)
{
   ElidePriority ed = new ElidePriority(soff,eoff,pri);
   elide_pdata.add(ed);
}


void addElideRegion(int soff,int eoff)
{
   ElideRegion er = new ElideRegion(soff,eoff);
   elide_rdata.add(er);
}



void noteEdit(int soff,int len,int rlen)
{
   for (Iterator<ElidePriority> it = elide_pdata.iterator(); it.hasNext(); ) {
      ElidePriority ed = it.next();
      if (!ed.noteEdit(soff,len,rlen)) it.remove();
    }

   for (Iterator<ElideRegion> it = elide_rdata.iterator(); it.hasNext(); ) {
      ElideRegion ed = it.next();
      if (!ed.noteEdit(soff,len,rlen)) it.remove();
    }
}



/********************************************************************************/
/*										*/
/*	Elision computaton methods						*/
/*										*/
/********************************************************************************/

boolean computeElision(ISemanticData isd,IvyXmlWriter xw)
{
   NobaseAstNode root = isd.getRootNode();
   NobaseFile file = isd.getFileData();

   if (root == null || elide_rdata.isEmpty()) return false;

   ElidePass1 ep1 = null;
   if (!elide_pdata.isEmpty()) {
      ep1 = new ElidePass1(file);
      try {
	 root.accept(ep1);
      }
      catch (Exception e) {
	 NobaseMain.logE("Problem with elision pass 1",e);
       }
    }

   ElidePass2 ep2 = new ElidePass2(ep1,file,xw);
   try {
      root.accept(ep2);
   }
   catch (Exception e) {
      NobaseMain.logE("Problem with elision pass 2",e);
    }

   return true;
}



/********************************************************************************/
/*										*/
/*	Access methods for elision information					*/
/*										*/
/********************************************************************************/

private double getElidePriority(NobaseFile nf,NobaseAstNode n)
{
   for (ElidePriority ep : elide_pdata) {
      if (ep.useForPriority(nf,n)) return ep.getPriority();
    }

   return 0;
}



private boolean isActiveRegion(int soff,int eoff)
{
   for (ElideRegion er : elide_rdata) {
      if (er.overlaps(soff,eoff)) return true;
    }

   return false;
}



private boolean isRootRegion(int soff,int eoff)
{
   for (ElideRegion er : elide_rdata) {
      if (er.contains(soff,eoff)) return true;
    }

   return false;
}




private double scaleUp(NobaseAstNode n)
{
   return UP_DEFAULT_SCALE;
}




/********************************************************************************/
/*										*/
/*	Main priority function							*/
/*										*/
/********************************************************************************/

private double computePriority(double parprior,NobaseAstNode base,double pass1prior)
{
   double p = down_priority.getPriority(parprior,base);

   if (pass1prior > p) p = pass1prior;

   return p;
}




/********************************************************************************/
/*										*/
/*	Formatting type function						*/
/*										*/
/********************************************************************************/

private String getFormatType(NobaseAstNode n)
{
   String typ = null;
   boolean isdef = true;

   if (n == null) return null;

   NobaseSymbol nsp = n.getDefinition();
   if (nsp == null) {
      nsp = n.getReference();
      if (nsp != null) isdef = false;
    }
   if (nsp == null) {
      if (n instanceof Identifier) {
	 nsp = n.getParent().getDefinition();
       }
    }
   if (nsp != null) {
      switch (nsp.getNameType()) {
	 case MODULE :
	    typ = "MODULE";
	    break;
	 case FUNCTION :
	    typ = (isdef ? "METHODDECL" : "CALL");
	    break;
	 case LOCAL :
	    typ = (isdef ? "VARDECL" : null);
	    break;
	 case VARIABLE :
	    typ = (isdef ? "VARDECL" : "FIELD");
	    break;
       }
    }

   return typ;
}



private String getNodeType(NobaseAstNode n)
{
   String typ = null;

   if (n instanceof Expression) {
      typ = "EXPR";
    }
   else if (n instanceof FunctionConstructor || n instanceof FunctionDeclaration) {
      typ = "FUNCTION";
    }
   else if (n instanceof FileModule || n instanceof PlainModule) {
      typ = "MODULE";
    }
   else if (n instanceof Statement) {
      typ = "STMT";
    }

   return typ;
}



/********************************************************************************/
/*										*/
/*	Tree walk for setting initial priorities				*/
/*										*/
/********************************************************************************/

private class ElidePass1 extends NobaseAstVisitor {
   
   private Map<NobaseAstNode,Double> result_value;
   private int inside_count;
   private NobaseFile for_file;
   
   ElidePass1(NobaseFile nf) {
      result_value = new HashMap<NobaseAstNode,Double>();
      inside_count = 0;
      for_file = nf;
    }
   
   @Override public void preVisit(NobaseAstNode n) {
      if (inside_count > 0) {
         ++inside_count;
       }
      else { 
         double p = getElidePriority(for_file,n);
         if (p != 0) {
            result_value.put(n,p);
            ++inside_count;
          }
       }
    }
   
   public void postVisit(NobaseAstNode n) {
      if (inside_count > 0) {
	 --inside_count;
	 return;
       }
      double p = 0;
      for (int i = 0; i < n.getNumChildren(); ++i) {
         NobaseAstNode cn = n.getChild(i);
         p = merge(p,cn);
       }
      if (p > 0) {
	 p *= scaleUp(n);
	 result_value.put(n,p);
       }
    }
   
   public boolean visit(FunctionDeclaration n) {
      return isActiveRegion(n.getStartPosition(for_file),n.getEndPosition(for_file));
    }
   
   public boolean visit(Block n) {
      return isActiveRegion(n.getStartPosition(for_file),n.getEndPosition(for_file));
    }
   
   public boolean visit(Declaration n) {
      return isActiveRegion(n.getStartPosition(for_file),n.getEndPosition(for_file));
    }
   
   double getPriority(NobaseAstNode n) {
      Double dv = result_value.get(n);
      if (dv != null) return dv.doubleValue();
      return 0;
    }
   
   private double merge(double p,NobaseAstNode n) {
      Double dv = result_value.get(n);
      if (dv == null) return p;
      double q = 1 - (1-p)*(1-dv.doubleValue());
      return q;
    }
   
}	// end of innerclass ElidePass1




/********************************************************************************/
/*										*/
/*	Tree walk for setting final priorities					*/
/*										*/
/********************************************************************************/

private class ElidePass2 extends NobaseAstVisitor {
   
   private ElidePass1 up_values;
   private Map<NobaseAstNode,Double> result_value;
   private NobaseAstNode active_node;
   private IvyXmlWriter xml_writer;
   private boolean last_case;
   private Stack<NobaseAstNode> switch_stack;
   private NobaseFile for_file;
   
   ElidePass2(ElidePass1 pass1,NobaseFile nf,IvyXmlWriter xw) {
      up_values = pass1;
      xml_writer = xw;
      for_file = nf;
      result_value = new HashMap<NobaseAstNode,Double>();
      active_node = null;
      last_case = false;
      switch_stack = new Stack<NobaseAstNode>();
    }
   
   @Override public void preVisit(NobaseAstNode n) {
      if (active_node == null) {
         if (isRootRegion(n.getStartPosition(for_file),n.getEndPosition(for_file))) {
            active_node = n;
            result_value.put(n,1.0);
            // BedrockPlugin.logD("PRIORITY TOP " + n.getStartPosition() + " " + getNodeType(n) + " : " + n);
            outputXmlStart(n);
          }
         return;
       }
      double v = getPriority(n.getParent());
      double v0 = 0;
      if (up_values != null) v0 = up_values.getPriority(n);
      double p = computePriority(v,n,v0);
      // BedrockPlugin.logD("PRIORITY " + p + " " + n.getStartPosition() + " " + getNodeType(n) + " : " + n);
      if (p != 0) {
         result_value.put(n,p);
         checkSwitchBlock(n);
         outputXmlStart(n);
       }
    }
   
   @Override public void postVisit(NobaseAstNode n) {
      if (active_node == n) active_node = null;
      if (xml_writer != null && result_value.get(n) != null && result_value.get(n) > 0) {
	 xml_writer.end("ELIDE");
       }
      checkEndSwitchBlock(n);
    }
   
   @Override public boolean visit(FunctionDeclaration n) {
      return isActiveRegion(n.getStartPosition(for_file),n.getEndPosition(for_file));
    }
   
   @Override public boolean visit(Declaration n) {
      return isActiveRegion(n.getStartPosition(for_file),n.getEndPosition(for_file));
    }
   
   double getPriority(NobaseAstNode n) {
      Double dv = result_value.get(n);
      if (dv != null) return dv.doubleValue();
      return 0;
    }
   
   private void outputXmlStart(NobaseAstNode n) {
      if (xml_writer != null) {
         xml_writer.begin("ELIDE");
         int sp = n.getStartPosition(for_file);
         int ep = n.getEndPosition(for_file);
         int esp = n.getExtendedStartPosition(for_file);
         int eep = n.getExtendedEndPosition(for_file);
         xml_writer.field("START",sp);
         if (esp != sp) xml_writer.field("ESTART",esp);
         xml_writer.field("LENGTH",ep-sp);
         xml_writer.field("ELENGTH",eep-esp);
         double p = result_value.get(n);
         for (int i = 0; i < switch_stack.size(); ++i) p *= SWITCH_BLOCK_SCALE;
         xml_writer.field("PRIORITY",p);
         String typ = getFormatType(n);
         if (typ != null) {
            xml_writer.field("TYPE",typ);
            if (typ.startsWith("METHODDECL") || typ.startsWith("VARDECL")) {
               outputDeclInfo(n);
             }
          }
         String ttyp = getNodeType(n);
         if (ttyp != null) xml_writer.field("NODE",ttyp);
       }
    }
   
   private void outputDeclInfo(NobaseAstNode name) {
      NobaseSymbol nsp = name.getDefinition();
      if (nsp == null) nsp = name.getReference();
      if (nsp == null) return;
      
      switch (nsp.getNameType()) {
         case MODULE :
            break;
         case FUNCTION :
            xml_writer.field("FULLNAME",nsp.getBubblesName());
            break;
         case VARIABLE :
            xml_writer.field("FULLNAME",nsp.getBubblesName());
            break;
         case LOCAL :
            xml_writer.field("FULLNAME",nsp.getBubblesName());
            break;
       }
    }
   
   private void checkSwitchBlock(NobaseAstNode n) {
      if (!last_case || xml_writer == null) return;
      // BedrockPlugin.logD("SWITCH BLOCK CHECK " + result_value.get(n) + " " + n);
      last_case = false;
      if (result_value.get(n) == null) return;
      if (n instanceof CaseStatement) return;
      NobaseAstNode last = null;
      if (n instanceof Statement) {
	 NobaseAstNode pn = n.getParent();
	 if (!(pn instanceof SwitchStatement)) return;
         int idx = n.getIndexInParent();
	 if (idx < 0) return;
	 int lidx = idx;
	 while (lidx+1 < n.getParent().getNumChildren()) {
            NobaseAstNode cn = n.getParent().getChild(lidx+1);
            if (cn instanceof CaseStatement) break;
	    else if (cn instanceof Statement) ++lidx;
	    else return;
	  }
	 if (lidx - idx >= 2) last = n.getParent().getChild(lidx);
       }
      if (last == null) return;
      xml_writer.begin("ELIDE");
      int sp = n.getStartPosition(for_file);
      int esp = n.getExtendedStartPosition(for_file);
      int ep = last.getEndPosition(for_file);
      int eep = n.getExtendedEndPosition(for_file);
      int ln = ep - sp;
      int eln = eep - esp;
      xml_writer.field("START",sp);
      if (esp != sp) xml_writer.field("ESTART",esp);
      xml_writer.field("LENGTH",ln);
      if (eln != ln) xml_writer.field("ELENGTH",eln);
      double p = result_value.get(n);
      for (int i = 0; i < switch_stack.size(); ++i) p *= SWITCH_BLOCK_SCALE;
      xml_writer.field("PRIORITY",p);
      xml_writer.field("NODE","SWBLOCK");
      switch_stack.push(last);
    }
   
   private void checkEndSwitchBlock(NobaseAstNode n) {
      while (!switch_stack.isEmpty() && n == switch_stack.peek()) {
	 switch_stack.pop();
	 xml_writer.end("ELIDE");
       }
      last_case = (n instanceof CaseStatement);
    }
   
}	// end of innerclass ElidePass2



/********************************************************************************/
/*										*/
/*	Classes for elision region and priorities				*/
/*										*/
/********************************************************************************/

private abstract class ElideData {

   private int start_offset;
   private int end_offset;

   ElideData(int soff,int eoff) {
      start_offset = soff;
      end_offset = eoff;
    }

   boolean contains(int soff,int eoff) {
      return (start_offset <= soff && end_offset >= eoff);
    }

   boolean useForPriority(NobaseFile nf,NobaseAstNode n) {
      int sp = n.getStartPosition(nf);
      int ep = n.getEndPosition(nf);
      if (start_offset != end_offset) return contains(sp,ep);
      if (!overlaps(sp,ep)) return false;
      // check if any child overlaps, use child if so
      for (int i = 0; i < n.getNumChildren(); ++i) {
         NobaseAstNode cn = n.getChild(i);
         if (overlaps(cn.getStartPosition(nf),cn.getEndPosition(nf))) return false;
       }
      return true;
    }
   
   boolean overlaps(int soff,int eoff) {
      if (start_offset >= eoff) return false;
      if (end_offset <= soff) return false;
      return true;
    }

   boolean noteEdit(int soff,int len,int rlen) {
      if (end_offset <= soff) ; 			// before the change
      else if (start_offset > soff + len - 1) { 	// after the change
	 start_offset += rlen - len;
	 end_offset += rlen - len;
       }
      else if (start_offset <= soff && end_offset >= soff+len-1) {	// containing the change
	 end_offset += rlen -len;
       }
      else return false;				     // in the edit -- remove it
      return true;
    }

}	// end of inner abstract class ElideData




private class ElideRegion extends ElideData {

   ElideRegion(int soff,int eoff) {
      super(soff,eoff);
    }

}	// end of innerclass ElideData




private class ElidePriority extends ElideData {

   private double elide_priority;

   ElidePriority(int soff,int eoff,double pri) {
      super(soff,eoff);
      elide_priority = pri;
    }

   double getPriority() 			{ return elide_priority; }

}	// end of innerclass ElideData


/********************************************************************************/
/*										*/
/*	Priority computation classes						*/
/*										*/
/********************************************************************************/

private abstract static class Prioritizer {
   
   abstract double getPriority(double ppar,NobaseAstNode base);
   
}


private static class DefaultPrioritizer extends Prioritizer {
   
   private double base_value;
   private double count_scale;
   private double item_scale;
   
   DefaultPrioritizer(double v,double scl,double iscl) {
      base_value = v;
      count_scale = scl;
      item_scale = iscl;
    }
   
   double getPriority(double ppar,NobaseAstNode base) {
      double dv = base_value;
      NobaseAstNode par = base.getParent();
      
      if (par == null) return ppar * dv;
      if (par instanceof Block) {
         int ct = par.getNumChildren();
         boolean fnd = false;
         for (int i = 0; i < ct; ++i) {
            if (par.getChild(i) == base) fnd = true;
            dv *= count_scale;
            if (!fnd) dv *= item_scale;
          }
       }
      else  if (item_scale != 1) {
         int ct = par.getNumChildren();
         for (int i = 0; i < ct; ++i) {
            if (par.getChild(i) == base) break;
            dv *= item_scale;
          }
       }
      return ppar * dv;
    }
   
}	// end of innerclass DefaultPrioritizer



private static class PropertyDescriptor {
   
   private Class<?> parent_type;
   private int      child_index;
   
   PropertyDescriptor(Class<?> par,int idx) {
      parent_type = par;
      child_index = idx;
    }
   
   boolean match(NobaseAstNode n) {
      if (n.getParent() == null) return false;
      if (n.getParent().getClass() != parent_type) return false;
      if (n.getParent().getChild(child_index) != n) return false;
      return true;
    }
}


private static class StructuralPrioritizer extends Prioritizer {
   
   private Prioritizer base_prioritizer;
   private Map<PropertyDescriptor,Prioritizer> priority_map;
   
   StructuralPrioritizer(Prioritizer base) {
      base_prioritizer = base;
      priority_map = new HashMap<PropertyDescriptor,Prioritizer>();
    }
   
   void addPriority(Class<?> par,int idx,Prioritizer p) {
      PropertyDescriptor pd = new PropertyDescriptor(par,idx);
      priority_map.put(pd,p);
    }
   
   double getPriority(double ppar,NobaseAstNode base) {
      Prioritizer p = base_prioritizer;
      for (Map.Entry<PropertyDescriptor,Prioritizer> ent : priority_map.entrySet()) {
         if (ent.getKey().match(base)) {
            p = ent.getValue();
            break;
          }
       }
      return p.getPriority(ppar,base);
    }
   
}	// end of innerclass StructuralPrioritizer

private static class NodePrioritizer extends Prioritizer {
   
   private Prioritizer base_prioritizer;
   private Map<Class<?>,Prioritizer> priority_map;
   
   NodePrioritizer(Prioritizer base) {
      base_prioritizer = base;
      priority_map = new HashMap<Class<?>,Prioritizer>();
    }
   
   void addPriority(Class<?> c,Prioritizer p) {
      priority_map.put(c,p);
    }
   
   double getPriority(double ppar,NobaseAstNode base) {
      Prioritizer p = priority_map.get(base.getClass());
      if (p == null) p = base_prioritizer;
      return p.getPriority(ppar,base);
    }
   
}	// end of innerclass NodePrioritizer


}	// end of class NobaseElider




/* end of NobaseElider.java */

