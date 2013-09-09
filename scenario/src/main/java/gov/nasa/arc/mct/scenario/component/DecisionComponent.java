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
import gov.nasa.arc.mct.components.PropertyDescriptor;
import gov.nasa.arc.mct.components.PropertyDescriptor.VisualControlDescriptor;
import gov.nasa.arc.mct.scenario.component.TimePropertyEditor.TimeProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents an Decision user object.
 * 
 * Decisions have start and end times, which should fill the gap between two other 
 * Activity siblings. A Decision represents some external consideration/evaluation 
 * which must be performed within a certain time before a timeline proceeds.  
 *
 */
public class DecisionComponent extends AbstractComponent implements DurationCapability {

	private final AtomicReference<DecisionModelRole> model = new AtomicReference<DecisionModelRole>(new DecisionModelRole());
	
	/**
	 * Get underlying data about this decision (specifically,
	 * start and end times.)
	 * @return
	 */
	public DecisionData getData() {
		return getModel().getData();
	}
	
	@Override
	protected <T> T handleGetCapability(Class<T> capability) {
		// Note: Don't report self as capability until initialized.
		if (capability.isAssignableFrom(getClass()) && getData().getEndTime() != null) {
			return capability.cast(this);
		}
		if (capability.isAssignableFrom(ModelStatePersistence.class)) {
		    JAXBModelStatePersistence<DecisionModelRole> persistence = new JAXBModelStatePersistence<DecisionModelRole>() {

				@Override
				protected DecisionModelRole getStateToPersist() {
					return model.get();
				}

				@Override
				protected void setPersistentState(DecisionModelRole modelState) {
					model.set(modelState);
				}

				@Override
				protected Class<DecisionModelRole> getJAXBClass() {
					return DecisionModelRole.class;
				}
		        
			};
			
			return capability.cast(persistence);
		}
		return null;
	}
	
	/**
	 * Get a container for internal data about this decision
	 * (specifically, its start and end times.)
	 * @return contained data about this decision
	 */
	public DecisionModelRole getModel() {
		return model.get();
	}

	@Override
	public List<PropertyDescriptor> getFieldDescriptors()  {

		// Provide an ordered list of fields to be included in the MCT Platform's InfoView.
		List<PropertyDescriptor> fields = new ArrayList<PropertyDescriptor>();

 
		// We specify a mutable text field.  The control display's values are maintained in the business model
		// via the PropertyEditor object.  When a new value is to be set, the editor also validates the prospective value.
		PropertyDescriptor startTime = new PropertyDescriptor("Start Time", 
				new TimePropertyEditor(this, TimeProperty.START),  VisualControlDescriptor.TextField);
		startTime.setFieldMutable(true);
		PropertyDescriptor endTime = new PropertyDescriptor("End Time", 
				new TimePropertyEditor(this, TimeProperty.END),  VisualControlDescriptor.TextField);
		endTime.setFieldMutable(true);
		PropertyDescriptor duration = new PropertyDescriptor("Duration",
				new TimePropertyEditor(this, TimeProperty.DURATION), VisualControlDescriptor.TextField);
		duration.setFieldMutable(true);
		PropertyDescriptor notes = new PropertyDescriptor("Notes", 
				new NotesPropertyEditor(this),  VisualControlDescriptor.TextArea);
		notes.setFieldMutable(true);
		
		fields.add(startTime);
		fields.add(endTime);
		fields.add(duration);
		fields.add(notes);

		return fields;
	}

	@Override
	public long getStart() {
		return getData().getStartTime().getTime();
	}

	@Override
	public long getEnd() {
		return getData().getEndTime().getTime();
	}

	@Override
	public void setStart(long start) {
		getData().setStartDate(new Date(start));
	}

	@Override
	public void setEnd(long end) {
		getData().setEndDate(new Date(end));
	}

	@Override
	public boolean isLeaf() {
		return true; // Decisions should never have children in the user object graph
	}

}