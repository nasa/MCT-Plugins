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
import gov.nasa.arc.mct.components.FeedProvider;
import gov.nasa.arc.mct.policy.ExecutionResult;
import gov.nasa.arc.mct.policy.Policy;
import gov.nasa.arc.mct.policy.PolicyContext;
import gov.nasa.arc.mct.scenario.component.ActivityComponent;
import gov.nasa.arc.mct.scenario.component.DecisionComponent;
import gov.nasa.arc.mct.scenario.component.DurationCapability;
import gov.nasa.arc.mct.scenario.component.ScenarioComponent;
import gov.nasa.arc.mct.scenario.component.TagComponent;
import gov.nasa.arc.mct.scenario.component.TagRepositoryComponent;
import gov.nasa.arc.mct.scenario.component.TimelineComponent;

import java.util.Collection;

public class ScenarioContainmentPolicy implements Policy {

	@Override
	public ExecutionResult execute(PolicyContext context) {
		AbstractComponent parentComponent = 
				context.getProperty(PolicyContext.PropertyName.TARGET_COMPONENT.getName(), AbstractComponent.class);
		
		@SuppressWarnings("unchecked")
		Collection<AbstractComponent> childComponents = 
				context.getProperty(PolicyContext.PropertyName.SOURCE_COMPONENTS.getName(), Collection.class);
				
		boolean result = true;
		String resultText = "";
		
		if (parentComponent != null && childComponents != null) {
			for (AbstractComponent childComponent : childComponents) {
				if (!canContain(parentComponent, childComponent)) {
					result = false;
					resultText = parentComponent.getDisplayName() + " cannot contain " + 
					    childComponent.getDisplayName() + 
					    "; incompatible object types.";
					break;
				}
			}
		}
		
		return new ExecutionResult(context, result, resultText);
	}

	private boolean canContain(AbstractComponent parent, AbstractComponent child) {
		if (parent instanceof TagRepositoryComponent) {
			return child instanceof TagComponent;
		}
		
		if (parent instanceof ScenarioComponent) {
			return child instanceof TimelineComponent ||
				   child instanceof ActivityComponent;
		}
		
		if (parent instanceof TimelineComponent) {
			return !(child instanceof DecisionComponent ||
					child instanceof TimelineComponent ||
					child instanceof ScenarioComponent) &&
				   (child.getCapability(DurationCapability.class) != null  || // Activities
				   child.getCapability(FeedProvider.class) != null ||         // Telemetry
				   child.getComponentTypeID().contains("Collection"));
		}
		
		if (parent instanceof ActivityComponent) {
			return child instanceof ActivityComponent ||
				   child instanceof DecisionComponent ||
				   child instanceof TagComponent;
		}
		
		if (child instanceof DecisionComponent) {
			return parent instanceof ActivityComponent;
		}
		
		if (child instanceof TagComponent) {
			return parent instanceof ActivityComponent;
		}
		
		return true;
	}
	
}
