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
import gov.nasa.arc.mct.gui.SelectionProvider;
import gov.nasa.arc.mct.gui.View;
import gov.nasa.arc.mct.scenario.component.DurationCapability;
import gov.nasa.arc.mct.services.component.ViewInfo;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Abstract superclass for views which display timeline data. The motivation for 
 * defining a parent class for these is to consolidate usage of TimelineLocalControls, 
 * which should be included for all such views at the top level. 
 * (these views are often composited; note that TimelineLocalControls will   
 * suppress its own visibility and derive settings when nested within another 
 * TimelineLocalControls instance.)
 * 
 * @author vwoeltje
 *
 */
public abstract class AbstractTimelineView extends View implements ChangeListener {


	private static final long serialVersionUID = -5683099761127087080L;
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTimelineView.class);
	
	// Much start/end time behavior is deferred to the local controls
	private TimelineLocalControls timelineContainer;
	
	public AbstractTimelineView(AbstractComponent ac, ViewInfo vi) {
		super(ac,vi);
		setOpaque(false);
		// Configure based on DurationCapability
		DurationCapability dc = ac.getCapability(DurationCapability.class) ;
		if (dc != null) {
			timelineContainer = new TimelineLocalControls(dc);
			add(timelineContainer);
			timelineContainer.addChangeListener(this);
		} else {
			LOGGER.warn(getClass().getName() + " instantiated for component without DurationCapability. " +
					"This should have been prevented by policy. Subsequent errors anticipated.");
		}
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				select(null); // IF a click makes it here, de-select!
			}			
		});
	}
	
	/**
	 * Get the start time currently displayed, in milliseconds
	 * @return start time in milliseconds
	 */
	public long getStart() {
		return timelineContainer.getStart();
	}
	
	/**
	 * Get the end time currently displayed, in milliseconds
	 * @return end time in milliseconds
	 */
	public long getEnd() {
		return timelineContainer.getEnd();
	}
	
	/**
	 * Get the pixel scale currently being displayed. This is taken as the number of 
	 * pixels per millisecond (generally, this is must less than 1.0)
	 * @return number of pixels in a millisecond
	 */
	public double getPixelScale() {
		return timelineContainer != null ?
				timelineContainer.getPixelScale() : 
				1.0;
	}
	
	public long getTimeOffset() {
		return timelineContainer != null ? timelineContainer.getTimeOffset() : 0;
	}
	
	/**
	 * Get the number of pixels used for padding on the left-hand side.
	 * This, in conjunction with getPixelScale() and getTimeOffset() allows 
	 * easy conversion from X values to Time values, and vice versa.
	 * @return the number of pixels used for padding on the left
	 */
	public int getLeftPadding() {
		return timelineContainer.getLeftPadding();
	}
	
	/**
	 * Get the number of pixels used for padding on the right-hand side.
	 * @return the number of pixels used for padding on the right
	 */
	public int getRightPadding() {
		return timelineContainer.getRightPadding();
	}
	
	@Override
	public boolean isContentOwner() {
		return true;
	}
	

	/**
	 * Get the component in which content can be placed. In practice, 
	 * this should be populated with the view-specific part of sub-classes; 
	 * that is, stuff other than the common timeline local controls 
	 * should be added to the content pane.
	 * @return the content area for this view
	 */
	protected JComponent getContentPane() {
		return timelineContainer != null ? timelineContainer.getContentPane() : this; 
	}

	@Override
	public void stateChanged(ChangeEvent e) {		
		AbstractTimelineView parent = (AbstractTimelineView) SwingUtilities.getAncestorOfClass(AbstractTimelineView.class, this);
		// Propagate changes up; ensures that Scenario, for example, stays in sync with timelines
		if (parent != null) {
			parent.stateChanged(e);
		}
		revalidate();
		repaint();
	}
	
	@Override
	public SelectionProvider getSelectionProvider() {
		return timelineContainer;
	}
	
	/**
	 * Select the specified view
	 * @param view the view to select
	 */
	public void select(View view) {
		timelineContainer.select(view);
	}
	
	/**
	 * Try to select a specific component (by id)
	 * If there is no embedded view of a component with this id, this
	 * method does nothing.
	 * 
	 * @param componentId the id of a component to select
	 */
	public void selectComponent(String componentId) {
		timelineContainer.selectComponent(componentId);
	}
	
	/**
	 * Invoke save on the component in this view, as well as for any parent views
	 */
	public void save() {
		getManifestedComponent().save();
		AbstractTimelineView parent = (AbstractTimelineView) SwingUtilities.getAncestorOfClass(AbstractTimelineView.class, this);
		if (parent != null) {
			parent.save();
		}
		repaint();
		revalidate();
	}
	
	public void updateMasterDuration() {
		DurationCapability dc = getManifestedComponent().getCapability(DurationCapability.class);
		if (dc != null) {
			timelineContainer.updateMasterDuration(dc);
		}
	}
}
