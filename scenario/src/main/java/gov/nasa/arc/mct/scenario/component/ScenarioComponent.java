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

import java.util.concurrent.atomic.AtomicReference;

public class ScenarioComponent extends CostFunctionComponent implements DurationCapability {
	private ObjectManager objectManager = new ObjectManager.ExplicitObjectManager();//.DirtyObjectManager(this);
	private final AtomicReference<ActivityModelRole> model = new AtomicReference<ActivityModelRole>(new ActivityModelRole());
	
	public ActivityData getData() {
		return getModel().getData();
	}
	
	public String getDisplay(){
		return this.getDisplayName();
	}
	
	@Override
	protected <T> T handleGetCapability(Class<T> capability) {
		if (capability.isAssignableFrom(getClass())) {
			return capability.cast(this);
		}
		if (capability.isAssignableFrom(ObjectManager.class)) {
			return capability.cast(objectManager);
		}
		if (capability.isAssignableFrom(ScenarioCSVExportCapability.class)) {
			return capability.cast(new ScenarioCSVExportCapability(this));
		}
		if (capability.isAssignableFrom(GraphViewCapability.class)) {
			return capability.cast(new LegacyGraphData(this));
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
	
	public ActivityModelRole getModel() {
		return model.get();
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