/********************************************************************************/
/*										*/
/*		NobaseResolver.java						*/
/*										*/
/*	Handle symbol and type resolution for JavaScript			*/
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


import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class NobaseResolver implements NobaseConstants, NobaseAst
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private NobaseScope		global_scope;
private NobaseProject		for_project;
private static Map<String,Evaluator>   operator_evaluator;

private static Pattern	INTERNAL_NAME = Pattern.compile("_L\\d+");

static {
   operator_evaluator = new HashMap<String,Evaluator>();
   operator_evaluator.put("+",new EvalPlus());
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseResolver(NobaseProject proj,NobaseScope glbl)
{
   global_scope = glbl;
   for_project = proj;
}




/********************************************************************************/
/*										*/
/*	Remove Resolution methods						*/
/*										*/
/********************************************************************************/

void unresolve(NobaseAstNode node)
{
   UnresolveVisitor uv = new UnresolveVisitor();
   node.accept(uv);
}


private static class UnresolveVisitor extends NobaseAstVisitor {

   @Override public void postVisit(NobaseAstNode n) {
      n.clearResolve();
    }

}	// end of inner class UnresolveVisitor



/********************************************************************************/
/*										*/
/*	Resolution methods							*/
/*										*/
/********************************************************************************/

void resolveSymbols(ISemanticData isd)
{
   NobaseAstNode node = isd.getRootNode();
   unresolve(node);

   List<NobaseMessage> errors = new ArrayList<NobaseMessage>();

   ValuePass vp = new ValuePass(isd.getFileData(),errors);
   for (int i = 0; i < 5; ++i) {
      node.accept(vp);
      if (!vp.checkChanged()) break;
      vp.forceDefine();
    }

   isd.addMessages(errors);
}





/********************************************************************************/
/*										*/
/*	Value pass -- assign or update values					*/
/*										*/
/********************************************************************************/

private class ValuePass extends NobaseAstVisitor {

   private NobaseScope cur_scope;
   private List<NobaseMessage> error_list;
   private boolean change_flag;
   private NobaseValue set_lvalue;
   private String enclosing_function;
   private NobaseFile enclosing_file;
   private Stack<String> name_stack;
   private Stack<NobaseValue> function_stack;
   private boolean force_define;

   ValuePass(NobaseFile module,List<NobaseMessage> errors) {
      cur_scope = global_scope;
      error_list = errors;
      change_flag = false;
      set_lvalue = null;
      force_define = false;
      enclosing_file = module;
      enclosing_function = null;
      name_stack = new Stack<String>();
      function_stack = new Stack<NobaseValue>();
    }

   boolean checkChanged() {
      boolean rslt = change_flag;
      change_flag = false;
      return rslt;
    }

   void forceDefine()				{ force_define = true; }

   @Override public boolean visit(FileModule b) {
      cur_scope = b.getScope();
      if (cur_scope == null) {
	 cur_scope = new NobaseScope(ScopeType.FILE,global_scope);
	 for_project.setupModule(enclosing_file,cur_scope);
	 b.setScope(cur_scope);
       }
      NobaseSymbol osym = b.getDefinition();
      if (osym == null) {
	 NobaseSymbol nsym = new NobaseSymbol(for_project,enclosing_file,b,
						 enclosing_file.getModuleName(),false);
	 nsym.setBubblesName(enclosing_file.getModuleName());
	 b.setDefinition(nsym);
       }

      return true;
    }

   @Override public void endVisit(FileModule b) {
      for_project.finishModule(enclosing_file);
      cur_scope = cur_scope.getParent();
    }

   @Override public void endVisit(ArrayConstructor n) {
      List<NobaseValue> vals = new ArrayList<NobaseValue>();
      for (int i = 0; i < n.getNumElements(); ++i) {
	 NobaseValue nv = n.getElement(i).getNobaseValue();
	 vals.add(nv);
       }
      setValue(n,NobaseValue.createArrayValue(vals));
    }

   @Override public boolean visit(ArrayIndex n) {
      NobaseValue lvl = set_lvalue;
      set_lvalue = null;
      for (int i = 0; i < n.getNumOperands(); ++i) {
	 n.getOperand(i).accept(this);
       }
      NobaseValue nvl = n.getOperand(0).getNobaseValue();
      NobaseValue ivl = n.getOperand(1).getNobaseValue();      if (lvl == null) {
	 Object idxv = (ivl == null ? null : ivl.getKnownValue());
	 NobaseValue rvl = null;
	 if (nvl != null) rvl = nvl.getProperty(idxv);
	 if (rvl == null) rvl = NobaseValue.createUnknownValue();
	 setValue(n,rvl);
       }
      else {
	 Object idxv = (ivl == null ? null : ivl.getKnownValue());
	 if (idxv != null && idxv instanceof String && nvl != null) {
	    if (nvl.addProperty(idxv,lvl)) change_flag = true;
	  }
	 setValue(n,lvl);
       }
      return false;
    }

   @Override public boolean visit(AssignOperation n) {
      NobaseValue ovl = set_lvalue;
      set_lvalue = null;
      if (n.getNumOperands() > 1) {
         n.getOperand(1).accept(this);
         if (n.getOperator().equals("=")) {
            set_lvalue = n.getOperand(1).getNobaseValue();
          }
       }
      n.getOperand(0).accept(this);
      set_lvalue = ovl;
      return false;
    }

   @Override public boolean visit(Block n) {
      NobaseScope nscp = n.getScope();
      if (nscp == null) {
	 nscp = new NobaseScope(ScopeType.LOCAL,cur_scope);
	 n.setScope(nscp);
       }
      cur_scope = nscp;
      return true;
    }

   @Override public void endVisit(Block n) {
      cur_scope = cur_scope.getParent();
    }

   @Override public void endVisit(BooleanLiteral n) {
      setValue(n,NobaseValue.createBoolean(n.getValue()));
    }

   @Override public boolean visit(CatchStatement cstmt) {
      NobaseScope nscp = cstmt.getScope();
      if (nscp == null) {
	 nscp = new NobaseScope(ScopeType.BLOCK,cur_scope);
	 cstmt.setScope(nscp);
       }
      cur_scope = nscp;
      return true;
    }

   @Override public void endVisit(CatchStatement cstmt) {
      cur_scope = cur_scope.getParent();
    }

   @Override public void endVisit(CommaOperation n) {
      NobaseValue nv = n.getOperand(n.getNumOperands()-1).getNobaseValue();
      setValue(n,nv);
    }

   @Override public void endVisit(ConstructorCall n) {
      NobaseValue nv = NobaseValue.createObject();
      NobaseValue fv = n.getOperand(0).getNobaseValue();
      nv.setBaseValue(fv);
      setValue(n,nv);
      // handle new X
    }

   @Override public void endVisit(ControlOperation n) {
      evaluateOperation(n,NobaseValue.createBoolean());
    }

   @Override public boolean visit(Declaration n) {
      if (n.getInitializer() != null) {
         n.getInitializer().accept(this);
       }
      Identifier vident = n.getIdentifier();
      if (vident != null) {
         NobaseSymbol sym = n.getDefinition();
         if (sym == null) {
            NobaseSymbol nsym = new NobaseSymbol(for_project,enclosing_file,n,vident.getName(),true);
            if (enclosing_function != null) {
               setName(nsym,enclosing_function + "." + vident.getName(),cur_scope);
             }
            else {
               setName(nsym,vident.getName(),cur_scope);
             }
            
            sym = cur_scope.define(nsym);
            if (nsym != sym) {
               boolean dupok = false;
               if (n.getInitializer() != null) {
                  NobaseValue fval = n.getInitializer().getNobaseValue();
                  if (fval != null && fval.isFunction() && fval == sym.getValue()) dupok = true;
                }
               if (!dupok) duplicateDef(vident.getName(),n);
             }
            n.setDefinition(sym);
            vident.setDefinition(sym);
          }
         if (n.getInitializer() != null) {
            NobaseValue nv = n.getInitializer().getNobaseValue();
            sym.setValue(nv);
          }
       }
      return false;
    }

   @Override public void endVisit(DeleteOperation n) {
      setValue(n,NobaseValue.createAnyValue());
    }

   @Override public void endVisit(Elision n) {
      setValue(n,NobaseValue.createAnyValue());
    }

   @Override public boolean visitExpression(Expression e) {
      set_lvalue = null;
      return true;
    }

   @Override public boolean visit(FormalParameter fp) {
      Identifier fident = fp.getIdentifier();
      if (fident != null) {
         String newname = fident.getName();
         NobaseSymbol osym = fp.getDefinition();
         NobaseScope scp = cur_scope.getParent();
         NobaseSymbol psym = scp.lookup(newname);
         if (psym != null) {
            NobaseMessage msg = new NobaseMessage(ErrorSeverity.WARNING,
        	  "Parameter " + newname + " hides outside variable",
        	  fp.getStartLine(enclosing_file),fp.getStartChar(enclosing_file),
        	  fp.getEndLine(enclosing_file),fp.getEndChar(enclosing_file));
            error_list.add(msg);
          }
         if (osym == null) {
            NobaseSymbol sym = new NobaseSymbol(for_project,enclosing_file,fp,newname,true);
            setName(sym,newname,cur_scope);
            osym = cur_scope.define(sym);
            if (osym != sym) duplicateDef(fident.getName(),fp);
            if (enclosing_function != null) {
               newname = enclosing_function + "." + newname;
             }
            fp.setDefinition(osym);
            fp.getIdentifier().setDefinition(osym);
          }
       }
      return false;
    }

   @Override public void endVisit(FunctionCall n) {
      NobaseValue nv = NobaseValue.createUnknownValue();
      NobaseValue fv = n.getOperand(0).getNobaseValue();
      if (fv != null) {
         NobaseScope scp = cur_scope;
         NobaseSymbol thissym = scp.lookup("this");
         NobaseValue thisval = thissym.getValue();
         if (n.getOperand(0) instanceof MemberAccess) {
            MemberAccess ma = (MemberAccess) n.getOperand(0);
            thisval = ma.getOperand(0).getNobaseValue();
          }
         if (n.getParent() instanceof SimpleOperation) {
            SimpleOperation sop = (SimpleOperation) n.getParent();
            if (sop.getOperator().equals("new")) {
               thisval = NobaseValue.createObject();
               thisval.mergeProperties(fv);
               if (sop.getNobaseValue() == null) {
                  sop.setNobaseValue(thisval);
                }
             }
          }
   
         List<NobaseValue> args = new ArrayList<NobaseValue>();
         for (int i = 1; i < n.getNumOperands(); ++i) {
            args.add(n.getOperand(i).getNobaseValue());
          }
         nv = fv.evaluate(enclosing_file,args,thisval);
       }
      setValue(n,nv);
    }

   @Override public boolean visit(FunctionConstructor n) {
      NobaseScope defscope = cur_scope;
      String fctname = getFunctionName(n);
      NobaseValue nv = n.getNobaseValue();
      if (nv == null) {
         nv = NobaseValue.createFunction(n);
         setValue(n,nv);
       }
      function_stack.push(nv);
      if (fctname != null) {
         NobaseSymbol osym = n.getDefinition();
         if (osym == null) {
            NobaseAstNode defnode = n;
            if (defnode.getParent() instanceof Declaration)
               defnode = defnode.getParent();
            NobaseSymbol nsym = new NobaseSymbol(for_project,enclosing_file,defnode,fctname,true);
            setName(nsym,fctname,defscope);
            osym = defscope.define(nsym);
            n.setDefinition(osym);
            if (osym != nsym) {
               duplicateDef(fctname,n);
               nsym = osym;
             }
            nsym.setValue(nv);
          }
       }
   
      NobaseScope nscp = n.getScope();
      if (nscp == null) {
         NobaseValue othis = null;
         if (enclosing_function != null) {
            NobaseSymbol othissym = cur_scope.lookup("this");
            if (othissym != null) othis = othissym.getValue();
          }
         nscp = new NobaseScope(ScopeType.FUNCTION,cur_scope);
         n.setScope(nscp);
         nscp.setValue(nv);
         if (othis != null) {
            nv.mergeProperties(othis);
          }
         NobaseSymbol thissym = new NobaseSymbol(for_project,null,null,"this",true);
         thissym.setValue(nv);
         nscp.define(thissym);
       }
      cur_scope = nscp;
   
      name_stack.push(enclosing_function);
      NobaseAstNode defnd = getFunctionNode(n);
      if (fctname != null) {
         NobaseSymbol nsym = defnd.getDefinition();
         if (nsym == null) nsym = n.getDefinition();
         if (nsym == null) {
            nsym = new NobaseSymbol(for_project,enclosing_file,defnd,fctname,true);
            nsym.setValue(nv);
            setName(nsym,fctname,defscope);
          }
         if (!fctname.equals(nsym.getName()))
            cur_scope.defineAll(nsym,fctname);
         defnd.setDefinition(nsym);
         if (enclosing_function == null) enclosing_function = fctname;
         else if (fctname.contains(".")) enclosing_function = fctname;
         else enclosing_function += "." + fctname;
         Identifier id = n.getIdentifier();
         if (id != null && id.getDefinition() == null) {
            id.setDefinition(nsym);
          }
       }
   
      return true;
    }

   @Override public void endVisit(FunctionConstructor n) {
      cur_scope = cur_scope.getParent();
      enclosing_function = name_stack.pop();
      function_stack.pop();
    }

   @Override public boolean visit(Identifier id) {
      String name = id.getName();
      NobaseSymbol ref = id.getDefinition();
      if (ref == null) ref = id.getReference();
      if (ref == null) {
         ref = cur_scope.lookup(name);
         if (ref == null && force_define && !isGeneratedName(name)) {
            // see if we should create implicit definition
            NobaseScope dscope = cur_scope.getDefaultScope();
            if (dscope.getScopeType() == ScopeType.FILE ||
        	  dscope.getScopeType() == ScopeType.GLOBAL) {
               NobaseMessage msg = new NobaseMessage(ErrorSeverity.WARNING,
        	     "Implicit declaration of " + name,
        	     id.getStartLine(enclosing_file),id.getStartChar(enclosing_file),
        	     id.getEndLine(enclosing_file),id.getEndChar(enclosing_file));
               error_list.add(msg);
               ref = new NobaseSymbol(for_project,enclosing_file,id,name,false);
               setName(ref,name,dscope);
               dscope.define(ref);
             }
            else {
               dscope.setProperty(name,NobaseValue.createAnyValue());
             }
          }
         if (ref != null) {
            id.setReference(ref);
            NobaseScope dscp = ref.getDefScope();
            if (dscp != null) {
               boolean fnd = false;
               for (NobaseScope scp = cur_scope; scp != null; scp = scp.getParent()) {
                  if (scp == dscp) {
                     fnd = true;
                     break;
                   }
                }
               if (!fnd) {
        	  NobaseMessage msg = new NobaseMessage(ErrorSeverity.WARNING,
        		"Possible misuse of variable " + name + " based on lexical scope ",
        		id.getStartLine(enclosing_file),id.getStartChar(enclosing_file),
        		id.getEndLine(enclosing_file),id.getEndChar(enclosing_file));
        	  error_list.add(msg);
        	}
             }
          }
       }
      if (ref != null) {
         if (set_lvalue != null) {
            NobaseValue nv = NobaseValue.mergeValues(set_lvalue,ref.getValue());
            setValue(id,nv);
          }
         else {
            id.setNobaseValue(ref.getValue());
          }
       }
      else {
         NobaseValue nv = cur_scope.lookupValue(name);
         if (nv == null && force_define) {
            System.err.println("NOBASE: no value found for " + name + " at " +
        	  id.getStartLine(enclosing_file));
            nv = NobaseValue.createUnknownValue();
          }
         id.setNobaseValue(nv);
       }
   
      return false;
    }

   @Override public boolean visit(IfStatement n) {
      NobaseScope nscp = n.getScope();
      if (nscp == null) {
	 nscp = new NobaseScope(ScopeType.LOCAL,cur_scope);
	 n.setScope(nscp);
       }
      cur_scope = nscp;
      return true;
    }

   @Override public void endVisit(IfStatement n) {
      cur_scope = cur_scope.getParent();
    }

   @Override public void endVisit(InOperation n) {
      setValue(n,NobaseValue.createBoolean());
    }

   @Override public void endVisit(IntegerLiteral n) {
      setValue(n,NobaseValue.createNumber(n.getValue()));
    }

   @Override public boolean visit(MemberAccess n) {
      NobaseValue lval = set_lvalue;
      set_lvalue = null;
      n.getOperand(0).accept(this);
      set_lvalue = lval;
      
      NobaseScope scp = n.getScope();
      if (scp == null) {
         scp = new NobaseScope(ScopeType.MEMBER,cur_scope);
         n.setScope(scp);
       }              
      scp.setValue(n.getOperand(0).getNobaseValue());      
      cur_scope = scp;
      if (set_lvalue != null) {
         String nm = n.getMemberName();
         cur_scope.setProperty(nm,set_lvalue);
         setValue(n,set_lvalue);
       }
      else {
         n.getOperand(1).accept(this);
         setValue(n,n.getOperand(1).getNobaseValue());
       }
      cur_scope = cur_scope.getParent();
      return false;
    }


   @Override public void endVisit(NullLiteral n) {
      setValue(n,NobaseValue.createNull());
    }

   @Override public boolean visit(ObjectConstructor n) {
      NobaseScope scp = new NobaseScope(ScopeType.OBJECT,cur_scope);
      scp.setValue(NobaseValue.createObject());
      n.setScope(scp);

      cur_scope = scp;
      for (int i = 0; i < n.getNumElements(); ++i) {
	 NobaseAstNode nn = n.getElement(i);
	 nn.accept(this);
       }
      cur_scope = scp.getParent();
      return false;
    }

   @Override public void endVisit(RealLiteral n) {
      setValue(n,NobaseValue.createNumber(n.getValue()));
    }

   @Override public void endVisit(RegexpLiteral n) {
      NobaseSymbol regex = global_scope.lookup("RegExp");
      NobaseValue nv = NobaseValue.createObject();
      if (regex != null) nv = regex.getValue();
      setValue(n,nv);
    }

   @Override public void endVisit(Reference n) {
      Identifier id = n.getIdentifier();
      setValue(n,id.getNobaseValue());
    }

   @Override public void endVisit(ReturnStatement n) {
      Expression exp = n.getExpression();
      if (exp != null && function_stack.size() > 0) {
	 NobaseValue fvalue = function_stack.peek();
	 fvalue.setReturnValue(exp.getNobaseValue());
       }
    }

   @Override public void endVisit(SimpleOperation n) {
      evaluateOperation(n,null);
    }

   @Override public void endVisit(StringLiteral n) {
      setValue(n,NobaseValue.createString(n.getValue()));
    }

   @Override public void endVisit(TypeofOperation n) {
      setValue(n,NobaseValue.createString());
    }

   @Override public boolean visit(ValueProperty n) {
      Expression ex = n.getValueExpression();
      ex.accept(this);
      NobaseValue nv = ex.getNobaseValue();
      cur_scope.setProperty(n.getPropertyName(),nv);
      return false;
    }

   @Override public void endVisit(VoidOperation n) {
      setValue(n,NobaseValue.createAnyValue());
    }

   @Override public boolean visit(WithStatement wstmt) {
      wstmt.getScopeObject().accept(this);
      NobaseValue nv = wstmt.getNobaseValue();
      NobaseScope nscp = wstmt.getScope();
      if (nscp == null) {
	 nscp = new NobaseScope(ScopeType.WITH,cur_scope);
	 wstmt.setScope(nscp);
       }
      cur_scope = nscp;
      cur_scope.setValue(nv);
      wstmt.getBody().accept(this);
      cur_scope = cur_scope.getParent();
      return false;
    }


   private void setValue(NobaseAstNode n,NobaseValue v) {
      if (v == null) v = NobaseValue.createUnknownValue();
      change_flag |= n.setNobaseValue(v);
    }

   private void duplicateDef(String nm,NobaseAstNode n) {
      NobaseMessage msg = new NobaseMessage(ErrorSeverity.WARNING,
	    "Duplicate defintion of " + nm,
	    n.getStartLine(enclosing_file),n.getStartChar(enclosing_file),n.getEndLine(enclosing_file),n.getEndChar(enclosing_file));
      error_list.add(msg);
    }

   private void setName(NobaseSymbol sym,String name,NobaseScope dscope) {
      String tnm = enclosing_file.getModuleName();
      String qnm = tnm + "." + name;
      if (enclosing_function != null) {
         qnm = tnm + "." + enclosing_function + "." + name;
       }
      if (sym != null) sym.setBubblesName(qnm);
      if (dscope == null) dscope = cur_scope;
      if (sym != null) sym.setDefScope(dscope);
    }

   private String getFunctionName(FunctionConstructor fc) {
      if (fc.getIdentifier() != null &&
            fc.getIdentifier().getName() != null &&
            !isGeneratedName(fc.getIdentifier().getName()))
         return fc.getIdentifier().getName();
   
      if (fc.getParent() instanceof Declaration) {
         Declaration d = (Declaration) fc.getParent();
         return d.getIdentifier().getName();
       }
      if (fc.getParent() instanceof AssignOperation) {
         AssignOperation ao = (AssignOperation) fc.getParent();
         if (ao.getOperand(1) != fc) return null;
         if (ao.getOperand(0) instanceof MemberAccess) {
            MemberAccess ma = (MemberAccess) ao.getOperand(0);
            String m1 = getIdentName(ma.getOperand(1));
            if (m1 == null) return null;
            String m0 = getIdentName(ma.getOperand(0));
            if (m0 != null && m0.equals("this") && enclosing_function != null) {
               return m1;
             }
            else if (m0 == null && ma.getOperand(0) instanceof MemberAccess) {
               MemberAccess ma1 = (MemberAccess) ma.getOperand(0);
               String k1 = getIdentName(ma1.getOperand(0));
               String k2 = getIdentName(ma1.getOperand(1));
               if (k2 != null && k2.equals("prototype") && k1 != null) {
                  return k1 + "." + m1;
                }
             }
            else if (m0 != null && m0.equals("exports") && enclosing_function == null) {
               return m1;
             }
          }
       }
      if (fc.getIdentifier() != null)
         return fc.getIdentifier().getName();
      
      return null;
    }

   private NobaseAstNode getFunctionNode(FunctionConstructor fc) {
      if (fc.getParent() instanceof FunctionDeclaration) return fc.getParent();
      if (fc.getIdentifier() != null &&
            fc.getIdentifier().getName() != null &&
            !isGeneratedName(fc.getIdentifier().getName())) return fc;
      if (fc.getParent() instanceof Declaration) return fc.getParent();
      if (fc.getParent() instanceof AssignOperation) return fc.getParent();
      if (fc.getIdentifier() != null &&
            fc.getIdentifier().getName() != null)
         return fc;
      
      return null;
    }

   private String getIdentName(Expression e) {
      if (e instanceof Identifier) {
         return ((Identifier) e).getName();
       }
      else if (e instanceof Reference) {
         return ((Reference) e).getIdentifier().getName();
       }
      return null;
    }

   private NobaseValue evaluateOperation(Operation n,NobaseValue dflt) {
      NobaseValue nv = dflt;
      String op = n.getOperator();
      Evaluator ev = operator_evaluator.get(op);
      if (ev != null) {
         List<NobaseValue> args = new ArrayList<NobaseValue>();
         for (int i = 0; i < n.getNumOperands(); ++i) {
            args.add(n.getOperand(i).getNobaseValue());
          }
         nv = ev.evaluate(enclosing_file,args,NobaseValue.createUndefined());
       }
      if (nv == null) nv = NobaseValue.createAnyValue();
      setValue(n,nv);
      return nv;
    }

}	// end of inner class ValuePass




/********************************************************************************/
/*										*/
/*	Operator implementations						*/
/*										*/
/********************************************************************************/

private static class EvalPlus implements Evaluator {

   @Override public NobaseValue evaluate(NobaseFile file,List<NobaseValue> args,
           NobaseValue thisval) {
      if (args.size() == 0) return null;
      if (args.size() == 1) return args.get(0);
   
      List<Object> vals = new ArrayList<Object>();
      boolean havestring = false;
      boolean allnumber = true;
      for (NobaseValue nv : args) {
         if (nv == null) return null;
         Object o = nv.getKnownValue();
         if (o == null) return null;
         if (o == KnownValue.UNKNOWN) return null;
         if (o == KnownValue.ANY) return null;
         if (o == KnownValue.UNDEFINED) return null;
         if (o == KnownValue.NULL) o = null;
         vals.add(o);
         if (!(o instanceof Number)) allnumber = false;
         if (o instanceof String) havestring = true;
       }
      if (havestring) {
         StringBuffer buf = new StringBuffer();
         for (Object o : vals) {
            buf.append(o);
          }
         return NobaseValue.createString(buf.toString());
       }
      if (allnumber) {
         double v = 0;
         for (Object o : vals) {
            v += ((Number) o).doubleValue();
          }
         return NobaseValue.createNumber(v);
       }
      return null;
    }

}	// end of inner class EvalPlus



/********************************************************************************/
/*										*/
/*	Helper functions							*/
/*										*/
/********************************************************************************/

static boolean isGeneratedName(NobaseSymbol ns)
{
   return isGeneratedName(ns.getName());
}


static boolean isGeneratedName(String name)
{
   if (name == null) return false;
   Matcher m = INTERNAL_NAME.matcher(name);
   return m.matches();
}





}	// end of class NobaseResolver




/* end of NobaseResolver.java */

