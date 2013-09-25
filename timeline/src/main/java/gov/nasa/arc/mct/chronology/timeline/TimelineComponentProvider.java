package gov.nasa.arc.mct.chronology.timeline;

import gov.nasa.arc.mct.chronology.timeline.component.TimelineComponent;
import gov.nasa.arc.mct.chronology.timeline.policy.TimelineFilterViewPolicy;
import gov.nasa.arc.mct.chronology.timeline.view.TimelineView;
import gov.nasa.arc.mct.policy.PolicyInfo;
import gov.nasa.arc.mct.services.component.AbstractComponentProvider;
import gov.nasa.arc.mct.services.component.ComponentTypeInfo;
import gov.nasa.arc.mct.services.component.ViewInfo;
import gov.nasa.arc.mct.services.component.ViewType;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Provides the Timeline View and related Timeline Component to MCT.
 * @author vwoeltje
 *
 */
public class TimelineComponentProvider extends AbstractComponentProvider {
	private static final Collection<ComponentTypeInfo> COMPONENT_TYPES = Arrays.asList(
			new ComponentTypeInfo("Timeline", "A container for events", TimelineComponent.class, true)
			); 

    private static final List<ViewInfo> VIEW_INFOS = Arrays.asList(
    		new ViewInfo(TimelineView.class, TimelineView.VIEW_ROLE_NAME, ViewType.OBJECT),
    		new ViewInfo(TimelineView.class, TimelineView.VIEW_ROLE_NAME, TimelineView.class.getName(), ViewType.EMBEDDED, true, TimelineComponent.class),
    		new ViewInfo(TimelineView.class, TimelineView.VIEW_ROLE_NAME, TimelineView.class.getName(), ViewType.CENTER, true, TimelineComponent.class)
    		);  
	
    private static final Collection<PolicyInfo> POLICY_INFOS = Arrays.asList(
            new PolicyInfo(PolicyInfo.CategoryType.FILTER_VIEW_ROLE.getKey(),
                    TimelineFilterViewPolicy.class)
    		);
    
	@Override
	public Collection<ComponentTypeInfo> getComponentTypes() {
		return COMPONENT_TYPES;
	}

	@Override
	public Collection<ViewInfo> getViews(String componentTypeId) {
		return VIEW_INFOS;
	}

	@Override
	public Collection<PolicyInfo> getPolicyInfos() {
		return POLICY_INFOS;
	}

	
}
