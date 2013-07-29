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
package gov.nasa.arc.mct.scenario.view;

import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.gui.View;
import gov.nasa.arc.mct.scenario.component.ActivityComponent;
import gov.nasa.arc.mct.scenario.component.DurationCapability;
import gov.nasa.arc.mct.services.component.ViewInfo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.LayoutManager2;
import java.awt.event.MouseAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;

/**
 * A view of an object showing time-based objects (Activities) laid out 
 * horizontally according to time, divided into rows. 
 * 
 * @author vwoeltje
 *
 */
public class TimelineView extends AbstractTimelineView {
	private static final int TIMELINE_ROW_HEIGHT = 40;
	private static final int TIMELINE_ROW_SPACING = 8;
	private static final long serialVersionUID = -5039383350178424964L;

	
	private List<TimelineBlock> blocks = new ArrayList<TimelineBlock>();
	private JPanel upperPanel = new JPanel();
	private Color backgroundColor = Color.WHITE;
	
	
	public TimelineView(AbstractComponent ac, ViewInfo vi) {
		super(ac,vi);
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(upperPanel, BorderLayout.NORTH);
		upperPanel.setLayout(new BoxLayout(upperPanel, BoxLayout.Y_AXIS));
		upperPanel.setOpaque(false);
		upperPanel.add(Box.createVerticalStrut(TIMELINE_ROW_SPACING));
		
		getContentPane().setBackground(backgroundColor);
		
		// Add all children
		for (AbstractComponent child : ac.getComponents()) {
			addTopLevelActivity(child);//addActivities(child, 0, new HashSet<String>());
		}
		
		upperPanel.add(GraphView.VIEW_INFO.createView(ac));
	}

	
	
	@Override
	public void stateChanged(ChangeEvent e) {
		super.stateChanged(e);
		revalidate();
		repaint();
		for (TimelineBlock block : blocks) {
			block.revalidate();
			block.repaint();
			for (JComponent row : block.rows) {
				row.revalidate();
				row.repaint();
			}
		}
	}


	private void addTopLevelActivity(AbstractComponent ac) {
		DurationCapability dc = ac.getCapability(DurationCapability.class);
		if (dc != null) {
			TimelineBlock block = null;
			for (TimelineBlock b : blocks) {
				if (b.maximumTime <= dc.getStart() || b.minimumTime >= dc.getEnd()) {
					block = b;
					break;
				}
			}
			if (block == null) {
				block = new TimelineBlock();
				block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
				block.setOpaque(false);				
				block.add(Box.createVerticalStrut(TIMELINE_ROW_SPACING));
				block.setAlignmentX(0.5f);
				upperPanel.add(block);
				upperPanel.add(new TimelineSeparator());
				blocks.add(block);
			}			
			if (dc.getStart() < block.minimumTime) {
				block.minimumTime = dc.getStart();
			}
			if (dc.getEnd() > block.maximumTime) {
				block.maximumTime = dc.getEnd();
			}
			addActivities(ac, null, 0, new HashSet<String>(), block);
		}		
	}

	private void addActivities(AbstractComponent ac, AbstractComponent parent, int depth, Set<String> ids, TimelineBlock block) {
		DurationCapability dc = ac.getCapability(DurationCapability.class);
		if (dc != null && !ids.contains(ac.getComponentId())) {
			addViewToRow(dc, ac, (ActivityComponent) (parent instanceof ActivityComponent ? parent : null), block, depth);
			ids.add(ac.getComponentId()); // Prevent infinite loops in case of cycle
			for (AbstractComponent child : ac.getComponents()) {
				addActivities(child, ac, depth + 1, ids, block);
			}			
		}
	}
	
	private void addViewToRow(DurationCapability dc, AbstractComponent ac, ActivityComponent parent, TimelineBlock block, int row) {
		while (row >= block.rows.size()) {
			block.rows.add(new JPanel(new TimelineRowLayout()));
			block.rows.get(block.rows.size() - 1).setOpaque(false);
			block.add(block.rows.get(block.rows.size() - 1));
			block.add(Box.createVerticalStrut(TIMELINE_ROW_SPACING));
		}
		View activityView = ActivityView.VIEW_INFO.createView(ac);
		MouseAdapter controller = new TimelineDurationController(parent, dc, this);
		block.rows.get(row).add(activityView, dc);
		activityView.addMouseListener(controller);
		activityView.addMouseMotionListener(controller);
	}
	

	private class TimelineRowLayout implements LayoutManager2 {
		private Map<Component, DurationCapability> durationInfo = new HashMap<Component, DurationCapability>();
		
		@Override
		public void addLayoutComponent(String name, Component comp) {
		}

		@Override
		public void removeLayoutComponent(Component comp) {
			durationInfo.remove(comp);
		}

		@Override
		public Dimension preferredLayoutSize(Container parent) {			
			return new Dimension(0, TIMELINE_ROW_HEIGHT);
		}

		@Override
		public Dimension minimumLayoutSize(Container parent) {
			return new Dimension(0, TIMELINE_ROW_HEIGHT);
		}

		@Override
		public void layoutContainer(Container parent) {
			for (Component child : parent.getComponents()) {
				DurationCapability duration = durationInfo.get(child);
				if (duration != null) {
					int x = getLeftPadding() + (int) (getPixelScale() * (duration.getStart() - getTimeOffset()));
					int width = (int) (getPixelScale() * (duration.getEnd() - duration.getStart())) + 1;
					child.setBounds(x, 0, width, TIMELINE_ROW_HEIGHT);					
				}
			}
		}

		@Override
		public void addLayoutComponent(Component comp, Object constraints) {
			if (constraints instanceof DurationCapability) {
				durationInfo.put(comp, (DurationCapability) constraints);
			} else {
				throw new IllegalArgumentException("Only valid constraint for " + getClass().getName() + 
						" is " + DurationCapability.class.getName());
			}
		}

		@Override
		public Dimension maximumLayoutSize(Container parent) {
			return new Dimension(Integer.MAX_VALUE, TIMELINE_ROW_HEIGHT);
		}

		@Override
		public float getLayoutAlignmentX(Container target) {
			// TODO Auto-generated method stub
			return 0.5f;
		}

		@Override
		public float getLayoutAlignmentY(Container target) {
			// TODO Auto-generated method stub
			return 0.5f;
		}

		@Override
		public void invalidateLayout(Container target) {
		}
		
	}
	
	private class TimelineBlock extends JPanel {
		private static final long serialVersionUID = 3461668344855752107L;
		public long maximumTime = Long.MIN_VALUE;
		public long minimumTime = Long.MAX_VALUE;
		public List<JComponent> rows = new ArrayList<JComponent>();
	}

	private class TimelineSeparator extends JPanel {
		private static final long serialVersionUID = -8551198172846693356L;

		public TimelineSeparator() {
			setOpaque(false);
			add(Box.createVerticalStrut(1));
		}
		
		public void paintComponent(Graphics g) {
			g.drawLine(getLeftPadding(), 0, getWidth() - getRightPadding(), 0);
		}
	}
}
