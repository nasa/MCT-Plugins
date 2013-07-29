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

import java.util.Collection;

/**
 * Represents some cost function associated with a component. For instance, an 
 * Activity may have Comms or Power costs associated with it which should be 
 * tracked and presented along with timelines. 
 * 
 * This should be exposed via the getCapabilities (plural) method of 
 * AbstractComponent. Note that such a component may be configured to have 
 * multiple costs (Comms and Power, for instance), in which case it should 
 * return one instance of CostFunctionCapability for each cost. 
 * 
 * @author vwoeltje
 *
 */
public interface CostFunctionCapability {
	/**
	 * Get the name of this cost.
	 * @return the name of this cost
	 */
	public String getName();
	
	/**
	 * Get the units used for this cost (as a string)
	 * @return the units used by this cost
	 */
	public String getUnits();
	
	/**
	 * Assess this cost at the specified time. 
	 * @param time the time, in milliseconds since start of timeline
	 * @return the cost at the specified time
	 */
	public double getValue(long time);
	
	/**
	 * Get the times at which this cost function changes. Since costs 
	 * are handled step-wise, having a list of points at which costs 
	 * change makes it simple to draw/label these transitions.
	 * Times returned here are in milliseconds after the start 
	 * of timeline.
	 * @return a collection of times at which costs change
	 */
	public Collection<Long> getChangeTimes();
}
