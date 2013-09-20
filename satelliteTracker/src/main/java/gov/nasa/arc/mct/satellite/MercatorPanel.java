/*******************************************************************************
 * Mission Control Technologies, Copyright (c) 2009-2012, United States Government
 * as represented by the Administrator of the National Aeronautics and Space 
 * Administration. All rights reserved.
 *
 * The MCT platform is licensed under the Apache License, Version 2.0 (the 
 * "License"); you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations under 
 * the License.
 *
 * MCT includes source code licensed under additional open source licenses. See 
 * the MCT Open Source Licenses file included with this distribution or the About 
 * MCT Licenses dialog available at runtime from the MCT Help menu for additional 
 * information. 
 *******************************************************************************/
package gov.nasa.arc.mct.satellite;


import gov.nasa.arc.mct.satellite.utilities.ConvertECEFtoLLA;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/*
 * This class handles the drawing of objects on a mercator map. 
 * 
 * Note: this class is used in MercatorProjectionView.
 */
@SuppressWarnings("serial")
public class MercatorPanel  extends JPanel {
	
	//this will be the image of the earth
	private static BufferedImage image = null;
	
	//to transform our data so it can be drawn on a mercator plot
	private ConvertECEFtoLLA convert = new ConvertECEFtoLLA();
	
	private double zoom = 1.0;
	private int translateX=0;
	private int translateY=0;
	
	//The trajectories to be drawn on the mercator map and historical data to draw the trail of the satellite's orbit 
	private Map<JComponent, Trajectory>   trajectories = new HashMap<JComponent, Trajectory>();
	private Map<JComponent, List<Vector>> histories    = new HashMap<JComponent, List<Vector>>();
	
	
	private final int MAX_HISTORY = 1000;  //this effects the length of the satellite's orbit-trail 
	private final int PIXEL_WRAP_TOL = 6;  //used to determine if a satellite's orbit has wrapped around the mercator map
	
	private List<ViewChangeListener> listeners = new ArrayList<ViewChangeListener>();
	
	//each object being drawn gets a differnt color
	private static final Color[] COLORS = {
		//Color.YELLOW,
		Color.ORANGE,
		Color.CYAN,
		Color.GREEN,
		Color.MAGENTA,
		Color.WHITE,
		Color.PINK,
		Color.RED
	};
	
	public MercatorPanel() {
		if (image == null) {
			try {
				image = ImageIO.read(getClass().getResourceAsStream("images/worldJuly.jpg"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		setLayout(new FlowLayout() {
			@Override
			public Dimension minimumLayoutSize(Container parent) {
				return new Dimension (0,0);
			}
			@Override
			public Dimension preferredLayoutSize(Container parent) {
				return new Dimension (0,0);
			}
		});
		setOpaque(false);
		//addMouseMotionListener(new DragListener());
		addMouseWheelListener (new ScrollListener());
	}
	
	/**
	 * 
	 * @param trajectories the trajectories to be drawn upon the mercator map
	 */
	public MercatorPanel(Map<JComponent, Trajectory> trajectories) {
		this();
		this.trajectories = trajectories;
		for (JComponent comp : trajectories.keySet()) {
			add(comp);
		}
		
	}
	
	public void addViewChangeListener(ViewChangeListener vcl) {
		listeners.add(vcl);
	}
	
	public void removeViewChangeListener(ViewChangeListener vcl) {
		listeners.remove(vcl);
	}
	
	@Override
	public void paintComponent(Graphics g) {

		super.paintComponent(g);
		
		Graphics2D g2d = (Graphics2D)g;
		
		AffineTransform saveTransform = g2d.getTransform();
		AffineTransform at = new AffineTransform(saveTransform);	
		at.translate(getWidth()/2, getHeight()/2);	   //so we zoom wrt the center of the image
		at.scale(zoom, zoom);
		at.translate(-getWidth()/2, -getHeight()/2);  //so we zoom wrt the center of the image
		g2d.setTransform(at);

		double curWidth = getWidth();
		double curHeight= getHeight();
		
		g2d.drawImage(image, 0 , 0, (int)curWidth,(int)curHeight, null);


		int c = 0;
		for (Entry<JComponent, Trajectory> entry : trajectories.entrySet()) {
			Color color = COLORS[(c++) % COLORS.length];
			Trajectory traj = entry.getValue();
			
			if (!histories.containsKey(entry.getKey())) {
				histories.put(entry.getKey(), new ArrayList<Vector>());
			}
			
			List<Vector> history = histories.get(entry.getKey());
			Vector position = traj.getPosition();
			
			if (history.isEmpty() || !history.get(history.size() - 1).equals(position)) {
				if (position.magnitude() > 0) {
					history.add(position);
				}
			}
			
			//chop of tail of the satellite's orbit to the length of MAX_HISTORY
			while (history.size() > MAX_HISTORY) history.remove(0);
			
			if (history.isEmpty()) continue;
			
			Vector p1, p2;
			p1 = transform(history.get(0));
			
			int x1 = 0, y1 = 0, x2 = 0, y2 = 0;
			for (Vector vec : history) {
				p2 = transform(vec);
				
				x1 = (int) (curWidth*((180+p1.getX())/360));
				y1 = (-1)*(int)(  ((p1.getY()-90)/180)*curHeight);
				
				x2 = (int) (curWidth*((180+p2.getX())/360));
				y2 = (-1)*(int)(  ((p2.getY()-90)/180)*curHeight   );

				g2d.setColor(color);
				
				if( hasWrappedScreen(x1, y1, x2, y2, curWidth, curHeight) )
					g2d.drawLine(x1, y1, x1, y1);	//prevents a line being drawn across screen when a satellite wraps about the screen
				else
					g2d.drawLine(x1, y1, x2, y2);
				p1 = p2;
			}
			
			//setting the location for the Satellite's Icon and name
			JComponent satRep = entry.getKey();
			double wvr2 = getWidth()/2;
			double hvr2 = getHeight()/2;
			double transX = (x1-wvr2)*zoom+wvr2;
			double transY = (y1-hvr2)*zoom+hvr2;
			satRep.setLocation((int)(transX - 10), (int)(transY-10));
			satRep.setForeground(g2d.getColor());
		}
	g2d.setTransform(saveTransform);
}
	
	private boolean	hasWrappedScreen
	( double x1, double y1, double x2, double y2, double curWidth, double curHeight)  {
		if( Math.abs(x1 - x2) > (curWidth-PIXEL_WRAP_TOL) || Math.abs(y2-y1) > (curHeight-PIXEL_WRAP_TOL))
			return true;
		else
			return false;
	}
	
	
	/*
	 * transforms a vector from ecef coordinates into lat lon alt corrdinates
	 * (see ConvertECEFtoLLA for units used for lat lon and alt) 
	 */
	private Vector transform (Vector vec) {
		
		double[] ecefVec = {vec.getX(), vec.getY(), vec.getZ()};
		
		double[] lla = convert.ecefToLLA(ecefVec);
		
		//ordered this way so the methods getX() gives longitude and getY() gives latitude
		return new Vector(lla[1], lla[0], lla[2]);
	}
	
	
	
	@Override
	public Dimension getMinimumSize() {
		return new Dimension(0,0);
	}

	/*
	 * so the user can zoom with their mouse
	 */
	private class ScrollListener implements MouseWheelListener {

		@Override
		public void mouseWheelMoved(MouseWheelEvent evt) {
			double r = (double) evt.getWheelRotation();
			zoom *= Math.pow(1.05, -r);
			
			if (zoom > 5.00)
				zoom = 5.00;
			
			if (zoom < 1.0)
				zoom = 1.00;

			revalidate();
			repaint();
		}
	}
	
	public interface ViewChangeListener {
		public void viewChanged(double xr, double yr, double px, double py, double zoom);
	}
	
	
	/*
	 * testing to see if the mercator plot, given ECEF coordinates, properly plots the coordinates
	 */
	public static final void main(String[] args) {
		
		Trajectory t = new Trajectory() {

			@Override
			public Vector getPosition() {
				// San Francisco, ECEF (km)
				//return new Vector(-2709.487, -4281.02, 3861.564); //-4409.0, 2102.0, -4651.0);
				return new Vector(6378.137, 0, 0);	//0 lat 0 lon in ECEF
			}

			@Override
			public Vector getVelocity() {
				double t = (double) System.currentTimeMillis() / 1000.0;
				return new Vector(Math.cos(t), 0, -Math.sin(t));
			}
			
		};
		
		Map<JComponent, Trajectory> traj = new HashMap<JComponent, Trajectory>();
		traj.put(new JLabel("SF"), t);
		
		final JFrame frame = new JFrame("test");
		frame.getContentPane().add(new MercatorPanel(traj));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(500, 500);
		frame.setVisible(true);

	}
}
