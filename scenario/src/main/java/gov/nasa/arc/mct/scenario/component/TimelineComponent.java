package gov.nasa.arc.mct.scenario.component;

import gov.nasa.arc.mct.components.AbstractComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
		for (AbstractComponent child : getComponents()) {
			DurationCapability dc = child.getCapability(DurationCapability.class);
			if (dc != null) {
				long childEnd = dc.getEnd();
				if (childEnd > end) {
					end = childEnd;
				}
			}
		}
		return end;
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