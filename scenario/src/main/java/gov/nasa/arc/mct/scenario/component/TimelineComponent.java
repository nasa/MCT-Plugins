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

import java.util.HashSet;
import java.util.Set;

/**
 * A Timeline serves as a container for activities. Multiple timelines may be arranged 
 * within a Scenario.
 * 
 *
 */
public class TimelineComponent extends CostFunctionComponent implements DurationCapability {
	
	public String getDisplay(){
		return this.getDisplayName();
	}

	
	@Override
	protected <T> T handleGetCapability(Class<T> capability) {
		if (capability.isAssignableFrom(getClass())) {
			return capability.cast(this);
		}
		return super.handleGetCapability(capability);
	}
	
	@Override
	public Set<AbstractComponent> getAllModifiedObjects() {
		// Note that this is necessary to support Save All
		// ("all modified objects" is the All in Save All;
		//  typically, this should be all dirty children.)		
		
		// TODO: What about cycles?
		Set<AbstractComponent> modified = new HashSet<AbstractComponent>();
		for (AbstractComponent child : getComponents()) {
			if (child.isDirty()) {
				modified.add(child);
			}
			modified.addAll(child.getAllModifiedObjects());
		}
		return modified;
	}
	

	@Override
	public long getStart() {		
		// Time is measured as "milliseconds since start of timeline", so this is always 0
		return 0l;
	}
	@Override
	public long getEnd() {
		// The end of a Timeline is the end of its last activity
		long end = 0;
		Set<String> ignore = new HashSet<String>();
		for (AbstractComponent child : getComponents()) {
			end = Math.max(end, getEndFor(child, ignore));
		}
		return end;
	}
	
	// Recurse into collections to find possible end points
	// until base case of a component with a duration or a 
	// component with no children is reached.
	// Maintain list of things to ignore, to avoid cycles
	private long getEndFor(AbstractComponent child, Set<String> ignore) {
		ignore.add(child.getComponentId());
		DurationCapability dc = child.getCapability(DurationCapability.class);
		if (dc != null) {
			return dc.getEnd();
		} else {
			long end = 0;
			for (AbstractComponent grandchild : child.getComponents()) {
				if (!ignore.contains(grandchild.getComponentId())) {
					end = Math.max(end, getEndFor(grandchild, ignore));
				}
			}
			return end;
		}
	}
	
	@Override
	public void setStart(long start) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setEnd(long end) {
		// TODO Auto-generated method stub
		
	}
}