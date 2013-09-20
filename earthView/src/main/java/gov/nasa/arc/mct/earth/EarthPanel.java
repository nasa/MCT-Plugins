package gov.nasa.arc.mct.earth;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class EarthPanel extends JPanel {
	private static final long serialVersionUID = 5022663186983216966L;
	private static BufferedImage image = null;
	private BufferedImage[] render    = { new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB),  new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB) };
	private int             visible   = 0; // Index to on-screen buffer; offscreen = 1^visible
	
	private double xr   = 0.0;
	private double yr   = 0.0;
	private double px   = 0.0;
	private double py   = 0.0;
	private double zoom = 1.0;
	
	private int    depth = 0;
	
	private Map<JComponent, Trajectory>   trajectories = new HashMap<JComponent, Trajectory>();
	private Map<JComponent, List<Vector>> histories    = new HashMap<JComponent, List<Vector>>();
	
	private List<ViewChangeListener> listeners = new ArrayList<ViewChangeListener>();
	
	private static final Color[] COLORS = {
		Color.YELLOW,
		Color.ORANGE,
		Color.CYAN,
		Color.GREEN,
		Color.MAGENTA,
		Color.WHITE
	};
	
	public EarthPanel() {
		if (image == null) {
			try {
				image = ImageIO.read(getClass().getResourceAsStream("images/world.jpg"));
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
		addMouseMotionListener(new DragListener());
		addMouseWheelListener (new ScrollListener());
	}
	
	public EarthPanel(Map<JComponent, Trajectory> trajectories) {
		this();
		this.trajectories = trajectories;
		for (JComponent comp : trajectories.keySet()) {
			add(comp);
		}
	}
	
	public static final void main(String[] args) {
		Trajectory t = new Trajectory() {

			@Override
			public Vector getPosition() {
				//double t = (double) System.currentTimeMillis() / 1000.0;
				//return new Vector(Math.sin(t), 0, Math.cos(t));
				// San Francisco, ECEF (km)
				return new Vector(-2709.487, -4281.02, 3861.564); //-4409.0, 2102.0, -4651.0);
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
		frame.getContentPane().add(new EarthPanel(traj));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(500, 500);
		frame.setVisible(true);

	}
	
	public void setView (double xr, double yr, double px, double py, double zoom) {
		this.xr   = xr;
		this.yr   = yr;
		this.px   = px;
		this.py   = py;
		this.zoom = zoom;
		repaint();
	}
	
	public void addViewChangeListener(ViewChangeListener vcl) {
		listeners.add(vcl);
	}
	
	public void removeViewChangeListener(ViewChangeListener vcl) {
		listeners.remove(vcl);
	}
	
	@Override
	public void paint(Graphics g) {
		double scale = zoom * (double) Math.min(getWidth(), getHeight());
		render(((int) scale) >> depth);

		int x = getWidth() / 2  + (int) (px * scale);
		int y = getHeight() / 2 + (int) (py * scale);
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		while (!rendering.compareAndSet(false, true));
		g.drawImage(render[visible], x - (int)scale/2, y - (int)scale/2, (int)scale, (int)scale, this);
		rendering.set(false);

		int c = 0;
		for (Entry<JComponent, Trajectory> entry : trajectories.entrySet()) {
			Color color = COLORS[(c++) % COLORS.length];
			Trajectory traj = entry.getValue();
//			Vector position = transform (traj.getPosition());
//			Vector p2       = transform( traj.getPosition().add( traj.getVelocity().multiply(100.0) ) );
			
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
			while (history.size() > 200) history.remove(0);
			
			if (history.isEmpty()) continue;
			
			Vector p1, p2;
			p1 = transform(history.get(0));
			int x1 = 0, y1 = 0, x2 = 0, y2 = 0;
			for (Vector vec : history) {
				p2 = transform(vec);
				x1 = x + (int) (p1.getX() * scale / 2.0);
				y1 = y + (int) (p1.getY() * scale / 2.0); 
				x2 = x + (int) (p2.getX() * scale / 2.0);
				y2 = y + (int) (p2.getY() * scale / 2.0);
				g.setColor(p1.getZ() < 0 ? color : color.darker().darker());
				g.drawLine(x1, y1, x2, y2);
				p1 = p2;
			}
			JComponent representation = entry.getKey();
			representation.setLocation(x1 - 10, y1 - 10);
			representation.setForeground(g.getColor());

			//g.drawString(entry.getKey(), x1, y1);
		}
		
		if (depth > 0) {
			depth--;
			repaint();
		}
		
		super.paint(g);
		
//		int midx = getWidth()  / 2;
//		int midy = getHeight() / 2;
//		

//		
//		double low  = -scale;
//		double high =  scale;
//		
//		for (int u = (int) low; u < high; u++ ) {
//			for (int v = (int) low; v < high; v++) {
//				g.setColor(getColor((double) u / scale, (double) v / scale));
//				g.fillRect(u + midx, v + midy, 1, 1);
//			}
//		}
	}
	
	
	private void drawVessel(Graphics g, String name, int x1, int y1, int x2, int y2, Color c) {
		g.setColor(c);
		g.drawString(name, x1, y1);
		g.drawLine(x1, y1, x2, y2);
	}
	
	private void render(int sz) {
		if (sz > 0) new Renderer(sz).render();
	}
	
	private int getColor(double x, double y) {
		if (x*x + y*y > 1.0) return 0;
		
		double z = -Math.sqrt(1.0 - x*x - y*y);
		
		double t, u, v;
		
		t = xr;//Math.PI / 8;
		v = y;
		y = y * Math.cos(t) + z * Math.sin(t);
		z = z * Math.cos(t) - v * Math.sin(t);
		t = yr;//-Math.PI / 8;
		u = x;
		x = x * Math.cos(t) + z * Math.sin(t);
		z = z * Math.cos(t) - u * Math.sin(t);

		u = x;
		t = 0; //-(double) System.currentTimeMillis() / 2500.0;
		x = x * Math.cos(t) - z * Math.sin(t);
		z = z * Math.cos(t) + u * Math.sin(t);
		
		double xx = x / Math.sqrt(x*x + z*z);
		u = 0.5 + Math.signum(z) * 0.5 * Math.acos(xx) / Math.PI;
		v = 1.0 - Math.acos(y) / Math.PI;	

		
//		double x = (theta + 1.0) / 2.0;
//		double y = (v     + 1.0) / 2.0;
//		
		return image.getRGB((int) (u * (image.getWidth()-1)), (int) (v * (image.getHeight()-1)));
	}
	
	@Override
	public Dimension getMinimumSize() {
		return new Dimension(0,0);
	}

	private AtomicBoolean rendering = new AtomicBoolean(false);
	private AtomicBoolean busy      = new AtomicBoolean(false);
	private AtomicInteger current   = new AtomicInteger(-1);
	private class Renderer extends Thread {
		private int sz;		
		
		public Renderer (int sz) {
			this.sz = sz;
		}
		
		public void render() {
			//if (busy.compareAndSet(false, true)) {
			if (current.compareAndSet(-1, sz)) {
				start();
			} else {
				int other = current.get();
				if (other > sz) {
					if (current.compareAndSet(other, sz)) {
						start();
					}
				}
			}
			//}
		}
		public void run() {
			BufferedImage r;

			if (render[1^visible].getWidth() != sz || render[1^visible].getHeight() != sz) {
				if (render[1^visible].getWidth() > sz && render[1^visible].getHeight() > sz) {
					render[1^visible] = render[1^visible].getSubimage(0, 0, sz, sz);
				} else {
					render[1^visible] = new BufferedImage(sz, sz, BufferedImage.TYPE_INT_RGB);
				}
			}
			
			r = render[1^visible]; //new BufferedImage(sz, sz, BufferedImage.TYPE_INT_RGB);
			
			double x = -1.0;
			double y = -1.0;
			double delta = 2.0 / (double) sz;
			
			for (int v = 0; v < sz; v++ ) {
				for (int u = 0; u < sz; u++) {
					r.setRGB(u, v, getColor(x, y));
					x += delta;
					if (current.get() != sz) return;
				}
				x = -1.0;
				y += delta;
			}

			if (!current.compareAndSet(sz, -2)) return;
			while (!rendering.compareAndSet(false, true));
			visible = 1^visible;
			rendering.set(false);
			current.set(-1);
			
			//busy.set(false);
			repaint();
		}
	}
	
	private Vector transform (Vector vec) {
		vec = vec.multiply(1.0 / 6371.0);
		double x = vec.getX();
		double z = vec.getY();
		double y = -vec.getZ();
		
		double t, u, v;
		
		t = -yr;//-Math.PI / 8;
		u = x;
		x = x * Math.cos(t) + z * Math.sin(t);
		z = z * Math.cos(t) - u * Math.sin(t);
		
		t = -xr;//Math.PI / 8;
		v = y;
		y = y * Math.cos(t) + z * Math.sin(t);
		z = z * Math.cos(t) - v * Math.sin(t);
		
		return new Vector (x, y, z);
	}
	
	private void fireViewChanged() {
		for (ViewChangeListener vcl : listeners) {
			vcl.viewChanged(xr, yr, px, py, zoom);
		}
	}
	
	private class DragListener implements MouseMotionListener {
		private boolean dragging = false;
		private int lastX;
		private int lastY;
		@Override
		public void mouseDragged(MouseEvent evt) {
			if (dragging) {
				int dx = evt.getX() - lastX;
				int dy = evt.getY() - lastY;
				if (evt.isShiftDown()) {
					px += (double) dx / (double) getWidth () / zoom;
					py += (double) dy / (double) getHeight() / zoom;
					if (Math.abs(px) > 1.0) px = Math.signum(px);
					if (Math.abs(py) > 1.0) py = Math.signum(py);
				} else if (evt.isAltDown() || evt.isMetaDown()) {
					double r = (double) dy / (double) getWidth() / zoom;
					zoom *= Math.pow(2.00, -r);
					if (zoom > 2.00) zoom = 2.00;
					if (zoom < 0.01) zoom = 0.01;
				} else {				
					xr += 3.0 * (double) dy / (double) getHeight();
					yr += 3.0 * (double) dx / (double) getWidth() * Math.signum(Math.cos(xr));
					int sz = (int) (Math.min(getWidth(), getHeight()) * zoom);
					depth = 0;
					while (sz >> depth > 200) depth++;
				}
				repaint();
			}
			lastX = evt.getX();
			lastY = evt.getY();
			dragging = true;
		}

		@Override
		public void mouseMoved(MouseEvent arg0) {
			if (dragging) fireViewChanged();
			dragging = false;
		}
		
	}
	
	private class ScrollListener implements MouseWheelListener {

		@Override
		public void mouseWheelMoved(MouseWheelEvent evt) {
			double r = (double) evt.getWheelRotation();
			zoom *= Math.pow(1.05, -r);
			if (zoom > 2.00) zoom = 2.00;
			if (zoom < 0.01) zoom = 0.01;
			repaint();
		}
		
	}
	
	public interface ViewChangeListener {
		public void viewChanged(double xr, double yr, double px, double py, double zoom);
	}
}
