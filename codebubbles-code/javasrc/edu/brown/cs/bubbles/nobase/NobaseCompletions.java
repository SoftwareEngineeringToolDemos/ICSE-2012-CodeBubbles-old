/********************************************************************************/
/*                                                                              */
/*              NobaseCompletions.java                                          */
/*                                                                              */
/*      Handle finding completions                                              */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.nobase;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.*;



class NobaseCompletions implements NobaseConstants, NobaseAst
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ISemanticData   semantic_data;
private NobaseFile      current_file;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

NobaseCompletions(ISemanticData isd)
{
   semantic_data = isd;
   current_file = isd.getFileData();
}



/********************************************************************************/
/*                                                                              */
/*      Work methods                                                           */
/*                                                                              */
/********************************************************************************/

void findCompletions(int offset,IvyXmlWriter xw)
{
   FindAstLocation finder = new FindAstLocation(offset);
   semantic_data.getRootNode().accept(finder);
   NobaseAstNode node = finder.getRelevantNode();
   if (node == null) return;
   NobaseScope scp = findScope(node);
   if (scp == null) return;
   
   Set<NobaseSymbol> syms = new HashSet<NobaseSymbol>();
   int spos = node.getStartPosition(current_file);
   int epos = node.getEndPosition(current_file);

   if (node instanceof NobaseAst.Identifier) {
      String pfx = null;
      Identifier id = (Identifier) node;
      pfx = id.getName();
      int sidx = id.getStartPosition(current_file);
      int eidx = id.getEndPosition(current_file);
      if (eidx != offset) {
         pfx = pfx.substring(0,offset-sidx);
       }
      while (scp != null) {
         for (NobaseSymbol sym : scp.getDefinedNames()) {
            if (pfx == null || sym.getName().startsWith(pfx)) {
               if (pfx == null || !sym.getName().equals(pfx))
                  syms.add(sym);
            }
         }
         if (scp.getScopeType() == ScopeType.MEMBER) break;
         scp = scp.getParent();
      }
   }
   else if (node instanceof NobaseAst.MemberAccess) {
      spos = epos = offset;
      for (NobaseSymbol sym : scp.getDefinedNames()) {
         syms.add(sym);
      }
   }
   
   xw.begin("COMPLETIONS");
   
   for (NobaseSymbol sym : syms) {
      xw.begin("COMPLETION");
      xw.field("KIND","OTHER");
      xw.field("NAME",sym.getName());
      xw.field("TEXT",sym.getName());
      xw.field("REPLACE_START",spos);
      xw.field("REPLACE_END",epos);
      xw.field("RELVEANCE",1);
      xw.end("COMPLETION");
   }
      
   xw.end("COMPLETIONS");
}




/********************************************************************************/
/*                                                                              */
/*      Ast node finder                                                         */
/*                                                                              */
/********************************************************************************/

private class FindAstLocation extends NobaseAstVisitor {
   
   private int for_offset;
   private NobaseAstNode inner_node;
   
   FindAstLocation(int off) {
      for_offset = off;
      inner_node = null;
    }
   
   NobaseAstNode getRelevantNode()              { return inner_node; }
   
   @Override public boolean preVisit2(NobaseAstNode n) {
      int soff = n.getStartPosition(current_file);
      int eoff = n.getEndPosition(current_file);
      if (eoff < for_offset) return false;
      if (soff > for_offset) return false;
      inner_node = n;
      return true;
    }
   
}	// end of inner class FindAstLocation


/********************************************************************************/
/*                                                                              */
/*      Find scope for ast node                                                 */
/*                                                                              */
/********************************************************************************/

NobaseScope findScope(NobaseAstNode n) 
{
   while (n != null) {
      NobaseScope scp = n.getScope();
      if (scp != null) return scp;
      n = n.getParent();
    }
   return null;
}

}       // end of class NobaseCompletions




/* end of NobaseCompletions.java */

