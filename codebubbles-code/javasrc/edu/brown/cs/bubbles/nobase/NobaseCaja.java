/********************************************************************************/
/*										*/
/*		NobaseCaja.java 						*/
/*										*/
/*	Bridge to Google Caja parser						*/
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

import com.google.caja.lexer.*;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.*;
import com.google.caja.reporting.*;

import org.eclipse.jface.text.IDocument;

import java.util.*;


class NobaseCaja implements NobaseConstants, NobaseConstants.IParser
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static Map<ParseTreeNode,CajaAstNode> node_map;

private static boolean do_debug = true;


static {
   node_map = new WeakHashMap<ParseTreeNode,CajaAstNode>();
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseCaja()
{
}



/********************************************************************************/
/*										*/
/*	Parsing methods 							*/
/*										*/
/********************************************************************************/

@Override public ISemanticData parse(NobaseProject proj,NobaseFile fd,boolean lib)
{
   IDocument id = fd.getDocument();
   String s = id.get();
   InputSource is = new InputSource(fd.getFile());
   CharProducer cp = CharProducer.Factory.fromString(s,is);
   JsLexer lexer = new JsLexer(cp);
   JsTokenQueue tq = new JsTokenQueue(lexer,is);
   SimpleMessageQueue smq = new SimpleMessageQueue();
   Parser p = new Parser(tq,smq);
   p.setRecoverFromFailure(true);
   ParseData rslt = null;
   try {
      Block b = p.parse();
      rslt = new ParseData(proj,fd,smq,b,lib);
      if (do_debug) {
	 MessageContext mctx = new MessageContext();
	 try {
	    b.formatTree(mctx,System.err);
	    System.err.println();
	  }
	 catch (Exception e) { }
       }
    }
   catch (ParseException e) {
      NobaseMain.logE("Parse exception",e);
      // add error for parse exception
    }

   return rslt;
}




/********************************************************************************/
/*										*/
/*	Result data								*/
/*										*/
/********************************************************************************/

private static class ParseData implements ISemanticData {

   private NobaseProject for_project;
   private NobaseFile for_file;
   private List<NobaseMessage> message_list;
   private CajaAstNode	  root_node;
   private boolean is_library;

   ParseData(NobaseProject proj,NobaseFile file,SimpleMessageQueue smq,Block b,boolean lib) {
      for_project = proj;
      for_file = file;
      is_library = lib;
      message_list = new ArrayList<NobaseMessage>();
      for (Message m : smq.getMessages()) {
	 ErrorSeverity es = ErrorSeverity.ERROR;
	 switch (m.getMessageLevel()) {
	    case ERROR :
	    case FATAL_ERROR :
	       break;
	    case INFERENCE :
	       es = ErrorSeverity.INFO;
	       break;
	    case LINT :
	    case WARNING :
	       es = ErrorSeverity.WARNING;
	       break;
	    case SUMMARY :
	    case LOG :
	       continue;
	  }
	 FilePosition fp = null;
	 for (MessagePart mp : m.getMessageParts()) {
	    if (mp instanceof FilePosition) {
	       fp = (FilePosition) mp;
	       break;
	     }
	  }
	 int startln = 0;
	 int startcol = 0;
	 int endln = 0;
	 int endcol = 0;
	 String msg = m.toString();
	 if (fp != null) {
	    startln = fp.startLineNo();
	    startcol = fp.startCharInLine();
	    endln = fp.endLineNo();
	    endcol = fp.endCharInLine();
	    if (msg.startsWith("file:")) {
	       int idx = msg.indexOf(":");
	       idx = msg.indexOf(":",idx+1);
	       idx = msg.indexOf(":",idx+1);
	       msg = msg.substring(idx+1).trim();
	     }
	  }

	 NobaseMessage nm = new NobaseMessage(es,msg,startln,startcol,endln,endcol);
	 if (!is_library) message_list.add(nm);
       }

      root_node = new CajaAstFileModule(b,null);
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
/*	Ast node converter							*/
/*										*/
/********************************************************************************/

private static CajaAstNode createCajaAstNode(ParseTreeNode pn,CajaAstNode par)
{
   if (pn == null) return null;
   CajaAstNode rslt = node_map.get(pn);
   if (rslt != null) return rslt;

   if (pn instanceof ArrayConstructor) return new CajaAstArrayConstructor(pn,par);
   if (pn instanceof AssignOperation) return new CajaAstAssignOperation(pn,par);
   if (pn instanceof Block) return new CajaAstBlock(pn,par);
   if (pn instanceof BooleanLiteral) return new CajaAstBooleanLiteral(pn,par);
   if (pn instanceof BreakStmt) return new CajaAstBreakStatement(pn,par);
   if (pn instanceof CaseStmt) return new CajaAstCaseStatement(pn,par);
   if (pn instanceof CatchStmt) return new CajaAstCatchStatement(pn,par);
   if (pn instanceof ContinueStmt) return new CajaAstContinueStatement(pn,par);
   if (pn instanceof ControlOperation) return new CajaAstControlOperation(pn,par);
   if (pn instanceof DebuggerStmt) return new CajaAstDebuggerStatement(pn,par);
   if (pn instanceof DefaultCaseStmt) return new CajaAstDefaultCaseStatement(pn,par);
   if (pn instanceof Directive) return new CajaAstDirective(pn,par);
   if (pn instanceof DirectivePrologue) return new CajaAstDirectivePrologue(pn,par);
   if (pn instanceof DoWhileLoop) return new CajaAstDoWhileLoop(pn,par);
   if (pn instanceof Elision) return new CajaAstElision(pn,par);
   if (pn instanceof ExpressionStmt) return new CajaAstExpressionStatement(pn,par);
   if (pn instanceof FinallyStmt) return new CajaAstFinallyStatement(pn,par);
   if (pn instanceof ForEachLoop) return new CajaAstForEachLoop(pn,par);
   if (pn instanceof ForLoop) return new CajaAstForLoop(pn,par);
   if (pn instanceof FormalParam) return new CajaAstFormalParameter(pn,par);
   if (pn instanceof FunctionConstructor) return new CajaAstFunctionConstructor(pn,par);
   if (pn instanceof FunctionDeclaration) return new CajaAstFunctionDeclaration(pn,par);
   if (pn instanceof GetterProperty) return new CajaAstGetterProperty(pn,par);
   if (pn instanceof Identifier) return new CajaAstIdentifier(pn,par);
   if (pn instanceof Conditional) return new CajaAstIfStatement(pn,par);
   if (pn instanceof IntegerLiteral) return new CajaAstIntegerLiteral(pn,par);
   if (pn instanceof LabeledStmtWrapper) return new CajaAstLabeledStatement(pn,par);
   if (pn instanceof MultiDeclaration) return new CajaAstMultiDeclaration(pn,par);
   if (pn instanceof Noop) return new CajaAstNoopStatement(pn,par);
   if (pn instanceof NullLiteral) return new CajaAstNullLiteral(pn,par);
   if (pn instanceof ObjectConstructor) return new CajaAstObjectConstructor(pn,par);
// if (pn instanceof PlainModule) return new CajaAstPlainModule(pn,par);
   if (pn instanceof RealLiteral) return new CajaAstRealLiteral(pn,par);
   if (pn instanceof Reference) return new CajaAstReference(pn,par);
   if (pn instanceof RegexpLiteral) return new CajaAstRegexpLiteral(pn,par);
   if (pn instanceof ReturnStmt) return new CajaAstReturnStatement(pn,par);
   if (pn instanceof SetterProperty) return new CajaAstSetterProperty(pn,par);
   if (pn instanceof SimpleOperation) return new CajaAstSimpleOperation(pn,par);
   if (pn instanceof StringLiteral) return new CajaAstStringLiteral(pn,par);
   if (pn instanceof SwitchStmt) return new CajaAstSwitchStatement(pn,par);
   if (pn instanceof ThrowStmt) return new CajaAstThrowStatement(pn,par);
   if (pn instanceof TryStmt) return new CajaAstTryStatement(pn,par);
   if (pn instanceof ValueProperty) return new CajaAstValueProperty(pn,par);
   if (pn instanceof WhileLoop) return new CajaAstWhileLoop(pn,par);
   if (pn instanceof WithStmt) return new CajaAstWithStatement(pn,par);

   // these need to be last since they have subclasses
   if (pn instanceof Declaration) return new CajaAstDeclaration(pn,par);
   if (pn instanceof SpecialOperation) {
      SpecialOperation sop = (SpecialOperation) pn;
      switch (sop.getOperator()) {
	 case SQUARE_BRACKET :
	    return new CajaAstArrayIndex(pn,par);
	 case MEMBER_ACCESS :
	    return new CajaAstMemberAccess(pn,par);
	 case CONSTRUCTOR :
	    return new CajaAstConstructorCall(pn,par);
	 case FUNCTION_CALL :
	    return new CajaAstFunctionCall(pn,par);
	 case DELETE :
	    return new CajaAstDeleteOperation(pn,par);
	 case VOID :
	    return new CajaAstVoidOperation(pn,par);
	 case TYPEOF :
	    return new CajaAstTypeofOperation(pn,par);
	 case IN :
	    return new CajaAstInOperation(pn,par);
	 case COMMA :
	    return new CajaAstCommaOperation(pn,par);
	 default :
	    return null;
       }
    }

   return null;
}



private abstract static class CajaAstNode extends NobaseAstNodeBase {

   private ParseTreeNode caja_node;
   private CajaAstNode parent_node;

   CajaAstNode(ParseTreeNode ptn,CajaAstNode par) {
      caja_node = ptn;
      parent_node = par;
      if (ptn != null) {
	 node_map.put(ptn,this);
	 for (ParseTreeNode ctn : ptn.children()) {
	    if (node_map.get(ctn) == null) {
	       createCajaAstNode(ctn,this);
	     }
	  }
       }
    }

   protected ParseTreeNode getCajaNode()		{ return caja_node; }

   public void accept(NobaseAstVisitor v) {
      if (v == null) return;
      if (v.preVisit2(this)) {
	 if (accept0(v)) {
	    for (ParseTreeNode ptn : caja_node.children()) {
	       if (ptn == null) continue;
	       CajaAstNode cn = createCajaAstNode(ptn,this);
	       cn.accept(v);
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
      return caja_node.children().size();
    }
   @Override public NobaseAst.NobaseAstNode getChild(int i) {
      if (i < 0) return null;
      if (i >= caja_node.children().size()) return null;
      return createCajaAstNode(caja_node.children().get(i),this);
    }

   @Override public int getStartLine(NobaseFile nf) {
      return getCajaNode().getFilePosition().startLineNo();
    }
   @Override public int getStartChar(NobaseFile nf) {
      return getCajaNode().getFilePosition().startCharInLine();
    }
   @Override public int getStartPosition(NobaseFile nf) {
      return getCajaNode().getFilePosition().startCharInFile()-1;
    }
   @Override public int getEndLine(NobaseFile nf) {
      return getCajaNode().getFilePosition().endLineNo();
    }
   @Override public int getEndChar(NobaseFile nf) {
      return getCajaNode().getFilePosition().endCharInLine();
    }
   @Override public int getEndPosition(NobaseFile nf) {
      // bubbles wants offsets to be 0 based
      return getCajaNode().getFilePosition().endCharInFile()-1;
    }

   @Override public int getExtendedStartPosition(NobaseFile nf) {
      int spos = getStartPosition(nf);
      if (caja_node != null) {
         for (Token<?> t : caja_node.getComments()) {
            spos = Math.min(spos,t.pos.startCharInFile());
          }
       }
      return spos;
    }

   @Override public int getExtendedEndPosition(NobaseFile nf) {
      int epos = getEndPosition(nf);
      if (caja_node != null) {
         for (Token<?> t : caja_node.getComments()) {
            epos = Math.max(epos,t.pos.endCharInFile());
          }
       }
      return epos;
    }

   @Override public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append(((AbstractParseTreeNode) getCajaNode()).toString());

      // MessageContext ctx = new MessageContext();
      // try {
	 // getCajaNode().format(ctx,buf);
       // }
      // catch (IOException e) {
	 // return super.toString();
       // }

      return buf.toString();
    }

}	// end of inner class CajaAst


private static class CajaAstFileModule extends CajaAstNode implements NobaseAst.FileModule {

   CajaAstBlock  block_node;

   CajaAstFileModule(Block b,CajaAstNode par) {
      super(null,par);
      block_node = (CajaAstBlock) createCajaAstNode(b,this);
    }

   protected ParseTreeNode getCajaNode()		{ return block_node.getCajaNode(); }

   @Override public NobaseAst.Block getBlock() {
      return block_node;
    }
   @Override public int getNumChildren()		 { return 1; }
   @Override public NobaseAst.NobaseAstNode getChild(int i) {
      if (i == 0) return block_node;
      return null;
    }

   public void accept(NobaseAstVisitor v) {
      if (v == null) return;
      if (v.preVisit2(this)) {
	 if (accept0(v)) {
	    block_node.accept(v);
	  }
	 accept1(v);
       }
      v.postVisit(this);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}


private abstract static class CajaAstExpression extends CajaAstNode implements NobaseAst.Expression {

   protected CajaAstExpression(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   private Expression getExprNode()		{ return (Expression) getCajaNode(); }

   @Override public boolean isLeftHandSide() {
      return getExprNode().isLeftHandSide();
    }
}




private abstract static class CajaAstOperation extends CajaAstExpression implements NobaseAst.Operation {

   protected CajaAstOperation(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   private Operation getOpNode()		{ return (Operation) getCajaNode(); }

   @Override public int getNumOperands()	{ return getOpNode().children().size(); }

   @Override public String getOperator() {
      return getOpNode().getOperator().getSymbol();
    }

   @Override public NobaseAst.Expression getOperand(int i) {
      return (NobaseAst.Expression) createCajaAstNode(getOpNode().children().get(i),this);
    }

   @Override public boolean isLeftHandSide() {
      return getOpNode().isLeftHandSide();
    }

}	// end of inner calss CajaAstOperation

private static class CajaAstArrayConstructor extends CajaAstExpression implements NobaseAst.ArrayConstructor {

   CajaAstArrayConstructor(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   private ArrayConstructor getNode() {
      return (ArrayConstructor) getCajaNode();
    }

   @Override public int getNumElements() {
      return getNode().children().size();
    }
   @Override public NobaseAst.Expression getElement(int i) {
      return (NobaseAst.Expression) createCajaAstNode(getNode().children().get(i),this);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstArrayConstructor



private static class CajaAstArrayIndex extends CajaAstOperation implements NobaseAst.ArrayIndex {

   CajaAstArrayIndex(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstArrayIndex



private static class CajaAstAssignOperation extends CajaAstOperation implements NobaseAst.AssignOperation {

   CajaAstAssignOperation(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstAssignOperation




private static class CajaAstBlock extends CajaAstNode implements NobaseAst.Block {

   CajaAstBlock(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstBlock




private static class CajaAstBooleanLiteral extends CajaAstExpression implements NobaseAst.BooleanLiteral {

   CajaAstBooleanLiteral(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   @Override public Boolean getValue() {
      return ((BooleanLiteral) getCajaNode()).getValue();
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstBooleanLiteral




private static class CajaAstBreakStatement extends CajaAstNode implements NobaseAst.BreakStatement {

   CajaAstBreakStatement(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstBreakStatement




private static class CajaAstCaseStatement extends CajaAstNode implements NobaseAst.CaseStatement {

   CajaAstCaseStatement(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstCaseStatement




private static class CajaAstCatchStatement extends CajaAstNode implements NobaseAst.CatchStatement {

   CajaAstCatchStatement(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstCatchStatement



private static class CajaAstCommaOperation extends CajaAstOperation implements NobaseAst.CommaOperation {

   CajaAstCommaOperation(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstCommaOperation



private static class CajaAstConstructorCall extends CajaAstOperation implements NobaseAst.ConstructorCall {

   CajaAstConstructorCall(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstConstructorCall


private static class CajaAstContinueStatement extends CajaAstNode implements NobaseAst.ContinueStatement {

   CajaAstContinueStatement(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstContinueStatement




private static class CajaAstControlOperation extends CajaAstOperation implements NobaseAst.ControlOperation {

   CajaAstControlOperation(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstControlOperation




private static class CajaAstDebuggerStatement extends CajaAstNode implements NobaseAst.DebuggerStatement {

   CajaAstDebuggerStatement(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstDebuggerStatement




private static class CajaAstDefaultCaseStatement extends CajaAstNode implements NobaseAst.DefaultCaseStatement {

   CajaAstDefaultCaseStatement(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstDefaultCaseStatement



private static class CajaAstDeleteOperation extends CajaAstOperation implements NobaseAst.DeleteOperation {

   CajaAstDeleteOperation(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstDeleteOperation




private static class CajaAstDirective extends CajaAstNode implements NobaseAst.Directive {

   CajaAstDirective(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstDirective




private static class CajaAstDeclaration extends CajaAstNode implements NobaseAst.Declaration {

   CajaAstDeclaration(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   private com.google.caja.parser.js.Declaration getNode() {
      return (com.google.caja.parser.js.Declaration) getCajaNode();
    }

   @Override public NobaseAst.Identifier getIdentifier() {
      return (NobaseAst.Identifier) createCajaAstNode(getNode().getIdentifier(),this);
    }

   @Override public NobaseAst.Expression getInitializer() {
      return (NobaseAst.Expression) createCajaAstNode(getNode().getInitializer(),this);
    }


   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstDeclaration




private static class CajaAstDirectivePrologue extends CajaAstNode implements NobaseAst.DirectivePrologue {

   CajaAstDirectivePrologue(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstDirectivePrologue




private static class CajaAstDoWhileLoop extends CajaAstNode implements NobaseAst.DoWhileLoop {

   CajaAstDoWhileLoop(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstDoWhileLoop




private static class CajaAstElision extends CajaAstOperation implements NobaseAst.Elision {

   CajaAstElision(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstElision




private static class CajaAstExpressionStatement extends CajaAstNode implements NobaseAst.ExpressionStatement {

   CajaAstExpressionStatement(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   private ExpressionStmt getNode()	   { return (ExpressionStmt) getCajaNode(); }

   @Override public NobaseAst.Expression getExpression() {
      return (NobaseAst.Expression) createCajaAstNode(getNode().getExpression(),this);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstExpressionStatement




private static class CajaAstFinallyStatement extends CajaAstNode implements NobaseAst.FinallyStatement {

   CajaAstFinallyStatement(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstFinallyStatement




private static class CajaAstForEachLoop extends CajaAstNode implements NobaseAst.ForEachLoop {

   CajaAstForEachLoop(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstForEachLoop




private static class CajaAstForLoop extends CajaAstNode implements NobaseAst.ForLoop {

   CajaAstForLoop(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstForLoop




private static class CajaAstFormalParameter extends CajaAstNode implements NobaseAst.FormalParameter {

   CajaAstFormalParameter(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   private FormalParam getNode()		{ return (FormalParam) getCajaNode(); }

   @Override public NobaseAst.Identifier getIdentifier() {
      return (NobaseAst.Identifier) createCajaAstNode(getNode().getIdentifier(),this);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstFormalParameter



private static class CajaAstFunctionCall extends CajaAstOperation implements NobaseAst.FunctionCall {

   CajaAstFunctionCall(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstFunctionCall



private static class CajaAstFunctionConstructor extends CajaAstExpression implements NobaseAst.FunctionConstructor {

   CajaAstFunctionConstructor(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   private FunctionConstructor getNode()	{ return (FunctionConstructor) getCajaNode(); }

   @Override public NobaseAst.Block getBody() {
      return (NobaseAst.Block) createCajaAstNode(getNode().getBody(),this);
    }

   @Override public NobaseAst.Identifier getIdentifier() {
      return (NobaseAst.Identifier) createCajaAstNode(getNode().getIdentifier(),this);
    }

   @Override public List<NobaseAst.FormalParameter> getParameters() {
      List<NobaseAst.FormalParameter> rslt = new ArrayList<NobaseAst.FormalParameter>();
      for (FormalParam fp : getNode().getParams()) {
	 rslt.add((NobaseAst.FormalParameter) createCajaAstNode(fp,this));
       }
      return rslt;
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstFunctionConstructor




private static class CajaAstFunctionDeclaration extends CajaAstNode implements NobaseAst.FunctionDeclaration {

   CajaAstFunctionDeclaration(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstFunctionDeclaration




private static class CajaAstGetterProperty extends CajaAstNode implements NobaseAst.GetterProperty {

   CajaAstGetterProperty(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstGetterProperty




private static class CajaAstIdentifier extends CajaAstNode implements NobaseAst.Identifier {

   CajaAstIdentifier(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   private Identifier getNode() 	{ return (Identifier) getCajaNode(); }

   @Override public String getName() {
      return getNode().getName();
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstIdentifier




private static class CajaAstIfStatement extends CajaAstNode implements NobaseAst.IfStatement {

   CajaAstIfStatement(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstIfStatement



private static class CajaAstInOperation extends CajaAstOperation implements NobaseAst.InOperation {

   CajaAstInOperation(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstInOperation



private static class CajaAstIntegerLiteral extends CajaAstExpression implements NobaseAst.IntegerLiteral {

   CajaAstIntegerLiteral(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   @Override public Number getValue() {
      return ((IntegerLiteral) getCajaNode()).getValue();
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstIntegerLiteral




private static class CajaAstLabeledStatement extends CajaAstNode implements NobaseAst.LabeledStatement {

   CajaAstLabeledStatement(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstLabeledStatement




private static class CajaAstMemberAccess extends CajaAstOperation implements NobaseAst.MemberAccess {

   CajaAstMemberAccess(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   @Override public String getMemberName() {
      Reference r = (Reference) getCajaNode().children().get(1);
      return r.getIdentifierName();
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstMemberAccess




private static class CajaAstMultiDeclaration extends CajaAstNode implements NobaseAst.MultiDeclaration {

   CajaAstMultiDeclaration(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstMultiDeclaration




private static class CajaAstNoopStatement extends CajaAstNode implements NobaseAst.NoopStatement {

   CajaAstNoopStatement(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstNoopStatement




private static class CajaAstNullLiteral extends CajaAstExpression implements NobaseAst.NullLiteral {

   CajaAstNullLiteral(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstNullLiteral




private static class CajaAstObjectConstructor extends CajaAstExpression implements NobaseAst.ObjectConstructor {

   CajaAstObjectConstructor(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   private ObjectConstructor getNode()		{ return (ObjectConstructor) getCajaNode(); }

   @Override public int getNumElements()	{ return getNode().children().size(); }
   @Override public NobaseAst.NobaseAstNode getElement(int i) {
      return createCajaAstNode(getNode().children().get(i),this);
    }



   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstObjectConstructor




private static class CajaAstRealLiteral extends CajaAstExpression implements NobaseAst.RealLiteral {

   CajaAstRealLiteral(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   @Override public Number getValue() {
      return ((RealLiteral) getCajaNode()).getValue();
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstRealLiteral




private static class CajaAstReference extends CajaAstNode implements NobaseAst.Reference {

   CajaAstReference(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   private Reference getNode()		{ return (Reference) getCajaNode(); }

   @Override public NobaseAst.Identifier getIdentifier() {
      return (NobaseAst.Identifier) createCajaAstNode(getNode().getIdentifier(),this);
    }

   @Override public boolean isLeftHandSide() {
      return getNode().isLeftHandSide();
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstReference




private static class CajaAstRegexpLiteral extends CajaAstExpression implements NobaseAst.RegexpLiteral {

   CajaAstRegexpLiteral(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstRegexpLiteral




private static class CajaAstReturnStatement extends CajaAstNode implements NobaseAst.ReturnStatement {

   CajaAstReturnStatement(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   private ReturnStmt getNode()                 { return (ReturnStmt) getCajaNode(); }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
   @Override public NobaseAst.Expression getExpression() {
      ReturnStmt ret = getNode();
      if (ret.getReturnValue() == null) return null;
      return (NobaseAst.Expression) createCajaAstNode(ret.getReturnValue(),this);
    }

}	// end of inner class CajaAstReturnStatement




private static class CajaAstSetterProperty extends CajaAstNode implements NobaseAst.SetterProperty {

   CajaAstSetterProperty(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstSetterProperty




private static class CajaAstSimpleOperation extends CajaAstOperation implements NobaseAst.SimpleOperation {

   CajaAstSimpleOperation(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstSimpleOperation




private static class CajaAstStringLiteral extends CajaAstExpression implements NobaseAst.StringLiteral {

   CajaAstStringLiteral(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   @Override public String getValue() {
      return ((StringLiteral) getCajaNode()).getUnquotedValue();
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstStringLiteral




private static class CajaAstSwitchStatement extends CajaAstNode implements NobaseAst.SwitchStatement {

   CajaAstSwitchStatement(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstSwitchStatement




private static class CajaAstThrowStatement extends CajaAstNode implements NobaseAst.ThrowStatement {

   CajaAstThrowStatement(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstThrowStatement




private static class CajaAstTryStatement extends CajaAstNode implements NobaseAst.TryStatement {

   CajaAstTryStatement(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par); }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstTryStatement



private static class CajaAstTypeofOperation extends CajaAstOperation implements NobaseAst.TypeofOperation {

   CajaAstTypeofOperation(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstTypeofOperation



private static class CajaAstValueProperty extends CajaAstNode implements NobaseAst.ValueProperty {

   CajaAstValueProperty(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   private ValueProperty getNode()	{ return (ValueProperty) getCajaNode(); }

   @Override public NobaseAst.Expression getValueExpression() {
      return (NobaseAst.Expression) createCajaAstNode(getNode().getValueExpr(),this);
    }

   @Override public String getPropertyName() {
      return getNode().getPropertyName();
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstValueProperty




private static class CajaAstWhileLoop extends CajaAstNode implements NobaseAst.WhileLoop {

   CajaAstWhileLoop(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstWhileLoop




private static class CajaAstWithStatement extends CajaAstNode implements NobaseAst.WithStatement {

   CajaAstWithStatement(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   private WithStmt getNode()		   { return (WithStmt) getCajaNode(); }

   @Override public NobaseAst.Statement getBody() {
      return (NobaseAst.Statement) createCajaAstNode(getNode().getBody(),this);
    }

   @Override public NobaseAst.Expression getScopeObject() {
      return (NobaseAst.Expression) createCajaAstNode(getNode().getScopeObject(),this);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstWithStatement


private static class CajaAstVoidOperation extends CajaAstOperation implements NobaseAst.VoidOperation {

   CajaAstVoidOperation(ParseTreeNode pn,CajaAstNode par) {
      super(pn,par);
    }

   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }

}	// end of inner class CajaAstVoidOperation



}	// end of class NobaseCaja


/* end of NobaseCaja.java */

