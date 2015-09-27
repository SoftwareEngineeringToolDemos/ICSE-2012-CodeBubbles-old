/********************************************************************************/
/*										*/
/*		BoardMailMessage.java						*/
/*										*/
/*	Bubbles attribute and property management mail handler			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.board;

import java.io.IOException;



/**
 *	This class represents a potential e-mail message that can be set up and
 *	sent.
 **/

public interface BoardMailMessage {

/**
 *	set the subject line of the message
 **/

   void setSubject(String subject);



/**
 *	Add text to the body of the message.  Text is concatentated to any previous
 *	text added using this method.
 **/

   void addBodyText(String text);



/**
 *	Add an attachment to the message.  Right now this doesn't work on any mailer
 *	except outlook and should not be used.	Moreover, even there only one attachment
 *	is supported.  DO NOT USE.
 **/

   void addAttachment(String mine,Object what);

/**
 *	Send this email message.  An exception is thrown if there is a problem with
 *	the send.
 **/

   void send() throws IOException;



}	// end of interface BoardMailMessage




/* end of BoardMailMessage.java */
