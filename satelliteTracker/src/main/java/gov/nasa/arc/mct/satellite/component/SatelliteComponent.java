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
package gov.nasa.arc.mct.satellite.component;

import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.components.JAXBModelStatePersistence;
import gov.nasa.arc.mct.components.ModelStatePersistence;

import java.util.concurrent.atomic.AtomicReference;

import jsattrak.utilities.TLE;

/*
 * This class (used in createComp in SatelliteWizard) is for housing a SatelliteModel.  Currently, the
 * satelliteTracker uses the CoordinateComponents to provide the data-feeds.  So, this class in the future
 * should be removed.
 */
public class SatelliteComponent extends AbstractComponent {
	private AtomicReference<SatelliteModel> model = new AtomicReference<SatelliteModel>();
	
	public void setOrbitalParameters(TLE curTLE ) {
		SatelliteModel m = new SatelliteModel();
		m.set(curTLE);
		model.set(m); //this line will stay the same
	}
	
	public SatelliteModel getModel() {
		return model.get();
	}
	
	@Override
	protected <T> T handleGetCapability(Class<T> capability) {
		if (ModelStatePersistence.class.isAssignableFrom(capability)) {
		    JAXBModelStatePersistence<SatelliteModel> persistence = new JAXBModelStatePersistence<SatelliteModel>() {

				@Override
				protected SatelliteModel getStateToPersist() {
					return model.get();
				}

				@Override
				protected void setPersistentState(SatelliteModel modelState) {
					model.set(modelState);
				}

				@Override
				protected Class<SatelliteModel> getJAXBClass() {
					return SatelliteModel.class;
				}
		        
			};
			
			return capability.cast(persistence);
		}
		
		return null;
	}

}