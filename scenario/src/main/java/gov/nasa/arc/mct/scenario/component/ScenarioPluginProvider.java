package gov.nasa.arc.mct.scenario.component;

import gov.nasa.arc.mct.policy.PolicyInfo;
import gov.nasa.arc.mct.scenario.policy.TimelineFilterViewPolicy;
import gov.nasa.arc.mct.scenario.view.ScenarioView;
import gov.nasa.arc.mct.scenario.view.TimelineInspector;
import gov.nasa.arc.mct.scenario.view.TimelineView;
import gov.nasa.arc.mct.services.component.AbstractComponentProvider;
import gov.nasa.arc.mct.services.component.ComponentTypeInfo;
import gov.nasa.arc.mct.services.component.ViewInfo;
import gov.nasa.arc.mct.services.component.ViewType;

import java.util.Arrays;
import java.util.Collection;
import java.util.ResourceBundle;

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
			new ActivityCreationWizardUI());

	private static final ComponentTypeInfo decisionComponentType = new ComponentTypeInfo(
			bundle.getString("display_name_decision"),  
			bundle.getString("description_decision"), 
			DecisionComponent.class,
			new DecisionCreationWizardUI());

	private static final ComponentTypeInfo timelineComponentType = new ComponentTypeInfo(
			bundle.getString("display_name_timeline"),  
			bundle.getString("description_timeline"), 
			TimelineComponent.class);

	// TODO: Expose this & scenario view to MCT
	private static final ComponentTypeInfo scenarioComponentType = new ComponentTypeInfo(
			bundle.getString("display_name_scenario"),  
			bundle.getString("description_scenario"), 
			ScenarioComponent.class);
	
	private static final PolicyInfo timelinePolicy = new PolicyInfo(
			PolicyInfo.CategoryType.FILTER_VIEW_ROLE.getKey(), 
			TimelineFilterViewPolicy.class);
	
	@Override
	public Collection<ComponentTypeInfo> getComponentTypes() {
		// return the component types provided
		return Arrays.asList(activityComponentType, timelineComponentType, decisionComponentType , scenarioComponentType   );
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
				timelinePolicy
				);
	}
	
}
