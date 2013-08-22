package gov.nasa.arc.mct.scenario.component;

import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.components.JAXBModelStatePersistence;
import gov.nasa.arc.mct.components.ModelStatePersistence;
import gov.nasa.arc.mct.components.PropertyDescriptor;
import gov.nasa.arc.mct.components.PropertyDescriptor.VisualControlDescriptor;
import gov.nasa.arc.mct.scenario.component.TimePropertyEditor.TimeProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents an Activity user object.
 * 
 * Activities have start and end times, associated costs, and plain-text types. 
 * Activities may contain "sub-activities" (other activities); note that these 
 * sub-activities must always be contained entirely within their parent's 
 * time span.
 *
 */
public class ActivityComponent extends CostFunctionComponent implements DurationCapability {
	private final AtomicReference<ActivityModelRole> model = new AtomicReference<ActivityModelRole>(new ActivityModelRole());
	
	/**
	 * Get the underlying data about this Activity (start time, end time, costs, type...)
	 * @return underlying activity data
	 */
	public ActivityData getData() {
		return getModel().getData();
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
	protected <T> T handleGetCapability(Class<T> capability) {
		// Note: Don't report self as capability until initialized.
		if (capability.isAssignableFrom(getClass()) && getData().getEndTime() != null) {
			return capability.cast(this);
		}
		if (capability.isAssignableFrom(ModelStatePersistence.class)) {
		    JAXBModelStatePersistence<ActivityModelRole> persistence = new JAXBModelStatePersistence<ActivityModelRole>() {

				@Override
				protected ActivityModelRole getStateToPersist() {
					return model.get();
				}

				@Override
				protected void setPersistentState(ActivityModelRole modelState) {
					model.set(modelState);
				}

				@Override
				protected Class<ActivityModelRole> getJAXBClass() {
					return ActivityModelRole.class;
				}
		        
			};
			
			return capability.cast(persistence);
		}
		return null;
	}
	
	@Override
	public List<CostFunctionCapability> getInternalCostFunctions() {
		return Arrays.<CostFunctionCapability>asList(new CostFunctionStub(true), new CostFunctionStub(false));
	}

	/**
	 * Get the container for underlying Activity data
	 * @return the container for underlying Activity data (its model)
	 */
	public ActivityModelRole getModel() {
		return model.get();
	}

	@Override
	public List<PropertyDescriptor> getFieldDescriptors()  {

		// Provide an ordered list of fields to be included in the MCT Platform's InfoView.
		List<PropertyDescriptor> fields = new ArrayList<PropertyDescriptor>();


		// We specify a mutable text field.  The control display's values are maintained in the business model
		// via the PropertyEditor object.  When a new value is to be set, the editor also validates the prospective value.
		PropertyDescriptor type = new PropertyDescriptor("Activity Type",
				new TypePropertyEditor(this), VisualControlDescriptor.TextField);
		type.setFieldMutable(true);
		PropertyDescriptor startTime = new PropertyDescriptor("Start Time", 
				new TimePropertyEditor(this, TimeProperty.START),  VisualControlDescriptor.TextField);
		startTime.setFieldMutable(true);
		PropertyDescriptor endTime = new PropertyDescriptor("End Time", 
				new TimePropertyEditor(this, TimeProperty.END),  VisualControlDescriptor.TextField);
		endTime.setFieldMutable(true);
		PropertyDescriptor duration = new PropertyDescriptor("Duration",
				new TimePropertyEditor(this, TimeProperty.DURATION), VisualControlDescriptor.TextField);
		duration.setFieldMutable(true);
		PropertyDescriptor power = new PropertyDescriptor("Power (W)", 
				new PowerPropertyEditor(this),  VisualControlDescriptor.TextField);
		power.setFieldMutable(true);
		PropertyDescriptor comm = new PropertyDescriptor("Comm (Kb/s)", 
				new CommPropertyEditor(this),  VisualControlDescriptor.TextField);
		comm.setFieldMutable(true);
		PropertyDescriptor notes = new PropertyDescriptor("Notes", 
				new NotesPropertyEditor(this),  VisualControlDescriptor.TextArea);
		notes.setFieldMutable(true);

		fields.add(type);
		fields.add(startTime);
		fields.add(endTime);
		fields.add(duration);
		fields.add(power);
		fields.add(comm);
		fields.add(notes);

		return fields;
	}
	
	/**
	 * Get the named type associated with this activity.
	 * @return
	 */
	public String getType() {
		return getData().getActivityType();
	}

	@Override
	public long getStart() {
		return getData().getStartTime().getTime();
	}

	@Override
	public long getEnd() {
		return getData().getEndTime().getTime();
	}	
	
	public void setType(String type) {
		getData().setActivityType(type);
		//save();
	}

	@Override
	public void setStart(long start) {
		long old = getStart();
		getData().setStartDate(new Date(start));
		//save();		
		if (old < start) {
			constrainToDuration();
		}
	}

	@Override
	public void setEnd(long end) {
		long old = getEnd();
		getData().setEndDate(new Date(end));
		//save();
		if (old > end) {
			constrainToDuration();
		}
	}

	/**
	 * Constrain the duration of this Activity's child objects, based on changes 
	 * to some other duration. 
	 * 
	 * It is desirable that changes initiated explicitly by the user (through the 
	 * GUI, for instance) are not "undone" by these constraints, if possible. 
	 * The arguments to this method indicate the source of this change, allowing 
	 * constraints to be enforced relative to that. (For instance, if two 
	 * sub-activities overlap, one must be pushed forward and/or the other must 
	 * be pushed backward in time. The decision of which to push is made using 
	 * these arguments.)
	 * 
	 * TODO: These constraints are not consistently enforced outside of the 
	 * Timeline View GUI. Note that it is possible to create object graphs 
	 * which cannot enforce constraints consistently (for instance, 
	 * when one sub-activity has multiple parents). It is currently not 
	 * well-defined what should happen in this situation - it may be necessary 
	 * to re-think the manner in which sub-activity start/end times are 
	 * defined. 
	 * 
	 * @param source the Activity or other child object which has changed
     * @param isStart true if the change in time was toward the start (i.e. negative)
	 */
	public void constrainChildren(DurationCapability source, boolean isStart) {
		constrainActivities(source, isStart);
		constrainDecisions(isStart);
		constrainToDuration();
	}
	
	/**
	 * Ensure that all sub-activities fit within this activity. If possible, 
	 * simply push activities inward. If there is not "empty space" between 
	 * sub-activities with which to do this, then simply squash them 
	 * proportionally. 
	 */
	private void constrainToDuration() {
		// Note that durations are in ms
		long minimum = getStart();
		long maximum = getEnd();
		long duration = maximum - minimum;
		long childDuration = 0; // Used to track total duration of children
		DurationCapability latest = null;
		DurationCapability earliest = null;
		for (AbstractComponent child : getComponents()) {
			DurationCapability dc = child.getCapability(DurationCapability.class);
			// Check if capability is supported, and of non-zero duration
			if (dc != null && dc.getStart() != dc.getEnd()) {
				// Identify minimum/maximum times
				if (dc.getStart() < minimum) {
					earliest = dc;
					minimum = dc.getStart();
				}
				if (dc.getEnd() > maximum) {
					latest = dc;
					maximum = dc.getEnd();
				}
				// Track total duration; this is used to decide if we must squash to fit
				childDuration += dc.getEnd() - dc.getStart();
			}
		}
		if (maximum > getEnd() && minimum < getStart()) { // Squash children to fit, if needed
			if (childDuration > duration) {
				double durationFactor = ((double) duration) / ((double) childDuration);
				for (AbstractComponent child : getComponents()) {
					DurationCapability dc = child.getCapability(DurationCapability.class);
					if (dc != null) { // If getCapability returned null, capability is unsupported by this child
						long delta = (dc.getEnd() - dc.getStart() - (long) ((dc.getEnd() - dc.getStart()) * durationFactor)) / 2 + 1;
						dc.setEnd(dc.getEnd() - delta);
						dc.setStart(dc.getStart() + delta);
					}
				}
				minimum = earliest.getStart();
				maximum = latest.getEnd();
			}
		}
		long delta = 0L;
		if (maximum > getEnd()) { // Push in from the end, if needed
				delta = getEnd() - maximum;
				latest.setEnd(getEnd());
				latest.setStart(latest.getStart() + delta);
				constrainActivities(latest, true);
				constrainDecisions(true);
		} 
		if (minimum < getStart()) { // Push in from the start, if needed
				delta = getStart() - minimum;
				earliest.setEnd(earliest.getEnd() + delta);
				earliest.setStart(getStart());
				constrainActivities(earliest, false);
				constrainDecisions(false);
		}
	}
	
	/**
	 * Constrain this component's sub-activities 
	 * (decisions are handled in a separate pass)
	 * 
	 * @param source the Activity or other child object which has changed
	 * @param isStart true if the change in time was toward the start (i.e. negative)
	 */
	private void constrainActivities(DurationCapability source, boolean isStart) {
		int sign = isStart ? 1 : -1;
		long movingEdge = isStart ? source.getStart() : source.getEnd();
		long mostOverlapping = movingEdge;
		DurationCapability durationCapabilityToShift = null;
		
		// Determine which child component must be shifted by this change, if any
		for (AbstractComponent child : getComponents()) {
			DurationCapability dc = child
					.getCapability(DurationCapability.class);
			// Filter out children without capabilities, the source, and non-overlapping children
			if (dc != null && dc != source && overlaps(dc, source)) {
				long movedEdge = isStart ? dc.getEnd() : dc.getStart();
				if (movedEdge * sign > mostOverlapping * sign) {
					mostOverlapping = movedEdge;
					durationCapabilityToShift = dc;
				}
			}
		}
		
		// Shift the identified child forward/backward. 
		// This may push other children via a recursive call.
		if (durationCapabilityToShift != null) {
			long delta = movingEdge - mostOverlapping;
			durationCapabilityToShift.setStart(
					durationCapabilityToShift.getStart() + delta);
			durationCapabilityToShift.setEnd(
					durationCapabilityToShift.getEnd() + delta);
			constrainActivities(durationCapabilityToShift, delta < 0);
		}
		
		// TODO: The search-shift-recurse algorithm used here is O(n^2)
		//       for n sub-activities.
		//       An O(n lg n) iterative solution should be possible.
		//       (i.e. sort, then shift as necessary in one pass)
	}
	
	/**
	 * Constrain this component's sub-activities 
	 * (decisions are handled in a separate pass)
	 * 
	 * @param movingTowardStart true if the change was toward start (i.e. negative)
	 */
	private void constrainDecisions(boolean movingTowardStart) {
		// Enforce special positioning rules for Decisions
		// Decisions should never have empty space before or after,
		// but should also maintain consistent duration
		boolean moved = false;
		do {
			moved = false;
			for (AbstractComponent child : getComponents()) {
				// Identify decisions among children
				if (child instanceof DecisionComponent) {
					long start = ((DecisionComponent) child).getStart();
					long end = ((DecisionComponent) child).getEnd();
					DurationCapability preceedingCapability = null;
					DurationCapability followingCapability = null;
					long nearestPrecedent = getStart();
					long nearestFollower = getEnd();
					
					// Search for nearest preceeding/following siblings of decision
					for (AbstractComponent otherChild : getComponents()) {
						if (child != otherChild) {
							DurationCapability dc = otherChild
									.getCapability(DurationCapability.class);
							if (dc != null) { // getCapability returns null if otherChild does not offer this capability
								if (dc.getEnd() > nearestPrecedent && dc.getEnd() <= start) {
									preceedingCapability = dc;
									nearestPrecedent = dc.getEnd();
								}
								if (dc.getStart() < nearestFollower && dc.getStart() >= end) {
									 followingCapability = dc;
									 nearestFollower = dc.getStart();
								}
							}
						}
					}
					
					// If a preceeding capability was found, and there is a gap, move one or the other
					if (preceedingCapability != null && nearestPrecedent < start && !overlaps((DurationCapability) child, preceedingCapability)) {
						// Decide which capability to move and how far to move it based on the direction of the initiating change
						long delta = (start - nearestPrecedent) * (movingTowardStart ? -1 : 1);						
						DurationCapability toMove = (DurationCapability) (movingTowardStart ? child : preceedingCapability);
						toMove.setStart(toMove.getStart() + delta);
						toMove.setEnd(toMove.getEnd() + delta);
						moved = true;
					}
					// If a following capability was found, and there is a gap, move one or the other
					if (followingCapability != null && nearestFollower > end && !overlaps((DurationCapability) child, followingCapability)) {
						// Decide which capability to move and how far to move it based on the direction of the initiating change
						long delta = (end - nearestFollower) * (movingTowardStart ? 1 : -1);
						DurationCapability toMove = (DurationCapability) (!movingTowardStart ? child : followingCapability);
						toMove.setStart(toMove.getStart() + delta);
						toMove.setEnd(toMove.getEnd() + delta);
						moved = true;
					}
				}
			}
		} while (moved); // Repeat until all decision gaps are closed
	}
	
	@Override
	protected void addDelegateComponentsCallback(
			Collection<AbstractComponent> childComponents) {
		// Enforce duration constraints when adding sub-activities
		super.addDelegateComponentsCallback(childComponents);
		constrainToDuration();
	}

	/**
	 * Utility method to determine if two DurationCapabilities overlap 
	 * @param a
	 * @param b
	 * @return
	 */
	private boolean overlaps(DurationCapability a, DurationCapability b) {
		return (a.getStart() < b.getEnd() && a.getEnd() > b.getStart());
	}
	
	/**
	 * Stub implementation of cost functions for activity components.
	 * In the future, this should be generalizable to include more than 
	 * just Comms and Power
	 * 
	 * @author vwoeltje
	 *
	 */
	private class CostFunctionStub implements CostFunctionCapability {
		private boolean isComm; //Otherwise, is power
		
		public CostFunctionStub(boolean isComm) {
			super();
			this.isComm = isComm;
		}

		@Override
		public String getName() {
			return isComm ? "Comms" : "Power";
		}

		@Override
		public String getUnits() {
			return isComm ? "KB/s" : "Watts";
		}

		@Override
		public double getValue(long time) {
			if (time < getStart() || time >= getEnd()) {
				return 0;
			} else {
				return isComm ? getData().getComm() : getData().getPower();
			}
		}

		@Override
		public Collection<Long> getChangeTimes() {
			return Arrays.asList(getStart(), getEnd());
		}
		
	}


}