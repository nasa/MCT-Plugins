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

import gov.nasa.arc.mct.scenario.component.DurationCapability;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.Timer;

/**
 * Lays out objects in a timeline (more specifically, in one block of a timeline)
 * This layout is responsible for positioning components horizontally to 
 * reflect their start/end times, and for shifting components vertically 
 * (and animating this transition) to prevent them from overlapping.
 * 
 * @author vwoeltje
 *
 */
public class TimelineLayout implements LayoutManager2 {
	private int rowHeight = 24;
	private int rowPadding = 6;
	private TimelineContext context;
	private List<List<ComponentInfo>> rows = new ArrayList<List<ComponentInfo>>();
	
	private Map<Component, ComponentInfo> componentInfo = new HashMap<Component, ComponentInfo>();
	private Set<ComponentInfo> animating = new HashSet<ComponentInfo>();
	
	private Timer animator = null;	
	private boolean rowAssignmentsHandled = false;

	private static class ComponentInfo {
		// Start and end times
		private DurationCapability durationCapability;
		
		// Current row for the component
		private int row = 0;
		
		// Current animation (vertical offset, in rows) for component
		private float animation = 0f;
		
		// Order in which component was added
		private int order;

		public ComponentInfo(DurationCapability durationCapability, int order) {
			super();
			this.durationCapability = durationCapability;
			this.order = order;
		}
	}
	
	
	public TimelineLayout(TimelineContext context) {
		super();
		this.context = context;
	}

	@Override
	public void addLayoutComponent(String name, Component comp) {
	}

	@Override
	public void removeLayoutComponent(Component comp) {
		if (componentInfo.containsKey(comp)) {
			int row = componentInfo.get(comp).row;
			rows.get(row).remove(comp);
			animating.remove(componentInfo.get(comp));
			componentInfo.remove(comp);
		}
		// TODO: Adjust other orders
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) {			
		return new Dimension(0, getHeight());
	}

	@Override
	public Dimension minimumLayoutSize(Container parent) {
		return new Dimension(0, getHeight());
	}

	@Override
	public void layoutContainer(final Container parent) {
		int fullRowHeight = rowHeight + rowPadding;
		
		handleRowAssignments();
		
		// Lay out components temporally
		for (Component child : parent.getComponents()) {
			ComponentInfo info = componentInfo.get(child);
			if (componentInfo != null) {
				int x = context.getLeftPadding() + (int) (context.getPixelScale() * (double) (info.durationCapability.getStart() - context.getTimeOffset()));
				int width = (int) (context.getPixelScale() * (double) (info.durationCapability.getEnd() - info.durationCapability.getStart())) + 1;
				
				// Vertical offset due to row change animation
				int animationOffset = (int) (fullRowHeight * info.animation);
				
				child.setBounds(x, info.row * fullRowHeight + animationOffset + rowPadding/2, width, rowHeight);					
			}
		}
		
		// Start animating if needed
		if (!animating.isEmpty() && animator == null) {
			animator = new Timer(25, new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent evt) {
					Iterator<ComponentInfo> iter = animating.iterator();
					while (iter.hasNext()) {
						ComponentInfo component = iter.next();
						component.animation *= 0.75f;
						if (Math.abs(component.animation) <  1.0f / ((float)rowHeight)) {
							component.animation = 0f;
							iter.remove();
						}
					}		
					
					if (animating.isEmpty()) {
						animator.stop();
						animator = null;
					}
					
					// Update layout for parents, since height may have changed
					// TODO: Only do this if height will change?
					Container p = parent;
					while (p != null) {
						p.invalidate();
						p.validate();
						p.repaint();
						p = p.getParent();
					}
				}				
			});
			animator.start();
		}
		
		rowAssignmentsHandled = false;
	}

	@Override
	public void addLayoutComponent(Component comp, Object constraints) {
		if (constraints instanceof DurationCapability) {
			ComponentInfo info = new ComponentInfo((DurationCapability) constraints, componentInfo.size());
			componentInfo.put(comp, info);

			// Find a row with room for this component
			int row = 0;
			while (row < rows.size()) {
				boolean fits = true;
				for (ComponentInfo c : rows.get(row)) {
					if (overlaps(c,info)) {
						fits = false;
						break;
					}
				}
				if (fits) {
					break;
				} else {
					row++;
				}
			}
			setRow(info, row, false); // Will get sorted during layout
		} else {
			throw new IllegalArgumentException("Only valid constraint for " + getClass().getName() + 
					" is " + DurationCapability.class.getName());
		}
	}

	@Override
	public Dimension maximumLayoutSize(Container parent) {
		return new Dimension(Integer.MAX_VALUE, getHeight());
	}

	@Override
	public float getLayoutAlignmentX(Container target) {
		return 0.5f;
	}

	@Override
	public float getLayoutAlignmentY(Container target) {
		return 0.5f;
	}

	@Override
	public void invalidateLayout(Container target) {
	}
	
	private void handleRowAssignments() {
		if (!rowAssignmentsHandled) {
			// Fix row assignments
			for (List<ComponentInfo> row : rows) {
				Collections.sort(row, comparator);
			}
			for (int i = 0; i < rows.size(); i++) {
				cleanupRow(i);
			}
			for (int i = 1; i < rows.size(); i++) {
				packRow(i);
			}
			
			rowAssignmentsHandled = true;
		}
	}
	
	private int getHeight() {
		int fullRowHeight = rowHeight + rowPadding;
		
		// TODO: Compute this only on changes?
		handleRowAssignments();
		
		// Find maximum row that is not being animated to
		int bestRow = rows.size() - 1;
		while (bestRow > 0 && !containsUnanimatedComponents(bestRow)) {
			bestRow--;
		}
		int desiredHeight = (bestRow + 1) * fullRowHeight;
		for (ComponentInfo info : animating) {
			int necessaryHeight = 
					(int) (((float) info.row + info.animation + 1) * fullRowHeight);   
			desiredHeight = Math.max(desiredHeight, necessaryHeight);
		}		
		return desiredHeight;
	}
	
	private boolean containsUnanimatedComponents(int row) {
		for (ComponentInfo c : rows.get(row)) {
			if (!animating.contains(c)) {
				return true;
			}				
		}
		return false;
	}
			
	private void setRow(ComponentInfo info, int row, boolean animated) {
		// This will be -1 by default, to indicate "unset"
		int original = info.row;
		info.row = row;
		
		while (rows.size() <= row) {
			rows.add(new ArrayList<ComponentInfo>());
		}
		if (rows.size() > original) {
			rows.get(original).remove(info);
		}
		rows.get(row).add(info);
		
		if (original != row && animated) {
			info.animation += (float) (original - row);			
			animating.add(info);
		}
	}
	
	private List<ComponentInfo> fixList = new ArrayList<ComponentInfo>();
	
	private void cleanupRow(int row) {
		Set<Component> active = context.getActiveViews();
		List<ComponentInfo> comps = rows.get(row);
		
		// Identify comps which should be pushed down a row
		for (int i = 0; i < comps.size(); i++) {
			ComponentInfo c1 = comps.get(i);
			for (int j = i + 1; j < comps.size(); j++) {
				ComponentInfo c2 = comps.get(j);
				if (!c1.equals(c2)) {
					ComponentInfo toMove = toMove(c1, c2);
					if (toMove != null) {
						if (!active.contains(toMove)) {
							fixList.add(toMove);
						} else {
							fixList.add(toMove.equals(c1) ? c2 : c1);
						}
					} else {
						break; // c1 is clear
					}
				}
			}
		}
		
		// Push them
		for (ComponentInfo c : fixList) {
			setRow(c, row + 1, true);
		}
		
		// Make sure row is sorted before subsequent cleanupRow
		if (!fixList.isEmpty()) {
			Collections.sort(rows.get(row + 1), comparator);
		}
		
		// Clear out the list to reuse later
		fixList.clear();
	}
	
	private Map<ComponentInfo, Integer> fixMap = new HashMap<ComponentInfo, Integer>();
	
	/*
	 * We assume cleanupRow has already been called for all rows, 
	 * such that any two components on same row don't overlap
	 */
	private void packRow(int row) {
		Set<Component> active = context.getActiveViews();
		List<ComponentInfo> current = rows.get(row);
		for (ComponentInfo c1 : current) {
			if (!active.contains(c1)) {
				outer: for (int other = row - 1; other >= 0; other--) {
					List<ComponentInfo> above = rows.get(other);

					for (ComponentInfo c2 : above) {
						if (c1 != c2 && overlaps(c1, c2)) {
							break outer;
						}
					}
					
					// If control reaches here, this fits
					fixMap.put(c1, other);
				}
			}
		}

		for (Entry<ComponentInfo, Integer> fix : fixMap.entrySet()) {
			setRow(fix.getKey(), fix.getValue(), true);
		}
		fixMap.clear();

	}
	
	private ComponentInfo toMove(ComponentInfo c1, ComponentInfo c2) {
		if (!overlaps(c1, c2)) {
			return null;
		}
		
		// Prefer to move the most recently-added component downward
		if (c1.order < c2.order) {
			return c2;
		} else if (c2.order < c1.order) {
			return c1;
		}
		
		// Probably unreachable, but fall back to moving larger activities
		// if that is necessary
		DurationCapability d1 = c1.durationCapability;
		DurationCapability d2 = c2.durationCapability;
		return (d1.getEnd() - d1.getStart() > d2.getEnd() - d2.getStart()) ? c2 : c1;
	}
	
	private boolean overlaps(ComponentInfo c1, ComponentInfo c2) {
		DurationCapability d1 = c1.durationCapability;
		DurationCapability d2 = c2.durationCapability;
		return (d1.getEnd() > d2.getStart() && d2.getEnd() > d1.getStart());
	}
	
	private static final Comparator<ComponentInfo> comparator = new Comparator<ComponentInfo>() {
		@Override
		public int compare(ComponentInfo c1, ComponentInfo c2) {
			long s1 = c1.durationCapability.getStart();
			long s2 = c2.durationCapability.getStart();
			return (int) Math.signum(s1 - s2);
		}
		
	};
	
	/**
	 * Used to convey start/end times etc to a timeline layout
	 *
	 */
	public interface TimelineContext {
		/**
		 * Get the number of pixels to pad the left edge of the layout with
		 * @return left padding, in pixels
		 */
		public int getLeftPadding();
		
		/**
		 * Get the scale at which to draw/layout the timeline, in 
		 * milliseconds per pixel.
		 * @return the scale of the visible area, in ms/pixel
		 */
		public double getPixelScale();
		
		/**
		 * Get the time offset of the displayable area. That is, 
		 * how many milliseconds does the left edge correspond 
		 * to?
		 * @return time offset, in pixels, of this area
		 */
		public long getTimeOffset();
		
		/**
		 * Get any "active" views, such as those being moved by the 
		 * user. The layout will treat these specially and avoid 
		 * changing their row, to keep the object that a user is 
		 * manipulating from bouncing around.
		 * @return the set of actively-manipulated components
		 */
		public Set<Component> getActiveViews();
	}

}
