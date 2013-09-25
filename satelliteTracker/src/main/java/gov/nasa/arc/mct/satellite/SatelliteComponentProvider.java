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
package gov.nasa.arc.mct.satellite;

import gov.nasa.arc.mct.policy.PolicyInfo;
import gov.nasa.arc.mct.satellite.component.CoordinateComponent;
import gov.nasa.arc.mct.satellite.component.SatelliteComponent;
import gov.nasa.arc.mct.satellite.component.VectorComponent;
import gov.nasa.arc.mct.satellite.policy.MercatorViewPolicy;
import gov.nasa.arc.mct.satellite.view.MercatorProjectionView;
import gov.nasa.arc.mct.satellite.wizard.SatelliteWizard;
import gov.nasa.arc.mct.services.component.AbstractComponentProvider;
import gov.nasa.arc.mct.services.component.ComponentTypeInfo;
import gov.nasa.arc.mct.services.component.CreateWizardUI;
import gov.nasa.arc.mct.services.component.TypeInfo;
import gov.nasa.arc.mct.services.component.ViewInfo;
import gov.nasa.arc.mct.services.component.ViewType;

import java.util.Arrays;
import java.util.Collection;

/*
 * This class tells MCT "Hey I'm a plug-in!". It tells MCT what views are associated with
 * this plug-in, as well as what components and policies are associated with this plug-in.
 * 
 * How does MCT know which class is doing what (e.g: MercatorProjectionView providing a view
 * based on feed-data)?  MCT knows the flow of the plug-in because of this (Satellite)
 * component provider.
 * 
 * But, how does MCT know to come here?  The services.xml in the folder OSGI-INF (located
 * at satelliteTracker/src/main/resources) tells MCT that SatelliteComponentProvider is
 * truly a MCT component provider.
 * 
 */
public class SatelliteComponentProvider extends AbstractComponentProvider {

	
	/*  Our components associated with this plug-in:
	 * 		-SatelliteComponent.class: stores data needed to track a satellite
	 *      -VectorComponent.class: this is used to make Position vectors, a Velocity vector (these are really just used as a holder
	 *       for the components of the vector--which contain the important data for calculating position)
	 *      -CoordinateComponent.class: these are the individual elements of the vectors; these contain the data that the vector components
	 *       represent
	 */
	private static final Collection<ComponentTypeInfo> COMPONENTS = Arrays.asList(
			new ComponentTypeInfo("Satellite",   "An object to track a satellite's movement. ",        SatelliteComponent.class, true),
			new ComponentTypeInfo("Vector",  "",                 VectorComponent.class, false),
			new ComponentTypeInfo("Coordinate", "",              CoordinateComponent.class, false)
	);
	
	/* Our Views associated with this plug-in: so MCT knows that the class 'MercatorProjectionView' is providing
	 * visualizations with respect to feed-data
	 *   
	 *  ViewType.OBJECT is for a view that can be displayed in the (MCT) Inspector View
	 *  ViewType.EMBEDDED is for a view that can be displayed in another view.  We have this so we can put MercatorProjections
	 *  within a (MCT) Canvas View
	 */
	private static final Collection<ViewInfo> VIEWS = Arrays.asList(
			new ViewInfo(MercatorProjectionView.class, "Mercator Projection", ViewType.OBJECT),
			new ViewInfo(MercatorProjectionView.class, "Mercator Projection", ViewType.EMBEDDED)
	);
	
	/* Our policies associated with this plug-in:  here we tell MCT that the class 'MercatorViewPolicy'
	 * is handling the policies for this plug-in.
	 * 
	 */
	private static final Collection<PolicyInfo> POLICIES = Arrays.asList(
			new PolicyInfo(PolicyInfo.CategoryType.FILTER_VIEW_ROLE.getKey(), MercatorViewPolicy.class)
	);
	
	
	/*
	 * Here is where MCT talks to our plug-in, and our plug-in tells MCT
	 * what components are associated with this plug-in.
	 */
	@Override
	public Collection<ComponentTypeInfo> getComponentTypes() {
		return COMPONENTS;
	}
		

	/*
	 * Here is where MCT talks to our plug-in: our plug-in tells MCT
	 * what views are associated with this plug-in.
	 */
	@Override
	public Collection<ViewInfo> getViews(String componentTypeId) {
		return VIEWS;
	}
	
	/*
	 * Here is where MCT learns of the policys associated with this plug-in
	 */
	@Override
	public Collection<PolicyInfo> getPolicyInfos() {
        return POLICIES;
	}
	
	/*
	 * Here is where MCT finds accessory objects for certain types. 
	 * In this instance, MCT finds the contents of the Create Wizard 
	 * for satellite components.
	 */
	@Override
	public <T> T getAsset(TypeInfo<?> info, Class<T> assetClass) {
		if (assetClass.isAssignableFrom(CreateWizardUI.class)) {
			if (info.getTypeClass().isAssignableFrom(SatelliteComponent.class)) {
				return assetClass.cast(new SatelliteWizard());
			}
		}
		return super.getAsset(info, assetClass);
	}
	

}
