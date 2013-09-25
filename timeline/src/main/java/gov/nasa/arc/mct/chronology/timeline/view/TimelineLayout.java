package gov.nasa.arc.mct.chronology.timeline.view;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A LayoutManager specifically responsible for managing the horizontal positions of components 
 * with specific start / end times. 
 * @author vwoeltje
 *
 * @param <T> the type of object which describes a specific instant in the appropriate chronology  
 */
public class TimelineLayout implements LayoutManager2 {
	@SuppressWarnings("unused")
	private final static Logger LOGGER = LoggerFactory.getLogger(TimelineControlPanel.class);
	
	private Map<Component, TimelineLayoutWeight> weights = new HashMap<Component, TimelineLayoutWeight>();
	private Map<Container, Integer>              heights = new HashMap<Container, Integer>( );
	private int height = 1;
	private int width  = 1;
	private TimelineLayoutWeight window = new TimelineLayoutWeight(0.0, 1.0);
	
	@Override
	public void addLayoutComponent(String arg0, Component arg1) {
		/* Do nothing - strings indicate nothing in this layout */
	}

	public void setWidth(int w) {
		width = w;
		
		Set<Container> parents = new HashSet<Container>();
		for (Component c : weights.keySet()) parents.add(c.getParent());
		for (Container p : parents)          { p.invalidate(); p.validate(); p.repaint(); }
	}
	
	@Override
	public void layoutContainer(Container parent) {
		int count = parent.getComponentCount();
        height = 20;
		
		for (int i = 0 ; i < count; i++) {
			Component child = parent.getComponent(i);
			TimelineLayoutWeight weight = window.rescale(weights.get(child));
			if (weight == null) {
				continue;
			}
			if (weight.end < 0.0 || weight.start > 1.0) {
				child.setVisible(false);
				continue;
			}
			child.setVisible(true);
			int x1 = (int) ((double) width * weight.start);
			int x2 = (int) ((double) width * weight.end);
			int w  = (weight.start != weight.end) ? x2-x1 : Math.min(width /5, Math.min(child.getPreferredSize().width, width - x1));
			child.setBounds(x1, 0, w, Math.min(child.getPreferredSize().height, parent.getParent().getHeight()));
			//for (int x = 0; x < 1000; x++) child.doLayout();
			child.setBounds(x1, 0, w, Math.min(child.getPreferredSize().height, parent.getParent().getHeight()));
			height = Math.max(height, Math.min(child.getPreferredSize().height, parent.getParent().getHeight()));
		}
		for (int i = 0 ; i < count ; i++) {
			Component child = parent.getComponent(i);
			child.setBounds(child.getX(), 0, child.getWidth(), height);
		}
		parent.setBounds(parent.getX(), parent.getY(), width, parent.getHeight());
		parent.setPreferredSize(new Dimension(width, height));
		heights.put(parent, height);
		
	}

	@Override
	public Dimension minimumLayoutSize(Container arg0) {
		return new Dimension(0, (heights.containsKey(arg0) ? heights.get(arg0) : height));
	}

	@Override
	public Dimension preferredLayoutSize(Container arg0) {
		return new Dimension(width, (heights.containsKey(arg0) ? heights.get(arg0) : height));
	}

	@Override
	public void removeLayoutComponent(Component arg0) {
		weights.remove(arg0);
	}

	@Override
	public void addLayoutComponent(Component comp, Object weight) {
		if (weight instanceof TimelineLayoutWeight) weights.put(comp, (TimelineLayoutWeight) weight);
	}

	@Override
	public float getLayoutAlignmentX(Container arg0) {
		return JComponent.CENTER_ALIGNMENT;
	}

	@Override
	public float getLayoutAlignmentY(Container arg0) {
		return 0;
	}

	@Override
	public void invalidateLayout(Container arg0) {

	}

	public void setWindow(double low, double high) {
		if (low  < 0.0 || high > 1.0 || low > high) {
			//LOGGER.debug(String.format("Ignoring invalid  ");
			return;
		}
		window = new TimelineLayoutWeight(low, high);
		Set<Container> parents = new HashSet<Container>();
		for (Component c : weights.keySet()) parents.add(c.getParent());
		for (Container p : parents)          { p.invalidate(); p.validate(); p.repaint(); }
	}
	
	@Override
	public Dimension maximumLayoutSize(Container arg0) {		 
		return new Dimension(arg0.getParent().getWidth(), (heights.containsKey(arg0) ? heights.get(arg0) : height));
	}
	
	public TimelineLayoutWeight makeWeight(double start, double end) {
		return new TimelineLayoutWeight(start, end);
	}
	
	public class TimelineLayoutWeight {
		private double start, end;
		public TimelineLayoutWeight(double start, double end) {
			this.start = start;
			this.end   = end;
		}
		public TimelineLayoutWeight rescale(TimelineLayoutWeight w) {
			double low = (w.start - start) / (end - start);
			double high = (w.end - start) / (end - start);
			if (low  < 0.0) low  = 0.0;
			if (high > 1.0) high = 1.0;
			return new TimelineLayoutWeight(low, high);
		}
	}


	
}
