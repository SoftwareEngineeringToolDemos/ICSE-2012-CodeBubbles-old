/********************************************************************************/
/*                                                                              */
/*              NobaseType.java                                                 */
/*                                                                              */
/*      description of class                                                    */
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



class NobaseType implements NobaseConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String          type_name;
   
private static NobaseType undef_type = new UndefinedType();
private static NobaseType null_type = new NullType();
private static NobaseType bool_type = new BooleanType();
private static NobaseType number_type = new NumberType();
private static NobaseType string_type = new StringType();
private static NobaseType any_type = new AnyType();

   

/********************************************************************************/
/*                                                                              */
/*      Static creation methods                                                 */
/*                                                                              */
/********************************************************************************/

static NobaseType createUndefined()             { return undef_type; }

static NobaseType createNull()                  { return null_type; }

static NobaseType createBoolean()               { return bool_type; }

static NobaseType createNumber()                { return number_type; }

static NobaseType createString()                { return string_type; }

static NobaseType createObject()                { return new ObjectType(); }

static NobaseType createList()                  { return new ListType(); }

static NobaseType createFunction()              { return new CompletionType(); }

static NobaseType createAnyType()               { return any_type; }




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private NobaseType(String nm)
{
   type_name = nm;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getName()                                { return type_name; }



/********************************************************************************/
/*                                                                              */
/*      Undefined type                                                          */
/*                                                                              */
/********************************************************************************/

private static class UndefinedType extends NobaseType {
   
   UndefinedType() {
      super("undefined");
    }
   
}       // end of inner class UndefinedType



/********************************************************************************/
/*                                                                              */
/*      Null type                                                               */
/*                                                                              */
/********************************************************************************/

private static class NullType extends NobaseType {
   
   NullType() {
      super("null");
    }
   
}       // end of inner class NullType



/********************************************************************************/
/*                                                                              */
/*      Boolean type                                                            */
/*                                                                              */
/********************************************************************************/

private static class BooleanType extends NobaseType {
   
   BooleanType() {
      super("boolean");
    }
   
}       // end of inner class BooleanType



/********************************************************************************/
/*                                                                              */
/*      String Type                                                             */
/*                                                                              */
/********************************************************************************/

private static class StringType extends NobaseType {
   
   StringType() {
      super("string");
    }
   
}       // end of inner class StringType


/********************************************************************************/
/*                                                                              */
/*      Numeric Types                                                           */
/*                                                                              */
/********************************************************************************/

private static class NumberType extends NobaseType {
   
   NumberType() {
      super("number");
    }
   
}       // end of inner class NumberType



/********************************************************************************/
/*                                                                              */
/*      Object Types                                                            */
/*                                                                              */
/********************************************************************************/

private static class ObjectType extends NobaseType {
   
   ObjectType() {
      super("object");
    }
   
}       // end of inner class ObjectType



/********************************************************************************/
/*                                                                              */
/*      Reference Type                                                          */
/*                                                                              */
/********************************************************************************/

private static class ReferenceType extends NobaseType {
   
   ReferenceType() {
      super("reference");
    }
   
}       // end of inner class ReferenceType



/********************************************************************************/
/*                                                                              */
/*      List Type                                                               */
/*                                                                              */
/********************************************************************************/

private static class ListType extends NobaseType {
   
   ListType() {
      super("list");
    }
   
}       // end of inner class ListType




/********************************************************************************/
/*                                                                              */
/*      Completion type                                                         */
/*                                                                              */
/********************************************************************************/

private static class CompletionType extends NobaseType {
   
   CompletionType() {
      super("function");
    }
   
}       // end of inner class CompletionType




/********************************************************************************/
/*                                                                              */
/*      Property types                                                          */
/*                                                                              */
/********************************************************************************/

private static class PropertyDescriptorType extends NobaseType {
   
   PropertyDescriptorType() {
      super("property_descriptor");
    }
   
}       // end of inner class PropertyDescriptorType



private static class PropertyIdentifierType extends NobaseType {
   
   PropertyIdentifierType() {
      super("property_idenfifier");
    }
   
}       // end of inner class PropertyIdentifierTYpe


/********************************************************************************/
/*                                                                              */
/*      Any Type                                                                */
/*                                                                              */
/********************************************************************************/

private static class AnyType extends NobaseType {
   
   AnyType() {
      super("*ANY*");
    }
   
}       // end of inner class AnyType


}       // end of class NobaseType




/* end of NobaseType.java */

