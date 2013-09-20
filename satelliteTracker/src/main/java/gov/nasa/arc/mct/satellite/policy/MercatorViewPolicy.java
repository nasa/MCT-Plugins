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
package gov.nasa.arc.mct.satellite.policy;

import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.components.FeedProvider;
import gov.nasa.arc.mct.satellite.view.MercatorProjectionView;
import gov.nasa.arc.mct.policy.ExecutionResult;
import gov.nasa.arc.mct.policy.Policy;
import gov.nasa.arc.mct.policy.PolicyContext;
import gov.nasa.arc.mct.services.component.ViewInfo;


/*
 * This class tells MCT what types of components that can be viewed on a Mercator projection.  At
 * the moment if the selected-object in MCT has at least three grandChildren  which are feeds, then
 * they may be viewed on a mercator plot.
 * 
 * SatelliteComponentProvider tells MCT that this is the class that is handling the policies for
 * the satelliteTracker.
 */
public class MercatorViewPolicy implements Policy {

	/*
	 * Tell MCT whether or not 
	 */
	@Override
	public ExecutionResult execute(PolicyContext context) {
		boolean result = true;
		ViewInfo viewInfo = context.getProperty(PolicyContext.PropertyName.TARGET_VIEW_INFO.getName(), ViewInfo.class);

		if (MercatorProjectionView.class.isAssignableFrom(viewInfo.getViewClass())) {
			result = canView(context.getProperty(PolicyContext.PropertyName.TARGET_COMPONENT.getName(), AbstractComponent.class));
		}

		return new ExecutionResult(context, result, null);
	}
	
	
	private boolean hasFeed(AbstractComponent component) {
		return component.getCapability(FeedProvider.class)  != null;
	}
	
	private boolean canView(AbstractComponent component) {
		if (hasGrandchildFeeds(component, 3)) return true;
		for (AbstractComponent child : component.getComponents()) {
			if (hasGrandchildFeeds(child, 3)) return true;
		}
		return false;
	}
	
	/**
	 * Determine whether a given component has a specified number of grandchildren
	 *   example:  ISS (ZARYA)
	 *               Position
	 *                    x
	 *                    y
	 *                    z
	 *               Velocity
	 *                    x
	 *                    y
	 *                    z
	 * @param component the object that will be checked for how many grandchildren-with-feeds it has
	 * @param count the minimum number of grandchildren feeds that this component should have  
	 * @return whether or not the given component has at least 'count' number of grandchildren 
	 */
	private boolean hasGrandchildFeeds(AbstractComponent component, int count) {
		int c = 0;	//number of grandchildren
		
		//in the example above (in the javadocs) the children are Position and Velocity and x y z are
		//grandchildren; return is true as position has three grandchildren (so velocity is not even
		//checked
		for (AbstractComponent child : component.getComponents()) {
			for (AbstractComponent grandchild : child.getComponents()) {
				if (hasFeed(grandchild)) {
					c++;
					if (c >= count) return true;
				}
			}
		}
		
		return false;
	}

}
