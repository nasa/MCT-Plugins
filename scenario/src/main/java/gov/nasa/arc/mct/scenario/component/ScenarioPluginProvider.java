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

import gov.nasa.arc.mct.policy.PolicyInfo;
import gov.nasa.arc.mct.scenario.policy.ScenarioContainmentPolicy;
import gov.nasa.arc.mct.scenario.policy.TimelineFilterViewPolicy;
import gov.nasa.arc.mct.scenario.view.ScenarioView;
import gov.nasa.arc.mct.scenario.view.TimelineInspector;
import gov.nasa.arc.mct.scenario.view.TimelineView;
import gov.nasa.arc.mct.services.component.AbstractComponentProvider;
import gov.nasa.arc.mct.services.component.ComponentTypeInfo;
import gov.nasa.arc.mct.services.component.CreateWizardUI;
import gov.nasa.arc.mct.services.component.TypeInfo;
import gov.nasa.arc.mct.services.component.ViewInfo;
import gov.nasa.arc.mct.services.component.ViewType;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;

/**
 * ComponentProvider for the scenario plug-in. Exposes Activities, Scenarios, Timelines, 
 * Decisions, and associated policies/views to the MCT platform.
 * 
 *
 */
public class ScenarioPluginProvider extends AbstractComponentProvider {

	// use a resource bundle for strings to enable localization in the future if required
	private static ResourceBundle bundle = ResourceBundle.getBundle("Bundle"); 
	
	private static final ComponentTypeInfo activityComponentType = new ComponentTypeInfo(
			bundle.getString("display_name_activity"),  
			bundle.getString("description_activity"), 
			ActivityComponent.class,
			true);

	private static final ComponentTypeInfo decisionComponentType = new ComponentTypeInfo(
			bundle.getString("display_name_decision"),  
			bundle.getString("description_decision"), 
			DecisionComponent.class,
			true);

	private static final ComponentTypeInfo timelineComponentType = new ComponentTypeInfo(
			bundle.getString("display_name_timeline"),  
			bundle.getString("description_timeline"), 
			TimelineComponent.class,
			true);

	private static final ComponentTypeInfo scenarioComponentType = new ComponentTypeInfo(
			bundle.getString("display_name_scenario"),  
			bundle.getString("description_scenario"), 
			ScenarioComponent.class,
			true);
	
	
	private static final PolicyInfo timelineViewPolicy = new PolicyInfo(
			PolicyInfo.CategoryType.FILTER_VIEW_ROLE.getKey(), 
			TimelineFilterViewPolicy.class);
	
	private static final PolicyInfo containmentPolicy = new PolicyInfo(
			PolicyInfo.CategoryType.COMPOSITION_POLICY_CATEGORY.getKey(), 
			ScenarioContainmentPolicy.class);
	
	
	private Map<Class<?>, ImageIcon> iconMap = new HashMap<Class<?>, ImageIcon>();
	
	public ScenarioPluginProvider() {
		iconMap.put(ActivityComponent.class, 
				new ImageIcon(ScenarioPluginProvider.class.getResource("/icons/mct_icon_activity.png")));
		iconMap.put(DecisionComponent.class, 
				new ImageIcon(ScenarioPluginProvider.class.getResource("/icons/mct_icon_decision.png")));
		iconMap.put(TimelineComponent.class, 
				new ImageIcon(ScenarioPluginProvider.class.getResource("/icons/mct_icon_timeline.png")));
		iconMap.put(ScenarioComponent.class, 
				new ImageIcon(ScenarioPluginProvider.class.getResource("/icons/mct_icon_scenario.png")));
	}
	
	@Override
	public Collection<ComponentTypeInfo> getComponentTypes() {
		// return the component types provided
		return Arrays.asList(activityComponentType, timelineComponentType, decisionComponentType , scenarioComponentType );
	}

	@Override
	public Collection<ViewInfo> getViews(String componentTypeId) {

		// return a view if desired for the components being created. Note that this method is called
		// for every component type so a view could be supplied for any component type not just
		// components supplied by this provider.
		

		
		// TimelineFilterViewPolicy will suppress these as appropriate
		return Arrays.asList(
			    	new ViewInfo(ScenarioView.class, "Scenario", ViewType.CENTER),
			    	new ViewInfo(ScenarioView.class, "Scenario", ViewType.OBJECT), 
				    new ViewInfo(TimelineView.class, "Timeline", ViewType.CENTER),
				    new ViewInfo(TimelineInspector.class, "Timeline Inspector", ViewType.CENTER_OWNED_INSPECTOR),
					new ViewInfo(TimelineView.class, "Timeline", ViewType.OBJECT));
	}

	@Override
	public Collection<PolicyInfo> getPolicyInfos() {
		return Arrays.asList(
				timelineViewPolicy, containmentPolicy
				);
	}

	@Override
	public <T> T getAsset(TypeInfo<?> type, Class<T> assetClass) {
		// Create wizards
		if (assetClass.isAssignableFrom(CreateWizardUI.class)) {
			if (ActivityComponent.class.isAssignableFrom(type.getTypeClass())) {
				return assetClass.cast(new ActivityCreationWizardUI());
			}
			if (DecisionComponent.class.isAssignableFrom(type.getTypeClass())) {
				return assetClass.cast(new DecisionCreationWizardUI());
			}
		}
		
		// Icons
		if (assetClass.isAssignableFrom(ImageIcon.class)) {
			Class<?> typeClass = type.getTypeClass();
			if (iconMap.containsKey(typeClass)) {
				return assetClass.cast(iconMap.get(typeClass));
			}
		}
	
		// Default behavior
		return super.getAsset(type, assetClass);
	}


	
}
