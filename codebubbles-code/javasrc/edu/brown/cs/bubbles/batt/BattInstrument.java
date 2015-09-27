/********************************************************************************/
/*										*/
/*		BattInstrument.java						*/
/*										*/
/*	Bubble Automated Testing Tool javaagent instrumenter			*/
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


package edu.brown.cs.bubbles.batt;


import edu.brown.cs.bubbles.org.objectweb.asm.*;
import edu.brown.cs.bubbles.org.objectweb.asm.commons.CodeSizeEvaluator;
import edu.brown.cs.bubbles.org.objectweb.asm.util.TraceMethodVisitor;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.*;



class BattInstrument implements BattConstants, ClassFileTransformer
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BattAgent	for_agent;
private String		class_name;
private int		method_id;
private int		block_id;
private CodeSizeEvaluator size_eval;
private Set<String>	user_classes;

private static boolean	do_debug = false;


private static final String [] SYSTEM_PACKAGES = new String [] {
   "java/",
   "javax/",
   "org/junit/",
   "org/hamcrest/",
   "junit/",
   "org/w3c/",
   "sun/",
   "com/sun/",
   "oracle/",
   "jrockit/"
};



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BattInstrument(BattAgent agt)
{
   for_agent = agt;
   class_name = null;
   user_classes = null;
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void setClasses(String [] clsset)
{
   if (user_classes == null) user_classes = new HashSet<String>();

   for (String s : clsset) {
      s = s.replace('.','/');
      if (isBattClass(s) || isSystemClass(s)) continue;
      user_classes.add(s);
      // System.err.println("BATTAGENT: ADD CLASS " + s);
    }

   for (String s : user_classes) {
      String s1 = s.replace("/",".");
      String s2 = s1.replace("$",".");
      try {
	 // System.err.println("BATTAGENT: TRY " + s2);
	 Class.forName(s2);
       }
      catch (Throwable t) { }
    }
}



/********************************************************************************/
/*										*/
/*	Transformation methods							*/
/*										*/
/********************************************************************************/

public byte [] transform(ClassLoader ldr,String name,Class<?> cls,
			    ProtectionDomain dom,byte [] buf)
{
   // System.err.println("BATTAGENT: CHECK " + name + " " + Thread.currentThread().getName() + " " + cls);

   if (cls != null && Modifier.isAbstract(cls.getModifiers())) return null;

   if (user_classes == null) {
      if (isBattClass(name) || isSystemClass(name)) return null;
    }
   else if (!user_classes.contains(name)) return null;

   try {
      return instrument(name,buf);
    }
   catch (Throwable t) {
      System.err.println("BATT: Instrumentation issue: " + t);
    }

   return null;
}



private byte [] instrument(String name,byte [] buf)
{
   // System.err.println("BATTAGENT: INSTRUMENT " + name + " " + buf.length);

   byte [] rsltcode;
   try {
      ClassReader reader = new ClassReader(buf);
      ClassWriter writer = new ClassWriter(reader,ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
      ClassVisitor ins = new BattTransformer(name,writer);
      reader.accept(ins,ClassReader.SKIP_FRAMES);
      rsltcode = writer.toByteArray();
    }
   catch (Throwable t) {
      System.err.println("BATT: Problem doing instrumentation: " + t);
      t.printStackTrace();
      return null;
    }

   // System.err.println("BATTAGENT: INSTRUMENT RETURN " + name + " " + rsltcode.length);

   return rsltcode;
}




private boolean isBattClass(String name)
{
   if (name.startsWith("edu/brown/cs/bubbles/batt/")) return true;
   if (name.startsWith("edu/brown/cs/bubbles/org/objectweb/asm")) return true;
   if (name.startsWith("edu/brown/cs/bubbles/bandaid/org/objectweb/asm")) return true;

   return false;
}




private boolean isSystemClass(String name)
{
   // Should really check for a user class given we know those

   for (String s : SYSTEM_PACKAGES) {
      if (name.startsWith(s)) return true;
    }

   return false;
}




/********************************************************************************/
/*										*/
/*	Transformer								*/
/*										*/
/********************************************************************************/

private class BattTransformer extends ClassAdapter {

   private String super_name;
   private boolean is_abstract;

   BattTransformer(String name,ClassVisitor v) {
      super(v);
      class_name = name;
      is_abstract = false;
    }

   @Override public void visit(int v,int a,String nm,String sgn,String sup,String [] ifc) {
      super.visit(v,a,nm,sgn,sup,ifc);
      super_name = sup;
      is_abstract = (a & Opcodes.ACC_ABSTRACT) != 0;
    }

   @SuppressWarnings("unused")
   boolean isAbstract() 		{ return is_abstract; }

   @Override public MethodVisitor visitMethod(int acc,String name,String desc,String sgn,
						 String [] exc) {
      method_id = for_agent.getMethodId(class_name,name,desc);

      MethodVisitor mv = super.visitMethod(acc,name,desc,sgn,exc);
      if (name.equals("<clinit>")) return mv;

      if (do_debug) mv = new Tracer(mv,class_name + "." + name + desc);

      String sup = null;
      if (name.equals("<init>")) sup = super_name;

      mv = new CoverageSetup(mv,sup);

      size_eval = new CodeSizeEvaluator(mv);
      mv = size_eval;

      return mv;
    }

}	// end of inner class BattTransformer




/********************************************************************************/
/*										*/
/*	Method visitor to add coverage calls					*/
/*										*/
/********************************************************************************/

private class CoverageSetup extends MethodAdapter {

   private boolean	block_next;
   private String	super_class;

   CoverageSetup(MethodVisitor v,String sclass) {
      super(v);
      block_next = false;
      super_class = sclass;
    }

   @Override public void visitCode() {
      super.visitCode();
      if (super_class == null) {
	 addCall(method_id,"handleEntry");
	 block_next = true;
	 checkBlock();
       }
    }

   @Override public void visitEnd() {
      super.visitEnd();
      method_id = -1;
    }

   @Override public void visitFieldInsn(int opc,String own,String nam,String desc) {
      checkBlock();
      super.visitFieldInsn(opc,own,nam,desc);
    }

   @Override public void visitIincInsn(int var,int incr) {
      checkBlock();
      super.visitIincInsn(var,incr);
    }

   @Override public void visitInsn(int opc) {
      switch (opc) {
	 case Opcodes.IRETURN :
	 case Opcodes.LRETURN :
	 case Opcodes.FRETURN :
	 case Opcodes.DRETURN :
	 case Opcodes.ARETURN :
	 case Opcodes.RETURN :
	    addCall(method_id,"handleExit");
	    block_next = true;
	    break;
	 default :
	    checkBlock();
	    break;
       }
      super.visitInsn(opc);
    }

   @Override public void visitIntInsn(int opc,int opn) {
      checkBlock();
      super.visitIntInsn(opc,opn);
    }

   @Override public void visitJumpInsn(int opc,Label lbl) {
      checkBlock();
      super.visitJumpInsn(opc,lbl);
      block_next = true;
    }

   @Override public void visitLabel(Label lbl) {
      super.visitLabel(lbl);
    }

   @Override public void visitLdcInsn(Object o) {
      checkBlock();
      super.visitLdcInsn(o);
    }

   @Override public void visitLineNumber(int line,Label start) {
      for_agent.noteLineNumber(line,size_eval.getMinSize(),method_id,block_id);
      super.visitLineNumber(line,start);
    }

   @Override public void visitLookupSwitchInsn(Label dflt,int [] keys,Label [] lbls) {
      checkBlock();
      super.visitLookupSwitchInsn(dflt,keys,lbls);
      block_next = true;
    }

   @Override public void visitMethodInsn(int opc,String own,String nm,String ds) {
      checkBlock();
      super.visitMethodInsn(opc,own,nm,ds);
      if (super_class != null && nm.equals("<init>")) {
	 if (own.equals(super_class) || own.equals(class_name)) {
	    super_class = null;
	    addCall(method_id,"handleEntry");
	    block_next = true;
	    checkBlock();
	  }
       }
    }

   @Override public void visitMultiANewArrayInsn(String desc,int dims) {
      checkBlock();
      super.visitMultiANewArrayInsn(desc,dims);
    }

   @Override public void visitTableSwitchInsn(int min,int max,Label dflt,Label	[] lbls) {
      checkBlock();
      super.visitTableSwitchInsn(min,max,dflt,lbls);
      block_next = true;
    }

   @Override public void visitTypeInsn(int opc,String typ) {
      checkBlock();
      super.visitTypeInsn(opc,typ);
    }

   @Override public void visitVarInsn(int opc,int var) {
      checkBlock();
      super.visitVarInsn(opc,var);
    }

   private void checkBlock() {
      if (!block_next) return;
      int bid = for_agent.getBlockId(method_id,size_eval.getMinSize());
      addCall(bid,"handleBlockEntry");
      block_id = bid;
      block_next = false;
    }

   private void addCall(int id,String rtn) {
      if (super_class != null) return;
      if (id < 128) super.visitIntInsn(Opcodes.BIPUSH,id);
      else if (id < 128*256) super.visitIntInsn(Opcodes.SIPUSH,id);
      else super.visitLdcInsn(Integer.valueOf(id));
      super.visitMethodInsn(Opcodes.INVOKESTATIC,"edu/brown/cs/bubbles/batt/BattAgent",rtn,"(I)V");
    }

}	// end of inner class CoverageSetup



/********************************************************************************/
/*										*/
/*	Debugging methods and classes						*/
/*										*/
/********************************************************************************/

private static class Tracer extends TraceMethodVisitor {

   private String method_name;

   Tracer(MethodVisitor v,String who) {
      super(v);
      method_name = who;
    }

   @Override public void visitEnd() {
      List<?> tx = getText();
      System.err.println("TRACE METHOD " + method_name);
      for (Object o : tx) {
	 System.err.print(o.toString());
       }
    }

}	// end of inner class Tracer




}	// end of class BattInstrument



/* end of BattInstrument.java */
