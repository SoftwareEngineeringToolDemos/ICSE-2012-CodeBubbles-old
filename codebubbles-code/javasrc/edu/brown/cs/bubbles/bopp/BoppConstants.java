/********************************************************************************/
/*										*/
/*		BoppConstants.java						*/
/*										*/
/*	Conststand for the option panel 					*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Alexander Hills		      */
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


package edu.brown.cs.bubbles.bopp;

import edu.brown.cs.bubbles.board.BoardFont;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.BorderFactory;
import javax.swing.border.Border;

import java.awt.*;
import java.util.Collection;
import java.util.regex.Pattern;


/**
 * Interface defining constants for Bopp
 *
 *
 * @author ahills
 *
 */

public interface BoppConstants {



/********************************************************************************/
/*										*/
/*	Enumerations for different preferent types				*/
/*										*/
/********************************************************************************/

/**
 * Enumeration for different types of preferences that can be set
 */
enum OptionType {
   NONE,
   COLOR,
   INTEGER,
   DOUBLE,
   STRING,
   BOOLEAN,
   FONT,
   DIVIDER,
   COMBO,
   DIMENSION
}



/**
 * Enumeration for various tabs (Remember to add mappings to BoppPanelHandler.mkeTabMaps
 * if you add tabs)
 */
enum TabName {
   POPULAR,
   BUBBLE_OPTIONS,
   FONT_OPTIONS,
   TEXT_EDITOR_OPTIONS,
   SYSTEM_OPTIONS,
   VISUALIZATIONS,
   SEARCH_OPTIONS,
   USER_RECENT_OPTIONS,
   SEARCH,
   ALL
}




/********************************************************************************/
/*										*/
/*	Strings for different settings						*/
/*										*/
/********************************************************************************/

/**
 * Name of preferences xml
 */
String	  PREFERENCES_XML_FILENAME_NEW	= "preferences.new.xml";


/**
 * Divider for splitting recent options
 */
String	  RECENT_OPTIONS_SPACER 	= "&";


/**
 * Name of the popular tab
 */
String	  BOPP_FILE_NAME		= "Bopp";


/**
 * Name of the popular tab
 */
String	  POPULAR_TAB_STRING		= "Popular Options";


/**
 * Name of the popular tab
 */
String	  VISUALIZATIONS_TAB_STRING	= "Visualization Options";


/**
 * Name of the bubble options tab
 */
String	  BUBBLE_OPTIONS_STRING 	= "Bubble Config";


/**
 * Name of the font tab
 */
String	  FONT_OPTIONS_STRING		= "Font Options";

/**
 * Name of the text editor options tab
 */
String	  TEXT_EDITOR_STRING		= "Text Editor Options";


/**
 * Name of the system options tab
 */
String	  SYSTEM_OPTIONS_STRING 	= "System Options";


/**
 * Name of the all options tab
 */
String	  ALL_STRING			= "All Options";


/**
 * Name of the recently changed options tab
 */
String	  USER_RECENT_OPTIONS_STRING	 = "Recently Changed";


/**
 * Name of the search tab
 */
String	  SEARCH_STRING 	      = "Search";

/**
 * Name of search options tab
 */
String	  SEARCH_OPTIONS_STRING       = "Search Options";



/********************************************************************************/
/*										*/
/*	Constants for component creation					*/
/*										*/
/********************************************************************************/

/**
 * Background color of whole panel
 */

Color	  BACKGROUND_COLOR		   = Color.white;

/**
 * Size of the entire options panel
 */
Dimension OPTIONS_PANEL_SIZE		 = new Dimension(498,550);


/**
 * Size of panel with tabs
 */
Dimension TAB_PANEL_SIZE		     = new Dimension(200,440);


/**
 * Size of separator between options
 */
Dimension SEPARATOR_SIZE		     = new Dimension(350,5);


/**
 * Size of the inner panel inside tabs
 */
Dimension SCROLL_PANEL_SIZE		  = new Dimension(350,445);


/**
 * Size of a boolean option
 */
Dimension BOOLEAN_OPTION_SIZE		= new Dimension(340,50);


/**
 * Size of a color option
 */
Dimension COLOR_OPTION_SIZE		  = new Dimension(340,60);


/**
 * Size of a combo option
 */
Dimension COMBO_OPTION_SIZE		  = new Dimension(340,60);


/**
 * Size of a combo box
 */
Dimension COMBO_BOX_SIZE		     = new Dimension(340,30);


/**
 * Size of a color option
 */
Dimension FONT_OPTION_SIZE		   = new Dimension(340,30);


/**
 * Size of a divider
 */
Dimension DIVIDER_OPTION_SIZE		= new Dimension(340,32);


/**
 * Size of an integer option
 */
Dimension INT_OPTION_SIZE		    = new Dimension(340,42);


/**
 * Size of a string option
 */
Dimension STRING_OPTION_SIZE		 = new Dimension(340,52);

/**
 * Size of an slider
 */
Dimension SLIDER_SIZE			= new Dimension(300,20);


/**
 * Bold font for option display
 */
Font	  OPTION_BOLD_FONT		   = BoardFont.getFont(Font.SANS_SERIF,Font.BOLD,12);


/**
 * Plain font for option display
 */
Font	  OPTION_PLAIN_FONT		  = BoardFont.getFont(Font.SANS_SERIF,Font.PLAIN,12);


/**
 * font for option examples display
 */
Font	  OPTION_EXAMPLE_FONT		= BoardFont.getFont(Font.SANS_SERIF,Font.PLAIN,11);


/**
 * font for option warnings display
 */
Font	  OPTION_WARNING_FONT		= BoardFont.getFont(Font.SANS_SERIF,Font.ITALIC,11);


/**
 * Font for unselected tabs
 */
Font	  TAB_UNSELECTED_FONT		= BoardFont.getFont(Font.DIALOG,Font.PLAIN,12);


/**
 * Font for the currently selected tab
 */
Font	  TAB_SELECTED_FONT		  = BoardFont.getFont(Font.DIALOG,Font.BOLD,12);


/**
 * Main font for dividers
 */
Font	  DIVIDER_LARGE_FONT		 = BoardFont.getFont(Font.DIALOG,Font.BOLD,14);


/**
 * Subfont for dividers
 */
Font	  DIVIDER_SMALL_FONT		 = BoardFont.getFont(Font.DIALOG,Font.BOLD,10);


/**
 * Color for the font of warnings
 */
Color	  WARNING_COLOR 	      = Color.red;


/**
 * Height of a warning/example label
 */
int	  LABEL_SIZE			 = 20;


/**
 * Width of an integer label
 */
int	  INT_LABEL_WIDTH		    = 15;

/**
 * Height of a slider
 */
int	  SLIDER_INT_SIZE		    = 20;


/**
 * Maximum number of recent options to display
 */
int	  MAXIMUM_RECENTLY_DISPLAYED_OPTIONS = 20;


/**
 * X position of the color swatch in a color option
 */
int	  COLOR_SWATCH_X		     = 10;


/**
 * Y position of the color swatch in a color option
 */
int	  COLOR_SWATCH_Y		     = 10;


/**
 * Width of the color swatch in a color option
 */
int	  COLOR_SWATCH_WIDTH		 = 20;


/**
 * Height of the color swatch in a color option
 */
int	  COLOR_SWATCH_HEIGHT		= 20;


/**
 * X position of the color choooser button
 */
int	  COLOR_BUTTON_POSITION_X	    = 40;


/**
 * Y position of the color chooser button
 */
int	  COLOR_BUTTON_POSITION_Y	    = 5;


/**
 * Markers on either side of a divider's text to differentiate it
 */
String	  DIVIDER_EMPHASIS_MARKERS	   = "--";


/**
 * Text to show in the preview for font selection
 */
String	  FONT_PREVIEW_TEXT		  = "AbCdEfGhIjKlMnOpQrStUvWxYz";

/**
 * Color for the borders in the panel
 */
Color	  PANEL_BORDER_COLOR		 = new Color(173,214,226);


/**
 * Border for the entire panel
 */
Border	  PANEL_BORDER		       = BorderFactory.createMatteBorder(4, 4, 4,
						      4, PANEL_BORDER_COLOR);


/**
 * Border for the actual inner tab
 */
Border	  TAB_BORDER			 = BorderFactory.createMatteBorder(0, 2, 0,
						      0, PANEL_BORDER_COLOR);


/**
 * Border for the tab panel (splits the options panel into 3)
 */
Border	  TAB_PANEL_BORDER		   = BorderFactory.createMatteBorder(2, 0, 2,
						      0, PANEL_BORDER_COLOR);



/********************************************************************************/
/*										*/
/*	Constants for option button on the main panel				*/
/*										*/
/********************************************************************************/

Insets	  BOPP_BUTTON_INSETS		 = new Insets(0,8,0,8);



/********************************************************************************/
/*										*/
/*	Basic option								*/
/*										*/
/********************************************************************************/

interface BoppOptionNew {

   String getOptionName();
   OptionType getOptionType();
   Collection<String> getOptionTabs();

   void addButton(SwingGridPanel pnl);

   boolean search(Pattern [] pat);

}	// end of inner interface BoppOptionNew



}	// end of interface BoppConstants



/* end of BoppConstants.java */
