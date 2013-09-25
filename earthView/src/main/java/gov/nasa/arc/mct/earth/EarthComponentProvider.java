package gov.nasa.arc.mct.earth;

import gov.nasa.arc.mct.earth.policy.GlobalViewPolicy;
import gov.nasa.arc.mct.earth.view.EarthView;
import gov.nasa.arc.mct.policy.PolicyInfo;
import gov.nasa.arc.mct.services.component.AbstractComponentProvider;
import gov.nasa.arc.mct.services.component.ViewInfo;
import gov.nasa.arc.mct.services.component.ViewType;

import java.util.Arrays;
import java.util.Collection;

public class EarthComponentProvider extends AbstractComponentProvider {
	private static final Collection<ViewInfo> VIEWS = Arrays.asList(
			new ViewInfo(EarthView.class, "Global", ViewType.OBJECT),
			new ViewInfo(EarthView.class, "Global", ViewType.EMBEDDED)
	);
	
	private static final Collection<PolicyInfo> POLICIES = Arrays.asList(
			new PolicyInfo(PolicyInfo.CategoryType.FILTER_VIEW_ROLE.getKey(), GlobalViewPolicy.class)
	);
	
	@Override
	public Collection<PolicyInfo> getPolicyInfos() {
        return POLICIES;
	}
	
	@Override
	public Collection<ViewInfo> getViews(String componentTypeId) {
		
		return VIEWS;
	}
}
