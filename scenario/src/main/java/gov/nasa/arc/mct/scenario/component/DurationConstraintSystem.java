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
package gov.nasa.arc.mct.scenario.component;

import gov.nasa.arc.mct.components.AbstractComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a system of constraints for durations of activities and 
 * sub-activities. 
 * 
 * Parents must expand to fit children; decisions must span the 
 * gap between two activities, and push/pull activities as necessary
 * to retain this relationship.
 * 
 * @author vwoeltje
 *
 */
public class DurationConstraintSystem {
	private AbstractComponent root;
	private Map<DurationCapability, AbstractComponent> components =
			new HashMap<DurationCapability, AbstractComponent>();
	private Map<DurationCapability, List<DurationConstraint>> constraints =
			new HashMap<DurationCapability, List<DurationConstraint>>();
	private Map<DurationCapability, DurationEdge[]> edges =
			new HashMap<DurationCapability, DurationEdge[]>();
	
			
	/**
	 * Create a new system of constraints around the specified component.
	 * Any objects "above" the component in the user object graph 
	 * (that is, referencing components) will not be considered. 
	 * Duplicate references to components will not be considered (only 
	 * the first instance of a component encountered will be considered.)
	 * This ensures that the constraint system sees a tree structure, 
	 * with no ambiguity about how desired constraints should be 
	 * enforeced.
	 * 
	 * @param root the component at the top of the hierarchy
	 */
	public DurationConstraintSystem ( AbstractComponent root ) {
		this.root = root;
		addConstraintsFor(root, new HashSet<String>());
	}
	
	private void addConstraintsFor(AbstractComponent parent, Set<String> ignore) {
		ignore.add(parent.getComponentId());
		DurationCapability pdc = parent.getCapability(DurationCapability.class);
		if (pdc != null) {
			// Get a sorted list of children of this component
			// This also filters out duplicates
			List<AbstractComponent> children = getChildren(parent, ignore);
			
			// Add parent/child constraints
			if (children.size() > 0) {
				for (int sign : new int[]{-1, 1}) {
					for (AbstractComponent child : children) {
						DurationCapability cdc = 
								child.getCapability(DurationCapability.class);
						if (cdc != null) {
							// Permit reverse-lookup of component later
							components.put(cdc, child);
							// Parent edge pushes child's same edge
							addConstraint(pdc, sign, cdc, sign, false, false);
							// Child edge pushes parent's same edge & causes expansion
							addConstraint(cdc, sign, pdc, sign, false, true);
						}
					}
				}
			}			
			
			// Add peer constraints
			for (int i = 0; i < children.size()-1; i++) {
				AbstractComponent a = children.get(i);
				AbstractComponent b = children.get(i+1);
				DurationCapability adc = a.getCapability(DurationCapability.class);
				DurationCapability bdc = b.getCapability(DurationCapability.class);		
				if (adc != null && bdc != null) {
					// Permit reverse-lookup of component later
					components.put(adc, a);
					components.put(bdc, b);
					// Construct appropriate peer constraints (push or pull)
					boolean pulls = a instanceof DecisionComponent || b instanceof DecisionComponent;
					if (pulls) { // Activities do not constrain each other
						addConstraint(adc, 1, bdc, -1, pulls, false);
						addConstraint(bdc, -1, adc, 1, pulls, false);
					}
				}
			}
			
			// Recursively build remaining constraint hierarchy
			// (base case: When children is empty.)
			for (AbstractComponent child : children) {
				addConstraintsFor(child, ignore);
			}
		}		
	}
	
	private List<AbstractComponent> getChildren(AbstractComponent parent, Set<String> ignore) {
		List<AbstractComponent> children = new ArrayList<AbstractComponent>();
		
		// Assemble all children who offer a durationcapability
		for (AbstractComponent child : parent.getComponents()) {
			if (!ignore.contains(child.getComponentId())) { // Filter out duplicates
				if (child.getCapability(DurationCapability.class) != null){
					children.add(child);
					ignore.add(child.getComponentId());
				}
			}
		}
		
		// Sort by time; note, this assumes all children still have DurationCapability
		Collections.sort(children, new Comparator<AbstractComponent>() {
			@Override
			public int compare(AbstractComponent a, AbstractComponent b) {				
				return Long.valueOf(
							a.getCapability(DurationCapability.class).getStart()
						).compareTo(
							b.getCapability(DurationCapability.class).getStart()
						);
			}			
		});
		
		return children;
	}
	
	/**
	 * Enforce all constraints in the system. This should only 
	 * be invoked when there is not a specific component initiating 
	 * the change (as desired behavior varies depending on the 
	 * change to the component in those cases.)
	 * @return all objects changed
	 */
	public Set<AbstractComponent> changeAll() {
		return changeAll(root);
	}

	/**
	 * Enforce all constraints in the system, starting at the 
	 * specified component and proceeding down the tree. 
	 * This should only be invoked when there is not a specific 
	 * component initiating the change (as desired behavior varies 
	 * depending on the change to the component in those cases.)
	 * @param root the top of the sub-hierarchy to change
	 * @return all objects changed
	 */
	public Set<AbstractComponent> changeAll(AbstractComponent root) {
		Set<AbstractComponent> changed = new HashSet<AbstractComponent>();
		changeAll (root, new HashSet<String>(), changed);
		return changed;
	}
	
	private void changeAll(AbstractComponent root, Set<String> ignore, Set<AbstractComponent> changed) {
		if (!ignore.contains(root.getComponentId())) { // Filter out already-changed components
			ignore.add(root.getComponentId());

			// Trigger changes in children first
			for (AbstractComponent child : root.getComponents()) {
				changeAll(child, ignore, changed);
			}
			
			// Act as though the top of the sub-hierarchy got "wiggled"
			// (moved left, then right)
			DurationCapability dc = root.getCapability(DurationCapability.class);
			if (dc != null) {
				change(dc, -1, changed);
				change(dc, 1, changed);
			}
			
		}
	}
	
	/**
	 * Enforce constraints based on the specified change. 
	 * @param dc the object which changed
	 * @param sign the direction of the change (-1 = backward in time, +1 = forward)
	 * @return all objects changed by constraints
	 */
	public Set<AbstractComponent> change(DurationCapability dc, int sign) {
		Set<AbstractComponent> changed = new HashSet<AbstractComponent>();
		change(dc, sign, changed);
		return changed;
	}	
	
	private void change(DurationCapability dc, int sign, Set<AbstractComponent> changed) {
		// Get all constraints associated with the object that changed
		List<DurationConstraint> constraints = this.constraints.get(dc);
		
		// Enforce those constraints
		if (constraints != null) {
			int sz = constraints.size();
			for (int i = 0; i < sz; i++) {
				constraints.get(i).change(sign, changed);
			}
		}
	}
	
	/**
	 * Add a constraint between the two objects.
	 * @param source the object which might change
	 * @param sourceSign the relevant edge which may change (-1 is start, 1 is end)
	 * @param target the object effected by the constraint
	 * @param targetSign the relevant edge which may change (-1 is start, 1 is end)
	 * @param pulls true if the constraint should have a "pulls" behavior (false only pushes)
	 * @param expands true if the constraint should expand a container
	 */
	public void addConstraint(
			DurationCapability source, int sourceSign, 
			DurationCapability target, int targetSign,
			boolean pulls, boolean expands) {
		addDurationCapability(source);
		constraints.get(source).add(
				new DurationConstraint(
						getEdge(source, sourceSign),
						getEdge(target, targetSign),
						pulls, expands));
	}
	
	private DurationEdge getEdge(DurationCapability dc, int sign) {
		addDurationCapability(dc);
		
		// Get an appropriate edge (start or end)
		return edges.get(dc)[sign < 0 ? 0 : 1];
	}
	
	private void addDurationCapability(DurationCapability dc) {
		// Store default values for this edge in maps
		if (!edges.containsKey(dc)) {
			edges.put(dc, new DurationEdge[] {
				new DurationEdge(dc, -1), new DurationEdge(dc, 1)	
			});
		}
		if (!constraints.containsKey(dc)) {
			constraints.put(dc, new ArrayList<DurationConstraint>());
		}
	}
	
	
	private class DurationConstraint {
		private DurationEdge source;
		private DurationEdge target;
		private boolean pulls;
		private boolean expands;
		
		public DurationConstraint(DurationEdge source, DurationEdge target,
				boolean pulls, boolean expands) {
			super();
			this.source = source;
			this.target = target;
			this.pulls = pulls;
			this.expands = expands;
		}
		
		public void change(int sign, Set<AbstractComponent> changed) {
			// Get the difference between specified edges
			int cmp = target.compare(source);
			
			// "Expands" implies same edge is changed, so flip sign
			cmp *= expands ? -1 : 1;
			
			// Negative comparison implies that edge has been violated
			// (positive comparison implies there is a gap, so consider
			//  and non-zero comparison is a "violation")
			boolean violates = pulls ? (cmp != 0) : (cmp < 0);
			
			// If there is a violation, set the time of the edge
			if (violates) {
				long newValue = source.get();
				long diff = source.get() - target.get();
				if (!expands) { // Change both edges
					target.set(-1, target.get(-1) + diff);
				}							
				target.set(newValue);
				// Track changes
				AbstractComponent comp = components.get(target.dc);
				if (comp != null) {
					changed.add(components.get(target.dc));
				}
				DurationConstraintSystem.this.change(target.dc, (int)(Math.signum(diff)), changed);
			}
		}		
	}
	
	private class DurationEdge {
		private DurationCapability dc;
		private int sign;
		
		/**
		 * Create a new object describing the start or end edge 
		 * of the specified duration capability.
		 * @param dc the duration whose edge will be described
		 * @param sign the edge (-1 = start, 1 = end) to describe
		 */
		public DurationEdge(DurationCapability dc, int sign) {
			super();
			this.dc = dc;
			this.sign = sign;
		}
		
		/**
		 * Get the value associated with this edge
		 * @return the time associated with this edge
		 */
		public long get() {
			return get(1);
		}
		
		/**
		 *  Get the value associated with this edge, or a related edge
		 * @param s the edge to get (1 for this edge, -1 for the opposite edge)
		 * @return the time for the specified edge
		 */
		public long get(int s) {
			return sign * s< 0 ? dc.getStart() : dc.getEnd();
		}
		
		/**
		 * Set the time associated with this edge
		 * 
		 * @param value the new time for this edge
		 */
		public void set(long value) {
			set(1, value);
		}
		
		/**
		 * Set the time associated with this edge, or a related edge
		 * @param s the edge to set (1 for this edge, -1 for opposite edge)
		 * @param value the new time for the specified edge
		 */
		public void set(int s, long value) {
			if (sign * s < 0) {
				dc.setStart(value);
			} else {
				dc.setEnd(value);
			}
		}
		
		/**
		 * Compares a value to a given edge, taking into account 
		 * the edge's direction.
		 * 
		 * @param value the timestamp to compare
		 * @return a negative integer if bounded by edge, zero if equal
		 */
		public int compare(DurationEdge other) {
			return sign * Long.valueOf(other.get()).compareTo(get());
		}
	}
}
