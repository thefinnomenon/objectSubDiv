import java.awt.event.*;

import javax.media.opengl.*;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.vecmath.Point2f;

import java.util.ArrayList;
import java.util.Scanner;
import java.io.*;

public class curveGen extends JFrame implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, ActionListener{

	/* GL related variables */	
	private final GLCanvas canvas;
	private GL gl;
	
	private int winW = 600, winH = 600;

	private static final int Bezier = 1, Bspline = 2;
	/* initial curve type: Bezier */ 
	private int curve_type = Bezier;
	/* number of line segments used to approximate curves */
	private int nsegment = 32;
	/* toggle between closing a curve. only applicable to Bspline */
	private boolean close_curve = false;
	/* toggle showing of control point line */
	private boolean show_control_line = true;

	/* control_pts is an array that stores a list of the control points
	 * each element is a 2D point. control points can be created using
	 * the GUI interface, or loaded from a disk file
	 */
	private ArrayList<Point2f> control_pts = new ArrayList<Point2f> ();
	/* curve_pts is an array that stores points representing the Bspline
	 * or Bezier curve, and it is generated by your code.
	 */
	private ArrayList<Point2f> curve_pts = new ArrayList<Point2f> ();
	/* selected_point keeps track of the currect control point selected by mouse
	 * -1 means no point is selected currently
	 */
	private int selected_point = -1;

	private void drawControlLines()
	{
		int i;
		for (i = 0; i < control_pts.size(); i ++) {
			Point2f pt = control_pts.get(i);
			drawRectangle(pt.x-0.008f, pt.y-0.008f, pt.x+0.008f, pt.y+0.008f, 1, 0, 0);
		}
		for (i = 0; i < control_pts.size()-1; i ++) {
			Point2f pt1 = control_pts.get(i);
			Point2f pt2 = control_pts.get(i+1);
			drawLine(pt1.x, pt1.y, pt2.x, pt2.y, 0, 0, 1);
		}
	}

	public static void main(String args[]) {
		if (args.length == 1) {
			// if an input filename is given, try to load control points from the file
			new curveGen(args[0]);
		} else {
			new curveGen(null);
		}
	}

	private void drawLine(float x1, float y1, float x2, float y2, float red, float green, float blue)
	{
		gl.glColor3f(red, green, blue);
		gl.glBegin(GL.GL_LINES);
		gl.glVertex2f(x1, y1);
		gl.glVertex2f(x2, y2);
		gl.glEnd();
	}
	
	private void drawRectangle(float xmin, float ymin, float xmax, float ymax, float red, float green, float blue)
	{
		gl.glColor3f(red, green, blue);
		gl.glBegin(GL.GL_QUADS);
		gl.glVertex2f(xmin, ymin);
		gl.glVertex2f(xmin, ymax);
		gl.glVertex2f(xmax, ymax);
		gl.glVertex2f(xmax, ymin);
		gl.glEnd();
	}

	/* load control points from a disk file */
	private void loadPoints(String filename) {
		File file = null;
		Scanner scanner = null;
		try {
			file = new File(filename);
			scanner = new Scanner(file);
		} catch (IOException e) {
			System.out.println("Error reading from file " + filename);
			System.exit(0);
		}
		float x, y;
		while(scanner.hasNext()) {
			x = scanner.nextFloat();
			y = scanner.nextFloat();
			control_pts.add(new Point2f(x, y));
		}
		System.out.println("Read " + control_pts.size() +
					   	" points from file " + filename);
		scanner.close();
	}
	
	/* save control points and curve points to disk files
	 * both files have the extension name '.pts'
	 * you will input a filename, say, it's 'cup', then
	 * the control points will be saved to 'cup.pts',
	 * and the curve points will be saved to 'cup_curve.pts'.
	 */
	private void savePoints() {
		String curvename = JOptionPane.showInputDialog(this, "Input a name of the curve to save", "mycurve");
		if (curvename == null)	return;
		int i;
		// save the control points to curvename.pts
		FileOutputStream file = null;
		PrintStream output = null;
		try {
			file = new FileOutputStream(curvename+".pts");
			output = new PrintStream(file);
		} catch (IOException e) {
			System.out.println("Error writing to file " + curvename+".pts");
			return;
		}
		for (i = 0; i < control_pts.size(); i ++) {
			output.println(control_pts.get(i).x + " " + control_pts.get(i).y);
		}
		output.close();

		try {
			file = new FileOutputStream(curvename+"_curve.pts");
			output = new PrintStream(file);
		} catch (IOException e) {
			System.out.println("Error writing to file " + curvename+"_curve.pts");
			return;
		}
		for (i = 0; i < curve_pts.size(); i ++) {
			output.println(curve_pts.get(i).x + " " + curve_pts.get(i).y);
		}
		output.close();
	}

	/* creates OpenGL window */
	public curveGen(String inputFilename) {
		super("Assignment 2 -- Curve Generator");
		canvas = new GLCanvas();
		canvas.addGLEventListener(this);
		canvas.addKeyListener(this);
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
		getContentPane().add(canvas);
		setSize(winW, winH);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
		canvas.requestFocus();
		if (inputFilename != null) {
			loadPoints(inputFilename);
		}
	}
	
	/*************
	 ** MY CODE **  
	 *************/
	/* drawCurve()
	 * Draws line segments between curve points
	 */
	private void drawCurve() {
		for (int i = 0; i < curve_pts.size()-1; i ++) {
			Point2f pt1 = curve_pts.get(i);
			Point2f pt2 = curve_pts.get(i+1);
			drawLine(pt1.x, pt1.y, pt2.x, pt2.y, 1, 0, 0);
		}
	}
	
	/* drawBezier()
	 * draws Bezier curve
	 */
	private void drawBezier() {
		int npts = control_pts.size();
		if (npts < 3)
			return;
		curve_pts.clear();
		for(int i=0;i<=nsegment;i++){
			float t = (float)i / (float)nsegment;
			Point2f pt = compute_Bezier_pt(control_pts, t);
			curve_pts.add(pt);
		}
		drawCurve();
	}
	
	/* compute_Bezier_pt()
	 * Recursive calculation of Bezier curve point at (t)
	 */
	Point2f compute_Bezier_pt(ArrayList<Point2f> cp, float t){
		if(cp.size() == 1)
			return cp.get(0);
		ArrayList<Point2f> pts = new ArrayList<Point2f> ();
		for(int i=0;i<cp.size()-1;i++){
			float x = (1-t)*cp.get(i).x+(t)*(cp.get(i+1).x);
			float y = (1-t)*cp.get(i).y+(t)*(cp.get(i+1).y);
			pts.add(new Point2f(x, y));
		}
		return compute_Bezier_pt(pts, t);
	}
	
	/* drawBspline()
	 * draws Bspline curve
	 */
	private void drawBspline() {
		int npts = control_pts.size();
		int n = 3;
		if (npts < 4)
			return;
		curve_pts.clear();
		if(close_curve){	// Changes n so the for statement will
			n = 0;          // go all the way to the last control point 
		}					// *indexes to control_pts.get are %npts for wrap around
		for(int i=0;i<npts-n;i++){
			float P0x = control_pts.get(i).x;   	float P0y = control_pts.get(i).y;
			float P1x = control_pts.get((i+1)%npts).x; 	float P1y = control_pts.get((i+1)%npts).y;
			float P2x = control_pts.get((i+2)%npts).x; 	float P2y = control_pts.get((i+2)%npts).y;
			float P3x = control_pts.get((i+3)%npts).x; 	float P3y = control_pts.get((i+3)%npts).y;
			float Ax = -P0x + 3*P1x - 3*P2x + P3x; 	float Ay = -P0y + 3*P1y - 3*P2y + P3y;
			float Bx = 3*P0x - 6*P1x + 3*P2x;      	float By = 3*P0y - 6*P1y + 3*P2y;
			float Cx = -3*P0x + 3*P2x; 		       	float Cy = -3*P0y + 3*P2y;
			float Dx = P0x + 4*P1x + P2x;  	    	float Dy = P0y + 4*P1y + P2y;
			for(int k=0;k<nsegment;k++){
				float t = (float)k / (float)nsegment;
				float x = ((float)1/6)*(Ax*(t*t*t)+Bx*(t*t)+Cx*(t)+Dx);
				float y = ((float)1/6)*(Ay*(t*t*t)+By*(t*t)+Cy*(t)+Dy);
				curve_pts.add(new Point2f(x, y));
			}
		}
		drawCurve();
	}

	/* gl display function */
	public void display(GLAutoDrawable drawable) {
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		if (show_control_line)
			drawControlLines();
		switch (curve_type) {
			case Bezier:
				drawBezier();
				break;
			case Bspline:
				drawBspline();
				break;
		}
	}

	/* initialize GL */
	public void init(GLAutoDrawable drawable) {
		gl = drawable.getGL();

		gl.glClearColor(.3f, .3f, .3f, 1f);
		gl.glClearDepth(1.0f);

		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0, 1, 0, 1, -10, 10);
		gl.glMatrixMode(GL.GL_MODELVIEW);
	}

	/* mouse and keyboard callback functions */
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		winW = width;
		winH = height;

		gl.glViewport(0, 0, width, height);
	}

	public void mousePressed(MouseEvent e) {

		/* normalize the mouse position to be between [0, 1] x [0, 1] */
		float x = (float)e.getX()/(float)winW;
		float y = 1.0f - (float)e.getY()/(float)winH;
		
		/* if mouse left button is pressed */
		if (e.getButton() == MouseEvent.BUTTON1) {
			/* detect whether the mouse clicked on any existing control point */
			int i;
			selected_point = -1;
			for (i = 0; i < control_pts.size(); i ++) {
				Point2f pt = control_pts.get(i);
				if (Math.abs(pt.x - x) < 0.008f && Math.abs(pt.y - y) < 0.008f) {
					selected_point = i;
				}
			}	
			/* if CTRL is pressed, add a point */
			if ((e.getModifiers() & InputEvent.CTRL_MASK) != 0) {
				if (selected_point == -1) {
					control_pts.add(new Point2f(x, y));
				}
			}
			/* if SHIFT is pressed, and a valid point is selected, then delete the point */
			else if((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
				if (selected_point >= 0) {
					control_pts.remove(selected_point);
					selected_point = -1;
				}
			}
			canvas.display();
		}
	}

	/* if mouse is dragging on an existing control point, move that point */
	public void mouseDragged(MouseEvent e) {
		float x = (float)e.getX()/(float)winW;
		float y = 1.0f - (float)e.getY()/(float)winH;
		if (selected_point >= 0) {
			control_pts.get(selected_point).x = x;
			control_pts.get(selected_point).y = y;
			canvas.display();
		}
	}

	public void keyPressed(KeyEvent e) {
		switch(e.getKeyCode()) {
		case KeyEvent.VK_ESCAPE:
		case KeyEvent.VK_Q:
			System.exit(0);
			break;
		// press 'e' to clear all control points
		case KeyEvent.VK_E:
			control_pts.clear();
			canvas.display();
			break;
		// press '1' to select Bezier curve, '2' for Bspline
		case KeyEvent.VK_1:
			curve_type = Bezier;
			canvas.display();
			break;
		case KeyEvent.VK_2:
			curve_type = Bspline;
			canvas.display();
			break;
		// press '+' or '-' to increase or decrease nsegment
		case KeyEvent.VK_ADD:
		case KeyEvent.VK_EQUALS:
			nsegment = nsegment + 1;
			canvas.display();
			break;
		case KeyEvent.VK_MINUS:
			if (nsegment > 1)
				nsegment = nsegment - 1;
			canvas.display();
			break;
		// press 'c' to toggle closing curve
		case KeyEvent.VK_C:
			close_curve = !close_curve;
			canvas.display();
			break;
		// press 'l' to toggle showing control line
		case KeyEvent.VK_L:
			show_control_line = !show_control_line;
			canvas.display();
			break;
		// press 's' to save points
		case KeyEvent.VK_S:
			savePoints();
			canvas.display();
			break;
		}
	}

	// these event functions are not used for this assignment
	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) { }
	public void mouseReleased(MouseEvent e) { }
	public void keyTyped(KeyEvent e) { }
	public void keyReleased(KeyEvent e) { }
	public void mouseMoved(MouseEvent e) { }
	public void actionPerformed(ActionEvent e) { }
	public void mouseClicked(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) {	}

}
