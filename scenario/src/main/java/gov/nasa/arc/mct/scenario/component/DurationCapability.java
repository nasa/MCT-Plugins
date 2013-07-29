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


/**
 * Represents some duration associated with a component. Components with 
 * durations have start and end times, relative to some starting point 
 * (the starting point is intentionally unspecified - generally speaking, 
 * these are taken as time after the start of some timeline.) 
 * 
 * This should be exposed via the getCapability method of a component.
 * 
 * @author vwoeltje
 *
 */
public interface DurationCapability {
	/**
	 * Get the start time of the component, measured in milliseconds 
	 * since the start of the timeline.
	 * @return the start time of this component
	 */
	public long getStart();
	
	/**
	 * Get the end time of the component, measured in milliseconds 
	 * since the start of the timeline.
	 * @return the end time of this component
	 */
	public long getEnd();
	

	/**
	 * Set the start time of the component, measured in milliseconds 
	 * since the start of the timeline.
	 * @param start the new start time for this component
	 */
	public void setStart(long start);
	
	/**
	 * Set the end time of the component, measured in milliseconds 
	 * since the start of the timeline.
	 * @param end the new end time for this component
	 */
	public void setEnd(long end);
}
