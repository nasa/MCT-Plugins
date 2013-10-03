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
import gov.nasa.arc.mct.components.ObjectManager;
import gov.nasa.arc.mct.scenario.component.DurationCapability;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles merging of incoming changes (from database) into a 
 * timeline view without overwriting unsaved changes from the 
 * user. 
 * 
 * In practice, this is achieved by first taking a snapshot 
 * of unsaved changes (during the constructor call), then 
 * taking other component (typically fresh-from-persistence) 
 * and applying those unsaved changes, effectively to 
 * restore them.
 * 
 * Currently only start and end times are handled in this 
 * process. Changes to costs, base displayed name, et 
 * cetera will be lost upon a merge.
 * 
 * A more general approach might be to save and restore 
 * using ModelStatePersistence, as well as common 
 * component attributes such as BDN, etc.
 * 
 * There are some cases where this approach could have 
 * undesired results. Currently MCT does not distinguish 
 * what is dirty in a component, so BDN changes could 
 * cause a component's start/end times to be treated 
 * as unsaved changes, for example.
 * 
 * @author vwoeltje
 *
 */
public class TimelineMergeHandler {
	private Map<String, DurationCapability> dirtyDurations = 
			new HashMap<String, DurationCapability>();
	
	/**
	 * Create a new merge handler with a snapshot of unsaved 
	 * changes to a specific object and its descendants.
	 * @param dirtyParent the object whose changes should be tracked
	 */
	public TimelineMergeHandler(ObjectManager dirtyParent) {
		for (AbstractComponent dirtyChild : dirtyParent.getAllModifiedObjects()) {
			DurationCapability dc = 
					dirtyChild.getCapability(DurationCapability.class);
			if (dc != null) {
				dirtyDurations.put(dirtyChild.getComponentId(), dc);
			}
		}
	}
	
	/**
	 * Visit this component and its descendants, restoring any 
	 * unsaved start/end time changes associated with a component 
	 * (as observed during the constructor call).
	 * @param otherParent the component to which unsaved changes will be transferred
	 */
	public boolean update(AbstractComponent otherParent) {
		AbstractComponent delegate = otherParent.getWorkUnitDelegate();
		return update(delegate != null ? delegate : otherParent, 
				otherParent, new HashSet<String>());
	}
	
	private boolean update(AbstractComponent parent, AbstractComponent component, Set<String> ignore) {
		boolean updated = false;
		String id = component.getComponentId();
		if (!ignore.contains(id)) {
			ignore.add(id);
			
			// Are there unsaved changes that we want to restore?
			if (dirtyDurations.containsKey(id)) {
				DurationCapability dirty = dirtyDurations.get(id);
				DurationCapability clean = component.getCapability(DurationCapability.class);
				// Replace start and end with user's unsaved changes
				if (clean != null) {
					// DurationCapability will prohibit start > end, 
					// so be careful about ordering changes.
					if (clean.getStart() < dirty.getStart()) {
						clean.setEnd(dirty.getEnd());
						clean.setStart(dirty.getStart());
					} else {
						clean.setStart(dirty.getStart());
						clean.setEnd(dirty.getEnd());						
					}
					// Make sure the component still appears dirty
					component.save();
					ObjectManager om = parent.getCapability(ObjectManager.class);
					if (om != null) {
						om.addModifiedObject(component);
					}
					// Report that we did update a component
					updated = true;
				}
			}
			
			// Visit children
			for (AbstractComponent child : component.getComponents()) {
				updated |= update(parent, child, ignore);
			}
		}
		return updated;
	}
}
