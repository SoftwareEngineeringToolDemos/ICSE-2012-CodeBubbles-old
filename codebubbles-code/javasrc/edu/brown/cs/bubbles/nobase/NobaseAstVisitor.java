/********************************************************************************/
/*										*/
/*		NobaseAstVistor.java						*/
/*										*/
/*	Visitor for our AST nodes						*/
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


class NobaseAstVisitor implements NobaseConstants
{
   boolean visitStatement(NobaseAst.Statement node)         { return true; }
   void endVisitStatement(NobaseAst.Statement node)         { }
   boolean visitExpression(NobaseAst.Expression node)       { return true; }
   void endVisitExpression(NobaseAst.Expression node)       { }
   boolean visitOperation(NobaseAst.Operation node)         { return visitExpression(node); }
   void endVisitOperation(NobaseAst.Operation node)         { endVisitExpression(node); } 
   
   boolean visit(NobaseAst.ArrayConstructor node)	    { return visitExpression(node); }
   void endVisit(NobaseAst.ArrayConstructor node)	    { endVisitExpression(node); }
   boolean visit(NobaseAst.ArrayIndex node)	            { return visitOperation(node); }
   void endVisit(NobaseAst.ArrayIndex node)	            { endVisitOperation(node); }
   boolean visit(NobaseAst.AssignOperation node)	    { return visitOperation(node); }
   void endVisit(NobaseAst.AssignOperation node)	    { endVisitOperation(node); }
   boolean visit(NobaseAst.Block node)			    { return visitStatement(node); }
   void endVisit(NobaseAst.Block node)			    { endVisitStatement(node); }
   boolean visit(NobaseAst.BooleanLiteral node) 	    { return visitExpression(node); }
   void endVisit(NobaseAst.BooleanLiteral node) 	    { endVisitExpression(node); }
   boolean visit(NobaseAst.BreakStatement node) 	    { return visitStatement(node); }
   void endVisit(NobaseAst.BreakStatement node) 	    { endVisitStatement(node); }
   boolean visit(NobaseAst.CaseStatement node)		    { return visitStatement(node); }
   void endVisit(NobaseAst.CaseStatement node)		    { endVisitStatement(node); }
   boolean visit(NobaseAst.CatchStatement node) 	    { return visitStatement(node); }
   void endVisit(NobaseAst.CatchStatement node) 	    { endVisitStatement(node); }
   boolean visit(NobaseAst.CommaOperation node)	    { return visitOperation(node); }
   void endVisit(NobaseAst.CommaOperation node)	    { endVisitOperation(node); } 
   boolean visit(NobaseAst.ContinueStatement node)	    { return visitStatement(node); }
   void endVisit(NobaseAst.ContinueStatement node)	    { endVisitStatement(node); }
   boolean visit(NobaseAst.ConstructorCall node)	    { return visitExpression(node); }
   void endVisit(NobaseAst.ConstructorCall node)	    { endVisitExpression(node); }  
   boolean visit(NobaseAst.ControlOperation node)	    { return visitOperation(node); }
   void endVisit(NobaseAst.ControlOperation node)	    { endVisitOperation(node); }
   boolean visit(NobaseAst.DebuggerStatement node)	    { return visitStatement(node); }
   void endVisit(NobaseAst.DebuggerStatement node)	    { endVisitStatement(node); }
   boolean visit(NobaseAst.Declaration node)	            { return visitStatement(node); }
   void endVisit(NobaseAst.Declaration node)	            { endVisitStatement(node); } 
   boolean visit(NobaseAst.DefaultCaseStatement node)	    { return visitStatement(node); }
   void endVisit(NobaseAst.DefaultCaseStatement node)	    { endVisitStatement(node); }
   boolean visit(NobaseAst.DeleteOperation node)	    { return visitOperation(node); }
   void endVisit(NobaseAst.DeleteOperation node)	    { endVisitOperation(node); }
   boolean visit(NobaseAst.Directive node)		    { return true; }
   void endVisit(NobaseAst.Directive node)		    { }
   boolean visit(NobaseAst.DirectivePrologue node)	    { return true; }
   void endVisit(NobaseAst.DirectivePrologue node)	    { }
   boolean visit(NobaseAst.DoWhileLoop node)		    { return visitStatement(node); }
   void endVisit(NobaseAst.DoWhileLoop node)		    { endVisitStatement(node); }
   boolean visit(NobaseAst.Elision node)		    { return visitOperation(node); }
   void endVisit(NobaseAst.Elision node)		    { endVisitOperation(node); }
   boolean visit(NobaseAst.ExpressionStatement node)	    { return visitStatement(node); }
   void endVisit(NobaseAst.ExpressionStatement node)	    { endVisitStatement(node); }
   boolean visit(NobaseAst.FileModule node)	            { return true; }
   void endVisit(NobaseAst.FileModule node)	            { }
   boolean visit(NobaseAst.FinallyStatement node)	    { return visitStatement(node); }
   void endVisit(NobaseAst.FinallyStatement node)	    { endVisitStatement(node); }
   boolean visit(NobaseAst.ForEachLoop node)		    { return visitStatement(node); }
   void endVisit(NobaseAst.ForEachLoop node)		    { endVisitStatement(node);}
   boolean visit(NobaseAst.ForLoop node)		    { return visitStatement(node); }
   void endVisit(NobaseAst.ForLoop node)		    { endVisitStatement(node); }
   boolean visit(NobaseAst.FormalParameter node)	    { return true; }
   void endVisit(NobaseAst.FormalParameter node)	    { }
   boolean visit(NobaseAst.FunctionCall node)	            { return visitExpression(node); }
   void endVisit(NobaseAst.FunctionCall node)	            { endVisitExpression(node); }
   boolean visit(NobaseAst.FunctionConstructor node)	    { return visitExpression(node); }
   void endVisit(NobaseAst.FunctionConstructor node)	    { endVisitExpression(node); }
   boolean visit(NobaseAst.FunctionDeclaration node)	    { return visitStatement(node); }
   void endVisit(NobaseAst.FunctionDeclaration node)	    { endVisitStatement(node); }
   boolean visit(NobaseAst.GetterProperty node) 	    { return true; }
   void endVisit(NobaseAst.GetterProperty node) 	    { }
   boolean visit(NobaseAst.Identifier node)		    { return true; }
   void endVisit(NobaseAst.Identifier node)		    { }
   boolean visit(NobaseAst.IfStatement node)		    { return visitStatement(node); }
   void endVisit(NobaseAst.IfStatement node)		    { endVisitStatement(node); }
   boolean visit(NobaseAst.InOperation node)	            { return visitOperation(node); }
   void endVisit(NobaseAst.InOperation node)	            { endVisitOperation(node); }
   boolean visit(NobaseAst.IntegerLiteral node) 	    { return visitExpression(node); }
   void endVisit(NobaseAst.IntegerLiteral node) 	    { endVisitExpression(node); }
   boolean visit(NobaseAst.LabeledStatement node)	    { return visitStatement(node); }
   void endVisit(NobaseAst.LabeledStatement node)	    { endVisitStatement(node); }
   boolean visit(NobaseAst.MemberAccess node)	            { return visitOperation(node); }
   void endVisit(NobaseAst.MemberAccess node)	            { endVisitOperation(node); }
   boolean visit(NobaseAst.MultiDeclaration node)	    { return visitStatement(node); }
   void endVisit(NobaseAst.MultiDeclaration node)	    { endVisitStatement(node); }
   boolean visit(NobaseAst.NoopStatement node)		    { return visitStatement(node); }
   void endVisit(NobaseAst.NoopStatement node)		    { endVisitStatement(node); }
   boolean visit(NobaseAst.NullLiteral node)		    { return visitExpression(node); }
   void endVisit(NobaseAst.NullLiteral node)		    { endVisitExpression(node); }
   boolean visit(NobaseAst.ObjectConstructor node)	    { return visitExpression(node); }
   void endVisit(NobaseAst.ObjectConstructor node)	    { endVisitExpression(node); }
   boolean visit(NobaseAst.PlainModule node)		    { return true; }
   void endVisit(NobaseAst.PlainModule node)		    { }
   boolean visit(NobaseAst.RealLiteral node)		    { return visitExpression(node); }
   void endVisit(NobaseAst.RealLiteral node)		    { endVisitExpression(node); }
   boolean visit(NobaseAst.Reference node)		    { return visitExpression(node); }
   void endVisit(NobaseAst.Reference node)		    { endVisitExpression(node); }
   boolean visit(NobaseAst.RegexpLiteral node)		    { return visitExpression(node); }
   void endVisit(NobaseAst.RegexpLiteral node)		    { endVisitExpression(node); }
   boolean visit(NobaseAst.ReturnStatement node)	    { return visitStatement(node); }
   void endVisit(NobaseAst.ReturnStatement node)	    { endVisitStatement(node); }
   boolean visit(NobaseAst.SetterProperty node) 	    { return true; }
   void endVisit(NobaseAst.SetterProperty node) 	    { }
   boolean visit(NobaseAst.SimpleOperation node)	    { return visitOperation(node); }
   void endVisit(NobaseAst.SimpleOperation node)	    { endVisitOperation(node); }
   boolean visit(NobaseAst.StringLiteral node)		    { return visitExpression(node); }
   void endVisit(NobaseAst.StringLiteral node)		    { endVisitExpression(node); }
   boolean visit(NobaseAst.SwitchStatement node)	    { return visitStatement(node); }
   void endVisit(NobaseAst.SwitchStatement node)	    { endVisitStatement(node); }
   boolean visit(NobaseAst.ThrowStatement node) 	    { return visitStatement(node); }
   void endVisit(NobaseAst.ThrowStatement node) 	    { endVisitStatement(node); }
   boolean visit(NobaseAst.TryStatement node)		    { return visitStatement(node); }
   void endVisit(NobaseAst.TryStatement node)		    { endVisitStatement(node); }
   boolean visit(NobaseAst.TypeofOperation node)	    { return visitOperation(node); }
   void endVisit(NobaseAst.TypeofOperation node)	    { endVisitOperation(node); }
   boolean visit(NobaseAst.ValueProperty node)		    { return true; }
   void endVisit(NobaseAst.ValueProperty node)		    { }
   boolean visit(NobaseAst.VoidOperation node)	            { return visitOperation(node); }
   void endVisit(NobaseAst.VoidOperation node)	            { endVisitOperation(node); }
   boolean visit(NobaseAst.WhileLoop node)		    { return visitStatement(node); }
   void endVisit(NobaseAst.WhileLoop node)		    { endVisitStatement(node); }
   boolean visit(NobaseAst.WithStatement node)		    { return visitStatement(node); }
   void endVisit(NobaseAst.WithStatement node)		    { endVisitStatement(node); }

   void preVisit(NobaseAst.NobaseAstNode node)				{ }
   boolean preVisit2(NobaseAst.NobaseAstNode node) {
      preVisit(node);
      return true;
    }
   void postVisit(NobaseAst.NobaseAstNode node)				{ }

}	// end of class NobaseAstVisitor




/* end of NobaseAstVisitor.java */
