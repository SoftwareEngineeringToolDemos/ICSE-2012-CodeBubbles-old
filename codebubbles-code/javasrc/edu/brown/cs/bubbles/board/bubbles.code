// author: Rachel Gollub, 1995
// Bubbles!

import java.awt.*;
import java.applet.*;

public class Bubbles extends java.applet.Applet implements Runnable {
  Thread artist=null;
  int bubble=0, thisbubble=0;   // # of bubbles on screen, and current bubble
  int MAXBUBBLES = 25;           // Maximum bubbles on screen
  int stepper = 4;               // Counter for which bubbles to move when
  int record[][] = new int[MAXBUBBLES][5];  // Array that holds bubbles
  Color col = null;		// Color holder
  
public void init() {
  resize(500,500);              // Set bubble window size
}

public void move_bubble(int x, int y, int r, int step, Graphics g) {
  int i;

  for (i=x-r; i<=x+r; i++) {     // Draws the upper edge of a circle
    g.drawLine(i, y - (int)(Math.sqrt( r*r - ( (i-x)*(i-x) ))),
	       i, y + step - (int)(Math.sqrt( r*r - ( (i-x)*(i-x) ))));
  }
  g.setColor(getBackground());
  for (i=x-r; i<=x+r; i++) {     // Draws the lower edge of the circle
    g.drawLine(i, y + (int)(Math.sqrt( r*r - ( (i-x)*(i-x) ))),
	       i, y + step + (int)(Math.sqrt( r*r - ( (i-x)*(i-x) ))));
  }
}

public void paint(Graphics g) {
  int i, j, tmp;

  if (bubble < MAXBUBBLES || thisbubble < MAXBUBBLES) {    
    record[thisbubble][0]=(int)(Math.random() * 500);
    record[thisbubble][1]=550;
    record[thisbubble][2]=(int)(Math.random() * 500)/20;
    record[thisbubble][3]=(int)(Math.random() * 255);
    record[thisbubble][4]=(int)(Math.random() * 255);
    col = new Color(record[thisbubble][3], record[thisbubble][4], 255);
    g.setColor(col);
    g.fillOval(record[thisbubble][0]-record[thisbubble][2],
	       record[thisbubble][1]-record[thisbubble][2],
		record[thisbubble][2]*2,record[thisbubble][2]*2);
    if (bubble < MAXBUBBLES) {
      bubble++; thisbubble++;
    }
    else
      thisbubble = MAXBUBBLES;
  }
  for (i=0; i<bubble; i++) {
    if (i%5 <= stepper) { // Steps each bubble at a different speed
      record[i][1] -= 1;
      col = new Color(record[i][3], record[i][4], 255);
      g.setColor(col);
      move_bubble(record[i][0], record[i][1], record[i][2], 1, g);
      for (j=0; j<i; j++) {   // Checks for touching bubbles, pops one
	tmp = ( (record[i][1]-record[j][1])*(record[i][1]-record[j][1]) +
		(record[i][0]-record[j][0])*(record[i][0]-record[j][0]) );
	if (j != i && Math.sqrt(tmp) < record[i][2] + record[j][2]) {
	  g.setColor(getBackground());
	  for (tmp = record[i][2]; tmp >= -1; tmp = tmp - 2)
	    g.fillOval(record[i][0]-(record[i][2]-tmp), 
		       record[i][1]-(record[i][2]-tmp), 
		       (record[i][2]-tmp)*2, (record[i][2]-tmp)*2);
	  col = new Color(record[j][3], record[j][4], 255);
	  g.setColor(col);
	  g.fillOval(record[j][0]-record[j][2], record[j][1]-record[j][2], 
		     record[j][2]*2, record[j][2]*2);
	  record[i][1] = -1; record[i][2]=0;
	}
      }
    }
    if (record[i][1]+record[i][2] < 0 && bubble >= MAXBUBBLES) {
      thisbubble = i;
    }
    stepper=(int)(Math.random()*10);
    col = null;
  }
}

public void update(Graphics g) {
  paint(g);
}

public void start() {
  if (artist == null) {
    artist = new Thread(this);
    artist.start();
  }
}

public void stop() {
    artist = null;
}
public void run() {
  while (artist != null) {
    try {Thread.sleep(50);} catch (InterruptedException e){}
    repaint();
  }
  artist = null;
}
}
