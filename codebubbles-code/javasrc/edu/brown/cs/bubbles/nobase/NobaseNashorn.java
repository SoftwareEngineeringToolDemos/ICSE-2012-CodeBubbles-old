/********************************************************************************/
/*										*/
/*		NobaseNashorn.java						*/
/*										*/
/*	AST and parsing using Nashorn parser					*/
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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import jdk.nashorn.internal.ir.*;
import jdk.nashorn.internal.ir.debug.ASTWriter;
import jdk.nashorn.internal.ir.debug.PrintVisitor;
import jdk.nashorn.internal.ir.visitor.NodeVisitor;
import jdk.nashorn.internal.parser.*;
import jdk.nashorn.internal.runtime.*;
import jdk.nashorn.internal.runtime.options.Options;


class NobaseNashorn implements NobaseConstants, NobaseConstants.IParser
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static boolean do_debug = true;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseNashorn()
{ }



/********************************************************************************/
/*										*/
/*	Parsing methods 							*/
/*										*/
/********************************************************************************/

@Override public ISemanticData parse(NobaseProject proj,NobaseFile fd,boolean lib)
{
   Source src = new Source(fd.getFileName(),fd.getContents());
   Options opts = new Options("nashorn");
   PrintWriter pw = new PrintWriter(new StringWriter());
   ScriptEnvironment env = new ScriptEnvironment(opts,pw,pw);
   DeferredErrorManager em = new DeferredErrorManager(fd);
   if (fd.getFileName().endsWith(".json")) {
      JSONParser jsonparse = new JSONParser(src,em);
      Node nd = jsonparse.parse();
      if (do_debug) {
         System.err.println("WORKING ON " + fd.getFileName());
         try {
            ASTWriter pv = new ASTWriter(nd);
            System.err.println("PARSE: " + pv.toString());
          }
         catch (Throwable t) { }
       }
      return null;
    }

   Parser parser = new Parser(env,src,em);
   FunctionNode fn = parser.parse();
   if (fn == null) {
      NobaseMain.logE("Problem parsing " + fd.getFileName());
      return null;
    }
   if (do_debug) {
      System.err.println("WORKING ON " + fd.getFileName());
      try {
         ASTWriter pv = new ASTWriter(fn);
         System.err.println("PARSE: " + pv.toString());
       }
      catch (Throwable t) { }
    }
   ParseData rslt = new ParseData(proj,fd,em,fn.getBody(),lib);
   if (do_debug) {
      System.err.println("RESULT: " + rslt.getRootNode().dumpTree(fd));
    }
   return rslt;
}



/********************************************************************************/
/*										*/
/*	 Result Data								*/
/*										*/
/********************************************************************************/

private static class ParseData implements ISemanticData {

   private NobaseProject for_project;
   private NobaseFile for_file;
   private List<NobaseMessage> message_list;
   private NashornAstNode root_node;
   private boolean is_library;

   ParseData(NobaseProject proj,NobaseFile file,DeferredErrorManager em,Block b,boolean lib) {
      for_project = proj;
      for_file = file;
      is_library = lib;
      message_list = new ArrayList<NobaseMessage>(em.getMessages());
      // copy errors from error manager
      for_project = proj;
      for_file = file;
      is_library = lib;
      root_node = new NashornAstFileModule(b,null);
    }

   @Override public NobaseFile getFileData()		{ return for_file; }
   @Override public NobaseProject getProject()		{ return for_project; }
   @Override public List<NobaseMessage> getMessages()	{ return message_list; }
   @Override public NobaseAst.NobaseAstNode getRootNode() {
      return root_node;
    }

   @Override public void addMessages(List<NobaseMessage> msgs) {
      if (msgs == null || is_library) return;
      message_list.addAll(msgs);
    }

}	// end of inner class ParseData



/********************************************************************************/
/*										*/
/*	Generic AST Node							*/
/*										*/
/********************************************************************************/

private static NashornAstNode createNashornAstNode(Node pn,NashornAstNode par)
{
   if (pn == null) return null;
   NashornAstNode rslt = null;
   if (pn instanceof LiteralNode.ArrayLiteralNode) {
      rslt = new NashornAstArrayConstructor(pn,par);
    }
   else if (pn instanceof BinaryNode) {
      BinaryNode bin = (BinaryNode) pn;
      switch (bin.tokenType()) {
	 case COMMALEFT :
	 case COMMARIGHT :
	    rslt = new NashornAstCommaOperation(pn,par);
	    break;
	 case AND :
	 case OR :
	    rslt = new NashornAstControlOperation(pn,par);
	    break;
	 case DELETE :
	    rslt = new NashornAstDeleteOperation(pn,par);
	    break;
	 case IN :
	    rslt = new NashornAstInOperation(pn,par);
	    break;
	 default :
	    break;
       }
      if (rslt == null && bin.isAssignment()) {
	 rslt = new NashornAstAssignOperation(pn,par);
       }
      else if (rslt == null) {
	 rslt = new NashornAstSimpleOperation(pn,par);
       }
    }
   else if (pn instanceof UnaryNode) {
      UnaryNode uny = (UnaryNode) pn;
      switch (uny.tokenType()) {
	 case TYPEOF :
	    rslt = new NashornAstTypeofOperation(pn,par);
	    break;
	 case VOID :
	    rslt = new NashornAstVoidOperation(pn,par);
	    break;
	 default :
	    break;
       }
      if (rslt == null) {
	 rslt = new NashornAstSimpleOperation(pn,par);
       }
    }
   else if (pn instanceof TernaryNode) {
      rslt = new NashornAstSimpleOperation(pn,par);
    }
   else if (pn instanceof AccessNode) {
      return new NashornAstMemberAccess(pn,par);
    }
   else if (pn instanceof Block) {
      if (par instanceof NashornAstSwitchStatement) {
	 NashornAstNode npar = new NashornAstFinallyStatement(pn,par);
	 rslt = new NashornAstBlock(pn,npar);
       }
      else rslt = new NashornAstBlock(pn,par);
    }
   else if (pn instanceof BlockStatement) {
      BlockStatement bs = (BlockStatement) pn;
      rslt = new NashornAstBlock(bs.getBlock(),par);
    }
   else if (pn instanceof BreakNode) {
      rslt = new NashornAstBreakStatement(pn,par);
    }
   else if (pn instanceof CallNode) {
      CallNode cn = (CallNode) pn;
      if (cn.isNew()) rslt = new NashornAstConstructorCall(pn,par);
      else rslt = new NashornAstFunctionCall(pn,par);
    }
   else if (pn instanceof CaseNode) {
      CaseNode cn = (CaseNode) pn;
      if (cn.getTest() == null) rslt = new NashornAstDefaultCaseStatement(pn,par);
      else rslt = new NashornAstCaseStatement(pn,par);
    }
   else if (pn instanceof CatchNode) {
      rslt = new NashornAstCatchStatement(pn,par);
    }
   else if (pn instanceof ContinueNode) {
      rslt = new NashornAstContinueStatement(pn,par);
    }
   else if (pn instanceof EmptyNode) {
      rslt = new NashornAstNoopStatement(pn,par);
    }
   else if (pn instanceof ExpressionStatement) {
      rslt = new NashornAstExpressionStatement(pn,par);
    }
   else if (pn instanceof ForNode) {
      ForNode fn = (ForNode) pn;
      if (fn.isForEach() || fn.isForIn()) {
	 rslt = new NashornAstForEachLoop(pn,par);
       }
      else rslt = new NashornAstForLoop(pn,par);
    }
   else if (pn instanceof FunctionNode) {
      rslt = new NashornAstFunctionConstructor(pn,par);
    }
   else if (pn instanceof IdentNode) {
      rslt = new NashornAstIdentifier(pn,par);
    }
   else if (pn instanceof IndexNode) {
      rslt = new NashornAstArrayIndex(pn,par);
    }
   else if (pn instanceof IfNode) {
      rslt = new NashornAstIfStatement(pn,par);
    }
   else if (pn instanceof LabelNode) {
      rslt = new NashornAstLabeledStatement(pn,par);
    }
   else if (pn instanceof LiteralNode) {
      LiteralNode<?> ln = (LiteralNode<?>) pn;
      if (ln.isNull()) {
	 rslt = new NashornAstNullLiteral(pn,par);
       }
      else if (ln.isString()) {
	 rslt = new NashornAstStringLiteral(pn,par);
       }
      else if (ln.getValue() instanceof Boolean) {
	 rslt = new NashornAstBooleanLiteral(pn,par);
       }
      else if (ln.getValue() instanceof Long || ln.getValue() instanceof Integer) {
	 rslt = new NashornAstIntegerLiteral(pn,par);
       }
      else if (ln.isNumeric()) {
	 rslt = new NashornAstRealLiteral(pn,par);
       }
      else if (ln.getValue() instanceof Lexer.RegexToken) {
	 rslt = new NashornAstRegexpLiteral(pn,par);
       }
      // handle regex
    }
   else if (pn instanceof ObjectNode) {
      rslt = new NashornAstObjectConstructor(pn,par);
    }
   else if (pn instanceof PropertyNode) {
      PropertyNode prop = (PropertyNode) pn;
      // getter/etter might not be correct
      if (prop.getGetter() != null) {
	 rslt = new NashornAstGetterProperty(pn,par);
       }
      else if (prop.getSetter() != null) {
	 rslt = new NashornAstSetterProperty(pn,par);
       }
      else rslt = new NashornAstValueProperty(pn,par);
    }
   else if (pn instanceof ReturnNode) {
      rslt = new NashornAstReturnStatement(pn,par);
    }
   else if (pn instanceof SwitchNode) {
      rslt = new NashornAstSwitchStatement(pn,par);
    }
   else if (pn instanceof ThrowNode) {
      rslt = new NashornAstThrowStatement(pn,par);
    }
   else if (pn instanceof TryNode) {
      rslt = new NashornAstTryStatement(pn,par);
    }
   else if (pn instanceof VarNode) {
      rslt = new NashornAstDeclaration(pn,par);
    }
   else if (pn instanceof WhileNode) {
      WhileNode wn = (WhileNode) pn;
      if (wn.isDoWhile()) rslt = new NashornAstDoWhileLoop(pn,par);
      else rslt = new NashornAstWhileLoop(pn,par);
    }
   else if (pn instanceof WithNode) {
      rslt = new NashornAstWithStatement(pn,par);
    }

   if (rslt == null) {
      System.err.println("UNKNOWN AST: " + pn);
    }

   return rslt;
}



private abstract static class NashornAstNode extends NobaseAstNodeBase {

   private Node nashorn_node;
   private NashornAstNode parent_node;
   private List<NashornAstNode> child_nodes;
   private int start_position;
   private int end_position;
   private int extended_end;

   NashornAstNode(Node ptn,NashornAstNode par) {
      nashorn_node = ptn;
      parent_node = par;
      child_nodes = null;

      start_position = -1;
      if (ptn != null) {
	 int pos = ptn.getStart();
	 if (pos > 0) start_position = ptn.getStart();
       }

      if (ptn != null) {
	 ChildFinder cf = new ChildFinder(this);
	 ptn.accept(cf);
       }

      end_position = -1;
      extended_end = -1;
    }

   protected Node getNashornNode()		{ return nashorn_node; }

   void addChild(NashornAstNode node) {
      if (child_nodes == null) child_nodes = new ArrayList<NashornAstNode>(4);
      child_nodes.add(node);
      noteChild(node);
    }

   protected void addChild(NashornAstNode n,int idx) {
      if (child_nodes == null) child_nodes = new ArrayList<NashornAstNode>();
      child_nodes.add(idx,n);
      noteChild(n);
    }

   private void noteChild(NashornAstNode nd) {
      if (nd == null)
	 return;
      int pos = nd.start_position;
      if (pos > 0 && (pos < start_position || start_position < 0)) {
	 start_position = pos;
	 if (parent_node != null) parent_node.noteChild(this);
       }
    }

   public void accept(NobaseAstVisitor v) {
      if (v == null) return;
      if (v.preVisit2(this)) {
         if (accept0(v)) {
            if (child_nodes != null) {
               for (NashornAstNode cn : child_nodes) {
        	  if (cn != null) cn.accept(v);
        	}
             }
          }
         accept1(v);
       }
      v.postVisit(this);
    }

   protected abstract boolean accept0(NobaseAstVisitor v);
   protected abstract void accept1(NobaseAstVisitor v);

   @Override public NobaseAst.NobaseAstNode getParent() { return parent_node; }
   @Override public int getNumChildren() {
      if (child_nodes == null) return 0;
      return child_nodes.size();
    }
   @Override public NobaseAst.NobaseAstNode getChild(int i) {
      if (i < 0) return null;
      if (child_nodes == null) return null;
      if (i >= child_nodes.size()) return null;
      return child_nodes.get(i);
    }

   @Override public int getStartLine(NobaseFile nf) {
      return nf.getLineNumber(getStartPosition(nf));
    }
   @Override public int getStartChar(NobaseFile nf) {
      return nf.getCharPosition(getStartPosition(nf));
    }
   @Override public int getStartPosition(NobaseFile nf) {
      if (start_position < 0) return 0;
      return start_position;
    }
   @Override public int getEndLine(NobaseFile nf) {
      return nf.getLineNumber(getEndPosition(nf));
    }
   @Override public int getEndChar(NobaseFile nf) {
      return nf.getCharPosition(getEndPosition(nf));
    }

   @Override public int getEndPosition(NobaseFile nf) {
      if (end_position < 0) computeEnds(nf);
      return end_position;
    }

   @Override public int getExtendedStartPosition(NobaseFile nf) {
      int spos = getStartPosition(nf);
      NobaseAst.NobaseAstNode par = getParent();
      if (par == null || par instanceof NobaseAst.FileModule) return 0;

      int parspos = par.getStartPosition(nf);
      if (spos == parspos) return spos;
      if (par instanceof NobaseAst.Block && nf != null) {
	 int minpos = parspos;
	 for (int i = 0; i < par.getNumChildren(); ++i) {
	    NobaseAst.NobaseAstNode cnode = par.getChild(i);
	    if (cnode == this) continue;
	    int epos = cnode.getEndPosition(nf);
	    if (epos <= spos && epos > minpos) minpos = epos;
	  }
	 if (minpos == spos) return spos;
	 IDocument doc = nf.getDocument();
	 if (doc != null) {
	    try {
	       int lno = doc.getLineOfOffset(minpos);
	       int npos = doc.getLineOffset(lno+1);
	       if (npos < spos) return npos;
	     }
	    catch (BadLocationException e) { }
	  }
       }

      return spos;
    }

   @Override public int getExtendedEndPosition(NobaseFile nf) {
      if (extended_end < 0) computeEnds(nf);
      return extended_end;
    }

   @Override public String toString() {
      if (nashorn_node != null) {
	 PrintVisitor pv = new PrintVisitor(nashorn_node,false);
	 return pv.toString();
       }
      else return getChild(0).toString();
    }

   private void computeEnds(NobaseFile nf) {
      IDocument d = nf.getDocument();
      int spos = getStartPosition(nf);
      int epos = 0;
      if (nashorn_node != null) {
	 epos = nashorn_node.getFinish();
       }
      else if (child_nodes != null) {
	 epos = child_nodes.get(getNumChildren()-1).nashorn_node.getFinish();
       }
      end_position = epos;
      extended_end = epos;
      if (epos < spos) return;

      boolean incmmt = false;
      boolean inlinecmmt = false;
      boolean havetext = false;
      char instring = 0;
      String txt = null;
      try {
	 txt = d.get(spos,epos-spos);
       }
      catch (BadLocationException e) {
	 return;
       }
      int len = txt.length();
      for (int i = 0; i < len; ++i) {
	 char c = txt.charAt(i);
	 if (incmmt) {
	    if (c == '*' && i+1 < len && txt.charAt(i+1) == '/') {
	       i++;
	       incmmt = false;
	     }
	  }
	 else if (inlinecmmt && c != '\n') ;
	 else if (c == '\n') {
	    instring = 0;
	    inlinecmmt = false;
	    if (havetext) {
	       end_position = extended_end = i+spos+1;
	     }
	    havetext = false;
	  }
	 else if (instring != 0) {
	    if (c == '\\') ++i;
	    else if (c == instring) {
	       instring = 0;
	     }
	    havetext = true;
	    end_position = extended_end = i+spos+1;
	  }
	 else if (c == '/' && i+1 < len && txt.charAt(i+1) == '/') {
	    ++i;
	    inlinecmmt = true;
	  }
	 else if (c == '/' && i+1 < len && txt.charAt(i+1) == '*') {
	    ++i;
	    incmmt = true;
	  }
	 else if (c == '"' || c == '\'') {
	    instring = c;
	    havetext = true;
	    end_position = extended_end = i+spos+1;
	  }
	 else if (!Character.isWhitespace(c)) {
	    havetext = true;
	    end_position = extended_end = i+spos+1;
	  }
       }
    }

}	// end of inner class NashornAst



/********************************************************************************/
/*										*/
/*	Visitor to get child nodes						*/
/*										*/
/********************************************************************************/

private static class ChildFinder extends NodeVisitor<LexicalContext> {

   private NashornAstNode outer_parent;
   private int node_level;

   ChildFinder(NashornAstNode par) {
      super(new LexicalContext());
      outer_parent = par;
      node_level = 0;
    }

   @Override protected boolean enterDefault(Node n) {
      boolean fg = false;
      switch (node_level) {
         case 0:
            fg = true;
            ++node_level;
            break;
         case 1:
            if (n == null) {
               System.err.println("ADD NULL");
             }
            NashornAstNode node = createNashornAstNode(n,outer_parent);
            outer_parent.addChild(node);
            break;
         default :
            break;
       }
      return fg;
    }

  @Override protected Node leaveDefault(Node n) {
     --node_level;
     return n;
   }

}	// end of inner class ChildFinder




/********************************************************************************/
/*										*/
/*	FileModule node 							*/
/*										*/
/********************************************************************************/

private static class NashornAstFileModule extends NashornAstNode implements NobaseAst.FileModule {

   NashornAstFileModule(Block b,NashornAstNode par) {
      super(null,par);
      addChild(createNashornAstNode(b,this));
    }

   @Override public NobaseAst.Block getBlock() {
      return (NobaseAst.Block) getChild(0);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstFileModule



/********************************************************************************/
/*										*/
/*	Generic node types							*/
/*										*/
/********************************************************************************/

private abstract static class NashornAstExpression extends NashornAstNode implements NobaseAst.Expression {

   protected NashornAstExpression(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   @Override public boolean isLeftHandSide() {
      // go up parents
      return false;
    }
}




private abstract static class NashornAstOperation extends NashornAstExpression implements NobaseAst.Operation {

   protected NashornAstOperation(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   @Override public int getNumOperands() {
       return getNumChildren();
    }

   @Override public String getOperator() {
      return getNashornNode().tokenType().getName();
    }

   @Override public NobaseAst.Expression getOperand(int i) {
      return (NobaseAst.Expression) getChild(i);
    }

   @Override protected void dumpLocal(StringBuffer buf) {
      buf.append(" ");
      buf.append(getOperator());
    }

}	// end of inner calss NashornAstOperation



/********************************************************************************/
/*										*/
/*	AST nodes for Nashorn AST						*/
/*										*/
/********************************************************************************/


private static class NashornAstArrayConstructor extends NashornAstExpression implements NobaseAst.ArrayConstructor {

   NashornAstArrayConstructor(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   @Override public int getNumElements() {
      return getNumChildren();
    }

   @Override public NobaseAst.Expression getElement(int i) {
      return (NobaseAst.Expression) getChild(i);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstArrayConstructor



private static class NashornAstArrayIndex extends NashornAstOperation implements NobaseAst.ArrayIndex {

   NashornAstArrayIndex(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstArrayIndex



private static class NashornAstAssignOperation extends NashornAstOperation implements NobaseAst.AssignOperation {

   NashornAstAssignOperation(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstAssignOperation




private static class NashornAstBlock extends NashornAstNode implements NobaseAst.Block {

   NashornAstBlock(Node n,NashornAstNode par) {
      super(n,par);
    }

   @Override protected boolean accept0(NobaseAstVisitor v)	{ return v.visit(this); }
   @Override protected void accept1(NobaseAstVisitor v) 	{ v.endVisit(this); }

}	// end of inner class NashornAstBlock


private static class NashornAstBooleanLiteral extends NashornAstExpression implements NobaseAst.BooleanLiteral {

   NashornAstBooleanLiteral(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   @Override public Boolean getValue() {
      return ((LiteralNode<?>) getNashornNode()).getBoolean();
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

   protected void dumpLocal(StringBuffer buf) {
      buf.append(" ");
      buf.append(getValue());
    }

}	// end of inner class NashornAstBooleanLiteral




private static class NashornAstBreakStatement extends NashornAstNode implements NobaseAst.BreakStatement {

   NashornAstBreakStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstBreakStatement




private static class NashornAstCaseStatement extends NashornAstNode implements NobaseAst.CaseStatement {

   NashornAstCaseStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstCaseStatement




private static class NashornAstCatchStatement extends NashornAstNode implements NobaseAst.CatchStatement {

   NashornAstCatchStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstCatchStatement



private static class NashornAstCommaOperation extends NashornAstOperation implements NobaseAst.CommaOperation {

   NashornAstCommaOperation(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstCommaOperation



private static class NashornAstConstructorCall extends NashornAstOperation implements NobaseAst.ConstructorCall {

   NashornAstConstructorCall(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstConstructorCall


private static class NashornAstContinueStatement extends NashornAstNode implements NobaseAst.ContinueStatement {

   NashornAstContinueStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstContinueStatement




private static class NashornAstControlOperation extends NashornAstOperation implements NobaseAst.ControlOperation {

   NashornAstControlOperation(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstControlOperation




private static class NashornAstDefaultCaseStatement extends NashornAstNode implements NobaseAst.DefaultCaseStatement {

   NashornAstDefaultCaseStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstDefaultCaseStatement



private static class NashornAstDeleteOperation extends NashornAstOperation implements NobaseAst.DeleteOperation {

   NashornAstDeleteOperation(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstDeleteOperation




private static class NashornAstDeclaration extends NashornAstNode implements NobaseAst.Declaration {

   NashornAstDeclaration(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   @Override public NobaseAst.Identifier getIdentifier() {
      return (NobaseAst.Identifier) getChild(0);
    }

   @Override public NobaseAst.Expression getInitializer() {
      NobaseAst.NobaseAstNode cn = getChild(1);
      if (cn == null) return null;
      return (NobaseAst.Expression) cn;
    }


   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstDeclaration





private static class NashornAstDoWhileLoop extends NashornAstNode implements NobaseAst.DoWhileLoop {

   NashornAstDoWhileLoop(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstDoWhileLoop





private static class NashornAstExpressionStatement extends NashornAstNode implements NobaseAst.ExpressionStatement {

   NashornAstExpressionStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   @Override public NobaseAst.Expression getExpression() {
      return (NobaseAst.Expression) getChild(0);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstExpressionStatement




private static class NashornAstFinallyStatement extends NashornAstNode implements NobaseAst.FinallyStatement {

   NashornAstFinallyStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstFinallyStatement




private static class NashornAstForEachLoop extends NashornAstNode implements NobaseAst.ForEachLoop {

   NashornAstForEachLoop(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstForEachLoop




private static class NashornAstForLoop extends NashornAstNode implements NobaseAst.ForLoop {

   NashornAstForLoop(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstForLoop




private static class NashornAstFormalParameter extends NashornAstNode implements NobaseAst.FormalParameter {

   NashornAstFormalParameter(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   @Override public NobaseAst.Identifier getIdentifier() {
     return (NobaseAst.Identifier)  getChild(0);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstFormalParameter



private static class NashornAstFunctionCall extends NashornAstOperation implements NobaseAst.FunctionCall {

   NashornAstFunctionCall(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

   @Override protected void dumpLocal(StringBuffer buf) { }

}	// end of inner class NashornAstFunctionCall



private static class NashornAstFunctionConstructor extends NashornAstExpression implements NobaseAst.FunctionConstructor {

   NashornAstFunctionConstructor(Node pn,NashornAstNode par) {
      super(pn,par);
      FunctionNode fctnode = (FunctionNode) pn;
      IdentNode ident = fctnode.getIdent();
      int idx = 0;
      if (ident != null) {
		NashornAstNode inode = createNashornAstNode(ident,this);
		addChild(inode,idx++);
       }
      for (IdentNode param : fctnode.getParameters()) {
	 NashornAstNode fpnode = new NashornAstFormalParameter(param,this);
	 NashornAstNode pidnode = createNashornAstNode(param,fpnode);
	 fpnode.addChild(pidnode);
	 addChild(fpnode,idx++);
       }
    }

   @Override public NobaseAst.Block getBody() {
      return (NobaseAst.Block) getChild(getNumChildren()-1);
    }

   @Override public NobaseAst.Identifier getIdentifier() {
      NobaseAst.NobaseAstNode nd = getChild(0);
      if (nd != null && nd instanceof NobaseAst.Identifier) return (NobaseAst.Identifier) nd;
      return null;
    }

   @Override public List<NobaseAst.FormalParameter> getParameters() {
      List<NobaseAst.FormalParameter> rslt = new ArrayList<NobaseAst.FormalParameter>();
      for (int i = 0; i < getNumChildren()-1; ++i) {
	 NobaseAst.NobaseAstNode nd = getChild(i);
	 if (nd instanceof NobaseAst.FormalParameter) {
	    rslt.add((NobaseAst.FormalParameter) nd);
	  }
       }
      return rslt;
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstFunctionConstructor




@SuppressWarnings("unused")     // not generated -- uses FunctionConstructor
private static class NashornAstFunctionDeclaration extends NashornAstNode implements NobaseAst.FunctionDeclaration {

   NashornAstFunctionDeclaration(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstFunctionDeclaration




private static class NashornAstGetterProperty extends NashornAstNode implements NobaseAst.GetterProperty {

   NashornAstGetterProperty(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstGetterProperty




private static class NashornAstIdentifier extends NashornAstExpression implements NobaseAst.Identifier {

   NashornAstIdentifier(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   private IdentNode getNode()	       { return (IdentNode) getNashornNode(); }

   @Override public String getName() {
      return getNode().getName();
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

   protected void dumpLocal(StringBuffer buf) {
      buf.append(" ");
      buf.append(getName());
    }

}	// end of inner class NashornAstIdentifier




private static class NashornAstIfStatement extends NashornAstNode implements NobaseAst.IfStatement {

   NashornAstIfStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstIfStatement



private static class NashornAstInOperation extends NashornAstOperation implements NobaseAst.InOperation {

   NashornAstInOperation(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstInOperation



private static class NashornAstIntegerLiteral extends NashornAstExpression implements NobaseAst.IntegerLiteral {

   NashornAstIntegerLiteral(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   @Override public Number getValue() {
      return (Number) ((LiteralNode<?>) getNashornNode()).getValue();
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

   protected void dumpLocal(StringBuffer buf) {
      buf.append(" ");
      buf.append(getValue());
    }

}	// end of inner class NashornAstIntegerLiteral




private static class NashornAstLabeledStatement extends NashornAstNode implements NobaseAst.LabeledStatement {

   NashornAstLabeledStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstLabeledStatement




private static class NashornAstMemberAccess extends NashornAstOperation implements NobaseAst.MemberAccess {

   NashornAstMemberAccess(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   @Override public String getMemberName() {
      AccessNode an = (AccessNode) getNashornNode();
      return an.getProperty().getName();
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstMemberAccess




@SuppressWarnings("unused")     // parser splits into individual declarations
private static class NashornAstMultiDeclaration extends NashornAstNode implements NobaseAst.MultiDeclaration {

   NashornAstMultiDeclaration(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstMultiDeclaration




private static class NashornAstNoopStatement extends NashornAstNode implements NobaseAst.NoopStatement {

   NashornAstNoopStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstNoopStatement




private static class NashornAstNullLiteral extends NashornAstExpression implements NobaseAst.NullLiteral {

   NashornAstNullLiteral(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstNullLiteral




private static class NashornAstObjectConstructor extends NashornAstExpression implements NobaseAst.ObjectConstructor {

   NashornAstObjectConstructor(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   @Override public int getNumElements()	{ return getNumChildren(); }
   @Override public NobaseAst.NobaseAstNode getElement(int i) {
      return getChild(i);
    }



   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstObjectConstructor




private static class NashornAstRealLiteral extends NashornAstExpression implements NobaseAst.RealLiteral {

   NashornAstRealLiteral(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   @Override public Number getValue() {
      return ((LiteralNode<?>) getNashornNode()).getNumber();
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

   protected void dumpLocal(StringBuffer buf) {
      buf.append(" ");
      buf.append(getValue());
    }

}	// end of inner class NashornAstRealLiteral





private static class NashornAstRegexpLiteral extends NashornAstExpression implements NobaseAst.RegexpLiteral {

   NashornAstRegexpLiteral(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstRegexpLiteral




private static class NashornAstReturnStatement extends NashornAstNode implements NobaseAst.ReturnStatement {

   NashornAstReturnStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

   @Override public NobaseAst.Expression getExpression() {
      if (getNumChildren() == 0) return null;
      return (NobaseAst.Expression) getChild(0);
    }

}	// end of inner class NashornAstReturnStatement




private static class NashornAstSetterProperty extends NashornAstNode implements NobaseAst.SetterProperty {

   NashornAstSetterProperty(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstSetterProperty




private static class NashornAstSimpleOperation extends NashornAstOperation implements NobaseAst.SimpleOperation {

   NashornAstSimpleOperation(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstSimpleOperation




private static class NashornAstStringLiteral extends NashornAstExpression implements NobaseAst.StringLiteral {

   NashornAstStringLiteral(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   @Override public String getValue() {
      return ((LiteralNode<?>) getNashornNode()).getString();
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

   protected void dumpLocal(StringBuffer buf) {
      buf.append(" ");
      buf.append(getValue());
    }

}	// end of inner class NashornAstStringLiteral




private static class NashornAstSwitchStatement extends NashornAstNode implements NobaseAst.SwitchStatement {

   NashornAstSwitchStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstSwitchStatement




private static class NashornAstThrowStatement extends NashornAstNode implements NobaseAst.ThrowStatement {

   NashornAstThrowStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstThrowStatement




private static class NashornAstTryStatement extends NashornAstNode implements NobaseAst.TryStatement {

   NashornAstTryStatement(Node pn,NashornAstNode par) {
      super(pn,par); }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstTryStatement



private static class NashornAstTypeofOperation extends NashornAstOperation implements NobaseAst.TypeofOperation {

   NashornAstTypeofOperation(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstTypeofOperation



private static class NashornAstValueProperty extends NashornAstNode implements NobaseAst.ValueProperty {

   NashornAstValueProperty(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   private PropertyNode getNode()      { return (PropertyNode) getNashornNode(); }

   @Override public NobaseAst.Expression getValueExpression() {
      return (NobaseAst.Expression) getChild(1);
    }

   @Override public String getPropertyName() {
      return getNode().getKeyName();
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstValueProperty




private static class NashornAstWhileLoop extends NashornAstNode implements NobaseAst.WhileLoop {

   NashornAstWhileLoop(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstWhileLoop




private static class NashornAstWithStatement extends NashornAstNode implements NobaseAst.WithStatement {

   NashornAstWithStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   @Override public NobaseAst.Statement getBody() {
      return (NobaseAst.Statement) getChild(1);
    }

   @Override public NobaseAst.Expression getScopeObject() {
      return (NobaseAst.Expression) getChild(0);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstWithStatement


private static class NashornAstVoidOperation extends NashornAstOperation implements NobaseAst.VoidOperation {

   NashornAstVoidOperation(Node pn,NashornAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class NashornAstVoidOperation




/********************************************************************************/
/*										*/
/*	Error manager class							*/
/*										*/
/********************************************************************************/

private class DeferredErrorManager extends ErrorManager {

   private List<NobaseMessage> parse_errors;

   DeferredErrorManager(NobaseFile file) {
      parse_errors = new ArrayList<NobaseMessage>();
    }

   List<NobaseMessage> getMessages() {
      return parse_errors;
    }

   @Override public void error(ParserException e) {
      addMessage(e,ErrorSeverity.ERROR);
    }

   @Override public void error(String e) {
      System.err.println("ERROR with string: " + e);
    }

   @Override public void warning(ParserException e) {
      addMessage(e,ErrorSeverity.WARNING);
    }

   @Override public void warning(String e) {
      System.err.println("WARNING with string: " + e);
    }

   private void addMessage(ParserException e,ErrorSeverity es) {
      String msg = e.getMessage();
      int idx = msg.indexOf(":");
      if (idx >= 0) {
	 idx = msg.indexOf(" ",idx);
	 msg = msg.substring(idx+1).trim();
       }
      idx = msg.indexOf("\n");
      if (idx >= 0) {
	 msg = msg.substring(0,idx).trim();
       }
      long token = e.getToken();
      int pos = Token.descPosition(token);
      int len = Token.descLength(token);
      Source src = e.getSource();
      int sline = src.getLine(pos);
      int schar = src.getColumn(pos);
      int eline = sline;
      int echar = schar;
      try {
	 eline = src.getLine(pos+len);
	 echar = src.getColumn(pos+len);
       }
      catch (Exception ex) { }
      NobaseMessage nomsg = new NobaseMessage(es,msg,sline,schar,eline,echar);
      parse_errors.add(nomsg);
    }

}	// end of inner class DeferredErrorManager





}	// end of class NobaseNashorn




/* end of NobaseNashorn.java */

