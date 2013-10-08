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
import gov.nasa.arc.mct.components.JAXBModelStatePersistence;
import gov.nasa.arc.mct.components.ModelStatePersistence;
import gov.nasa.arc.mct.components.ObjectManager;
import gov.nasa.arc.mct.components.PropertyDescriptor;
import gov.nasa.arc.mct.components.PropertyDescriptor.VisualControlDescriptor;
import gov.nasa.arc.mct.scenario.component.TimePropertyEditor.TimeProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
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
	private ObjectManager objectManager = new ObjectManager.ExplicitObjectManager();
	private final AtomicReference<ActivityModelRole> model = new AtomicReference<ActivityModelRole>(new ActivityModelRole());
	
	/**
	 * Get the underlying data about this Activity (start time, end time, costs, type...)
	 * @return underlying activity data
	 */
	public ActivityData getData() {
		return getModel().getData();
	}

	@Override
	protected <T> List<T> handleGetCapabilities(Class<T> capability) {
		if (capability.isAssignableFrom(TagCapability.class)) {
			List<T> tagCapabilities = null;
			for (AbstractComponent child : getComponents()) {
				if (!(child instanceof ActivityComponent)) {
					List<T> childTags = child.getCapabilities(capability);
					if (childTags != null && !childTags.isEmpty()) {
						if (tagCapabilities == null) {
							tagCapabilities = new ArrayList<T>();
						}
						tagCapabilities.addAll(childTags);
					}
				}
			}
			if (tagCapabilities != null) {
				return tagCapabilities;
			}
		}
		return super.handleGetCapabilities(capability);
	}

	@Override
	protected <T> T handleGetCapability(Class<T> capability) {
		// Note: Don't report self as capability until initialized.
		if (capability.isAssignableFrom(getClass())) {
			return capability.cast(this);
		}
		if (capability.isAssignableFrom(ObjectManager.class)) {
			return capability.cast(objectManager);
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
		PropertyDescriptor tags = new PropertyDescriptor("Tags", 
				new TagPropertyEditor(this), VisualControlDescriptor.Custom);
		tags.setFieldMutable(true);

		fields.add(type);
		fields.add(startTime);
		fields.add(endTime);
		fields.add(duration);
		fields.add(power);
		fields.add(comm);
		fields.add(notes);
		fields.add(tags);

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
	}

	@Override
	public void setStart(long start) {

		getData().setStartDate(new Date(start > 0 ? start : 0));
	}

	@Override
	public void setEnd(long end) {
		getData().setEndDate(new Date(end > getStart() ? end : getStart()));
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