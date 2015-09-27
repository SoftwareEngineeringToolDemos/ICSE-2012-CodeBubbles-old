/********************************************************************************/
/*										*/
/*		NobaseAst.java							*/
/*										*/
/*	Abstract AST Definitions for NOBASE javascript				*/
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


import java.util.List;


interface NobaseAst extends NobaseConstants
{

   interface NobaseAstNode {
      void setReference(NobaseSymbol s);
      NobaseSymbol getReference();
      void setDefinition(NobaseSymbol s);
      NobaseSymbol getDefinition();
      void setScope(NobaseScope s);
      NobaseScope getScope();
      boolean setNobaseValue(NobaseValue t);
      NobaseValue getNobaseValue();
      void setNobaseName(NobaseName n);
      NobaseName getNobaseName();
   
      void clearResolve();
      void accept(NobaseAstVisitor visitor);
   
      int getStartLine(NobaseFile nf);
      int getStartChar(NobaseFile nf);
      int getEndLine(NobaseFile nf);
      int getEndChar(NobaseFile nf);
      int getStartPosition(NobaseFile nf);
      int getEndPosition(NobaseFile nf);
      int getExtendedStartPosition(NobaseFile nf);
      int getExtendedEndPosition(NobaseFile nf);
   
      NobaseAstNode getParent();
      int getNumChildren();
      NobaseAstNode getChild(int i);
      int getIndexInParent();
      
      String dumpTree(NobaseFile nf);
    }

   //	Group nodes
   interface Expression extends NobaseAstNode {
      boolean isLeftHandSide();
   }
   interface Operation extends Expression {
      Expression getOperand(int i);
      int getNumOperands();
      String getOperator();
    }

   interface Statement extends NobaseAstNode		{ }



   interface ArrayConstructor extends Expression {
      Expression getElement(int i);
      int getNumElements();
    }

   interface ArrayIndex extends Operation { }

   interface AssignOperation extends Operation { }

   interface Block extends Statement { }

   interface BooleanLiteral extends Expression { 
       Boolean getValue();
    }

   interface BreakStatement extends Statement { }

   interface CaseStatement extends Statement { }

   interface CatchStatement extends Statement { }

   interface CommaOperation extends Operation { }

   interface ContinueStatement extends Statement { }

   interface ConstructorCall extends Operation { }

   interface ControlOperation extends Operation { }

   interface DebuggerStatement extends Statement { }

   interface Declaration extends Statement {
      Identifier getIdentifier();
      Expression getInitializer();
    }

   interface DeleteOperation extends Operation { }

   interface DefaultCaseStatement extends Statement { }

   interface Directive extends NobaseAstNode { }

   interface DirectivePrologue extends NobaseAstNode { }

   interface DoWhileLoop extends Statement { }

   interface Elision extends Operation { }

   interface ExpressionStatement extends Statement {
      Expression getExpression();
   }

   interface FileModule extends NobaseAstNode { 
      Block getBlock();
   }
   
   interface FinallyStatement extends Statement { }

   interface ForEachLoop extends Statement { }

   interface ForLoop extends Statement { }

   interface FormalParameter extends NobaseAstNode {
      Identifier getIdentifier();
    }

   interface FunctionCall extends Operation { }

   interface FunctionConstructor extends Expression {
      Block getBody();
      Identifier getIdentifier();
      List<FormalParameter> getParameters();
    }

   interface FunctionDeclaration extends Statement { }

   interface GetterProperty extends NobaseAstNode { }

   interface Identifier extends NobaseAstNode {
      String getName();
    }

   interface IfStatement extends Statement { }

   interface InOperation extends Operation { }

   interface IntegerLiteral extends Expression {
      Number getValue();
    }

   interface LabeledStatement extends Statement { }

   interface MemberAccess extends Operation { 
      String getMemberName();
   }

   interface MultiDeclaration extends Statement { }

   interface NoopStatement extends Statement { }

   interface NullLiteral extends Expression { }

   interface ObjectConstructor extends Expression {
      int getNumElements();
      NobaseAstNode getElement(int i);
   }

   interface PlainModule extends NobaseAstNode { }

   interface RealLiteral extends Expression { 
      Number getValue();
    }

   interface Reference extends Expression {
      Identifier getIdentifier();
      boolean isLeftHandSide();
    }

   interface RegexpLiteral extends Expression { }

   interface ReturnStatement extends Statement {
      Expression getExpression();
   }

   interface SetterProperty extends NobaseAstNode { }

   interface SimpleOperation extends Operation { }

   interface StringLiteral extends Expression { 
      String getValue();
    }

   interface SwitchStatement extends Statement { }

   interface ThrowStatement extends Statement { }

   interface TryStatement extends Statement { }

   interface TypeofOperation extends Operation { }

   interface ValueProperty extends NobaseAstNode { 
      Expression getValueExpression();
      String getPropertyName();
   }

   
   interface VoidOperation extends Operation { }

   interface WhileLoop extends Statement { }

   interface WithStatement extends Statement {
      Statement getBody();
      Expression getScopeObject();
   }

}	// end of interface NobaseAst




/* end of NobaseAst.java */
