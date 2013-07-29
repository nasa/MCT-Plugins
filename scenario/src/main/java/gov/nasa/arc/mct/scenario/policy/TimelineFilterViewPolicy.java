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
package gov.nasa.arc.mct.scenario.policy;

import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.policy.ExecutionResult;
import gov.nasa.arc.mct.policy.Policy;
import gov.nasa.arc.mct.policy.PolicyContext;
import gov.nasa.arc.mct.scenario.component.CostFunctionCapability;
import gov.nasa.arc.mct.scenario.component.DurationCapability;
import gov.nasa.arc.mct.scenario.component.TimelineComponent;
import gov.nasa.arc.mct.scenario.view.AbstractTimelineView;
import gov.nasa.arc.mct.scenario.view.GraphView;
import gov.nasa.arc.mct.scenario.view.TimelineView;
import gov.nasa.arc.mct.services.component.ViewInfo;

/**
 * Policy controlling where timeline views are available. Prevents these views from 
 * being made available for non-duration components
 * @author vwoeltje
 *
 */
public class TimelineFilterViewPolicy implements Policy  {

	@Override
	public ExecutionResult execute(PolicyContext context) {
		ExecutionResult trueResult = new ExecutionResult(context, true, "");

		AbstractComponent targetComponent = context.getProperty(PolicyContext.PropertyName.TARGET_COMPONENT.getName(), AbstractComponent.class);
		ViewInfo viewInfo = context.getProperty(PolicyContext.PropertyName.TARGET_VIEW_INFO.getName(), ViewInfo.class);

		// Any timeline-like view requires a DurationCapability
		if (AbstractTimelineView.class.isAssignableFrom(viewInfo.getViewClass())) {
			if (targetComponent.getCapability(DurationCapability.class) == null) {
				return new ExecutionResult(context, false, 
						viewInfo.getViewName() + " only valid for objects with durations.");
			}
		}
		
		// Graph views require cost functions
		if (GraphView.class.isAssignableFrom(viewInfo.getViewClass())) {
			if (targetComponent.getCapabilities(CostFunctionCapability.class).isEmpty()) {
				return new ExecutionResult(context, false, 
						viewInfo.getViewName() + " only valid for objects with cost functions.");
			}
		}
		
		// Timeline view is exclusively available to Timeline objects
		if (TimelineView.class.isAssignableFrom(viewInfo.getViewClass())) {
			if (!(targetComponent instanceof TimelineComponent)) {
				return new ExecutionResult(context, false, 
						viewInfo.getViewName() + " only valid for Timeline objects.");
			}
		}		
		
		return trueResult;
	}

}
