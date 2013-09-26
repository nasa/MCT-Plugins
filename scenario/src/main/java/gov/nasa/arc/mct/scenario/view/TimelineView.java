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
import gov.nasa.arc.mct.platform.spi.PlatformAccess;
import gov.nasa.arc.mct.scenario.component.ActivityComponent;
import gov.nasa.arc.mct.scenario.component.CostFunctionCapability;
import gov.nasa.arc.mct.scenario.component.DurationCapability;
import gov.nasa.arc.mct.scenario.component.DurationConstraintSystem;
import gov.nasa.arc.mct.scenario.view.TimelineLayout.TimelineContext;
import gov.nasa.arc.mct.services.component.ViewInfo;
import gov.nasa.arc.mct.services.component.ViewType;
import gov.nasa.arc.mct.services.internal.component.ComponentInitializer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ChangeEvent;

/**
 * A view of an object showing time-based objects (Activities) laid out 
 * horizontally according to time, divided into rows. 
 * 
 * @author vwoeltje
 *
 */
public class TimelineView extends AbstractTimelineView implements TimelineContext {
	static final ViewInfo VIEW_INFO = new ViewInfo(TimelineView.class, "Timeline", ViewType.EMBEDDED);

	private static final long serialVersionUID = -5039383350178424964L;
	
	private JPanel upperPanel = new JPanel();
	private Color backgroundColor = Color.WHITE;
	private View  costGraph = null;
	private List<DurationConstraintSystem> constraints = new ArrayList<DurationConstraintSystem>();
	
	// Borrowed from NodeView - detect changes and merge into this view
    private PropertyChangeListener objectStaleListener = null;
	
	public TimelineView(AbstractComponent ac, ViewInfo vi) {
		// When we are a non-embedded view, work with a fresh copy of the 
		// component direct from persistence. This ensures that we get fresh 
		// copies of children, meaning we can propagate changes to e.g. 
		// Activities within managed views (including Timeline Inspector) 
		// without effecting the rest of the system.
		super(vi.getViewType().equals(ViewType.EMBEDDED) ?
				ac :
				(ac=PlatformAccess.getPlatform().getPersistenceProvider().getComponent(ac.getComponentId())),
				vi);		
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(upperPanel, BorderLayout.NORTH);
		upperPanel.setLayout(new BoxLayout(upperPanel, BoxLayout.Y_AXIS));
		upperPanel.setOpaque(false);
		
		getContentPane().setBackground(backgroundColor);
				
		buildUpperPanel();
		
		updateMasterDuration();
		
		refreshAll();
		
		// Refresh on any ancestor changes - these may change time scales
		this.addAncestorListener(new AncestorListener() {
			@Override
			public void ancestorAdded(AncestorEvent arg0) {
				refreshAll();
			}
			@Override
			public void ancestorMoved(AncestorEvent arg0) {
				refreshAll();
			}
			@Override
			public void ancestorRemoved(AncestorEvent arg0) {
				refreshAll();
			}			
		});
		
	}
	
	@Override
	public void viewPersisted() {
		// Always get the fresh version from the database, if we're non-embedded
		if (!getInfo().getViewType().equals(ViewType.EMBEDDED)) {
			setManifestedComponent(PlatformAccess.getPlatform().getPersistenceProvider().getComponent(getManifestedComponent().getComponentId()));
		}

		// Re-create everything; layout may be much different
		rebuildUpperPanel();
		
		// Update cost graph
		if (costGraph != null) {
			costGraph.setManifestedComponent(getManifestedComponent());
			costGraph.viewPersisted();
		}
		
		// Finally, ensure time settings are obeyed
		refreshAll();
	}
	
	private void rebuildUpperPanel() {
		upperPanel.removeAll();

		// Cache current selection to restore later
		Collection<View> selected = getSelectionProvider().getSelectedManifestations();
		String selectedId = null;
		if (!selected.isEmpty()) {
			selectedId = selected.iterator().next().getManifestedComponent().getComponentId();
			select(null);
		}
		
		// Clear all enforced constraints - these will be recreated
		constraints.clear();
		
		// Rebuild the view
		buildUpperPanel();
		
		// Restore the selection
		if (selectedId != null) {
			selectComponent(selectedId);
		}		
	}
	
	private void buildUpperPanel() {
		
		// Unregister the old listener
		if (objectStaleListener != null) {
			removePropertyChangeListener(objectStaleListener);
		}
		
		// Create a new instance & attach it (will be attached to new views as well)
		objectStaleListener = new TimelineStaleListener();
		addPropertyChangeListener(VIEW_STALE_PROPERTY, objectStaleListener);
		
		AbstractComponent ac = getManifestedComponent();
		if (!getInfo().getViewType().equals(ViewType.EMBEDDED)) { // If we're a clone, add a view manifestation of "this"
			ac.addViewManifestation(this);
		}

		// Add all children
		for (AbstractComponent child : ac.getComponents()) {
			addTopLevelActivity(child, new HashSet<String>());//addActivities(child, 0, new HashSet<String>());
		}
		
		List<CostFunctionCapability> costs = ac.getCapabilities(CostFunctionCapability.class);
		if (costs != null && !costs.isEmpty()) {
			upperPanel.add(new CollapsibleContainer(costGraph = GraphView.VIEW_INFO.createView(ac)));
		}

	}

	private void refreshAll() {
		revalidate();
		repaint();
		for (Component c : upperPanel.getComponents()) {
			c.invalidate();
			c.validate();
			c.repaint();
		}
	}
	
	
	@Override
	public void stateChanged(ChangeEvent e) {
		super.stateChanged(e);
		refreshAll();
	}

	@Override
	public void save() {
		// Should be called whenever something in this view has been saved
		super.save();
		
		// Make sure constraints still apply
		// (for instance, to reflect changes from Timeline Inspector)
		for (DurationConstraintSystem constraint : constraints) {
			constraint.changeAll();
		}
		
		// Expand visible bounds if necessary
		updateMasterDuration();
		
		// Force layout, re-display
		refreshAll();
	}

	private void addTopLevelActivity(AbstractComponent ac, Set<String> ignore) {
		DurationCapability dc = ac.getCapability(DurationCapability.class);
		if (dc != null) {
			// Every top-level activity gets its own block
			TimelineBlock block = new TimelineBlock();
			block.setOpaque(false);				
			block.setAlignmentX(0.5f);
			upperPanel.add(block);
						
			// Each top-level activity also has its own constraint system
			// (for dealing with changes to child activities)
			DurationConstraintSystem constraint = new DurationConstraintSystem(ac);
			constraints.add(constraint);
			
			// Poke all objects to resolve constraints
			Set<AbstractComponent> changes = constraint.changeAll(ac);
			
			// Save any that were changed
			for (AbstractComponent change : changes) {
				change.save();
			}
			
			// If there were changes, also save top-level timeline
			if (!changes.isEmpty()) {
				getManifestedComponent().save();
			}
			
			// Populate the block with activities
			addActivities(ac, null, 0, new HashSet<String>(), block, constraint);
		} else if (!ignore.contains(ac.getComponentId())){  // Avoid cycles
			ignore.add(ac.getComponentId());
			
			// Since there is no DurationCapability, must be collection 
			// or similar, so probe down for other activities (treat as top-level)
			for (AbstractComponent child : ac.getComponents()) {
				addTopLevelActivity(child, ignore);
			}
		}
	}

	private void addActivities(AbstractComponent ac, AbstractComponent parent, int depth, Set<String> ids, TimelineBlock block, DurationConstraintSystem constraints) {
		DurationCapability dc = ac.getCapability(DurationCapability.class);		
		if (dc != null && !ids.contains(ac.getComponentId())) {
			// Using workunitdelegate means these views will sync with inspector
			// If we already have a delegate, use that; otherwise, use manifested component
			// (this strategy permits parent views, like Scenario, to take over)
			AbstractComponent manifestedComponent = getManifestedComponent();
			AbstractComponent workDelegate = manifestedComponent.getWorkUnitDelegate();
			ac.getCapability(ComponentInitializer.class).setWorkUnitDelegate(workDelegate != null ? workDelegate : manifestedComponent);
			addViewToRow(dc, ac, (ActivityComponent) (parent instanceof ActivityComponent ? parent : null), block, depth, constraints);
			ids.add(ac.getComponentId()); // Prevent infinite loops in case of cycle
			for (AbstractComponent child : ac.getComponents()) {
				addActivities(child, ac, depth + 1, ids, block, constraints);
			}			
		}
	}
	
	private void addViewToRow(DurationCapability dc, AbstractComponent ac, ActivityComponent parent, TimelineBlock block, int row, DurationConstraintSystem constraints) {

		View activityView = ActivityView.VIEW_INFO.createView(ac);
		
		MouseAdapter controller = new TimelineDurationController(dc, this, constraints);
		block.add(activityView, dc);

		activityView.addMouseListener(controller);
		activityView.addMouseMotionListener(controller);
		
		activityView.addPropertyChangeListener(VIEW_STALE_PROPERTY, objectStaleListener);
	}

	
	private class TimelineBlock extends JPanel {
		private static final long serialVersionUID = 3461668344855752107L;
		
		public TimelineBlock() {
			super(new TimelineLayout(TimelineView.this));
		}
		
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawLine(getLeftPadding(), getHeight()-1, getWidth() - getRightPadding(), getHeight()-1);
		}
	}

	@Override
	public Set<Component> getActiveViews() {		
		return Collections.emptySet();
	}

	/**
	 * Since stale events may be fired by multiple components at once, 
	 * this class executes once only (after the first execution, the 
	 * GUI will have been rebuilt anyway.)
	 * 
	 * It is necessary to create a new instance of this for every 
	 * rebuildUpperPanel due to this behavior.
	 */
	private class TimelineStaleListener implements PropertyChangeListener { 
		private boolean used = false;

		@Override
		public void propertyChange(java.beans.PropertyChangeEvent evt) {
			// Redundant updates may be sent to new views
			// Verify object is really stale to avoid update cascade
			Object src = evt.getSource();
			if ((Boolean) evt.getNewValue() && 
				src instanceof View && 
				((View)src).getManifestedComponent().isStale() ) {
				
				// Similar to NodeView, get latest from persistence.
				// Check used flag to only execute once, as after this 
				// the view will have been remade anyway.
				if (!used && 
					getManifestedComponent().getComponentId() != null) {
					// Get the latest version of the object from persistence
					AbstractComponent committedComponent = 
							PlatformAccess.getPlatform().getPersistenceProvider()
							.getComponent(getManifestedComponent().getComponentId());

					// For future compatibility 
					//ObjectManager om = getManifestedComponent().getCapability(ObjectManager.class);					
					
					// Propagate unsaved changes to the newer component
					boolean updated = 
							new TimelineMergeHandler(getManifestedComponent())
							.update(committedComponent);
					setManifestedComponent(committedComponent);
					updateMasterDuration();
					rebuildUpperPanel();
						
					// Flag this as used - don't repeat the above for other stale notifications
					used = true;
					
					// Invoke the view's "save" if there was an update, to ensure Save All remains visible
					if (updated) {
						save();
					}
				}
				
			}
		}
	}
}
