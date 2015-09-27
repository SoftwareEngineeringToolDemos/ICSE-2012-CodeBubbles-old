/********************************************************************************/
/*										*/
/*		BoardMail.java							*/
/*										*/
/*	Bubbles attribute and property management mail handler			*/
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



package edu.brown.cs.bubbles.board;

import java.awt.Desktop;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;


/**
 *	This class is used to create and send email messages.
 **/

public class BoardMail implements BoardConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private static BoardMail mail_handler;


static {
   mail_handler = new BoardMail();
}



/********************************************************************************/
/*										*/
/*	Static interface							*/
/*										*/
/********************************************************************************/

/**
 *	Create a new email message being sent to the given address.
 **/

public static BoardMailMessage createMessage(String to)
{
   if (!mail_handler.ensureSetup()) return null;

   return mail_handler.createMessageImpl(to);
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BoardMail()
{ }



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private boolean ensureSetup()
{
   if (!Desktop.isDesktopSupported()) return false;
   Desktop dt = Desktop.getDesktop();
   if (!dt.isSupported(Desktop.Action.MAIL)) return false;

   return true;
}




/********************************************************************************/
/*										*/
/*	Message creation methods						*/
/*										*/
/********************************************************************************/

private MessageImpl createMessageImpl(String to)
{
   return new MessageImpl(to);
}



/********************************************************************************/
/*										*/
/*	Message implementation							*/
/*										*/
/********************************************************************************/

private static class MessageImpl implements BoardMailMessage
{
   private String mail_to;
   private String subject_text;
   private String body_text;
   private String attachment_file;

   MessageImpl() {
      mail_to = null;
      subject_text = null;
      body_text = null;
      attachment_file = null;
    }

   MessageImpl(String to) {
      this();
      mail_to = to;
    }

   public void setSubject(String s)			{ subject_text = s; }
   public void addBodyText(String t)			{ body_text = t; }
   public void addAttachment(String mime,Object what) {
      // TODO: need to set up attachment list
      if (what == null) attachment_file = null;
      else attachment_file = "'file://" + what.toString() + "'";
    }

   public void send() throws IOException {
      StringBuffer buf = new StringBuffer();
      buf.append("mailto:");
      if (mail_to != null) buf.append(encode(mail_to));
      int ct = addField(buf,"subject",subject_text,0);
      // TODO: do our own uuencoding of the body and an email attachment and see if it works
      ct = addField(buf,"body",body_text,ct);
      addField(buf,"attachment",attachment_file,ct);

      try {
	 System.err.println("Mail URI = " + buf.toString());
	 URI u = new URI(buf.toString());
	 Desktop.getDesktop().mail(u);
       }
      catch (URISyntaxException e) {
	 BoardLog.logE("BOARD","Problem generating mail uri: " + e);
	 throw new IOException("BAD URI for mail",e);
       }
      catch (UnsupportedOperationException e) {
	 throw new IOException("Mail not supported",e);
       }
   }

   private int addField(StringBuffer buf,String id,String fld,int ct) {
      if (fld == null) return ct;
      if (ct++ == 0) buf.append("?");
      else buf.append("&");
      buf.append(id);
      buf.append("=");
      buf.append(encode(fld));
      return ct;
    }

   private String encode(String fld) {
      try {
	 String s = URLEncoder.encode(fld,"UTF-8");
	 s = s.replace("+","%20");
	 return s;
       }
      catch (UnsupportedEncodingException e) {
	 return fld;
       }
    }

}	// end of inner class MessageImpl




}	// end of class BoardMail



/* end of BoardMail.java */
