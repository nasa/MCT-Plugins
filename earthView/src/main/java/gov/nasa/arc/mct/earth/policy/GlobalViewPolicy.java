package gov.nasa.arc.mct.earth.policy;

import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.components.FeedProvider;
import gov.nasa.arc.mct.earth.view.EarthView;
import gov.nasa.arc.mct.policy.ExecutionResult;
import gov.nasa.arc.mct.policy.Policy;
import gov.nasa.arc.mct.policy.PolicyContext;
import gov.nasa.arc.mct.services.component.ViewInfo;

public class GlobalViewPolicy implements Policy {

	private boolean hasFeed(AbstractComponent component) {
		return component.getCapability(FeedProvider.class)  != null;
	}
	
	private boolean hasGrandchildFeeds(AbstractComponent component, int count) {
		int c = 0;
		
		// Suppress for non-orbital components. This should be removed or modified if 
		// constructing from collections generally
		/*
		if (!UserOrbitalComponent.class.isAssignableFrom(component.getClass())) {
			return false;
		}
		*/
		
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
	
	private boolean canView(AbstractComponent component) {
		if (hasGrandchildFeeds(component, 3)) return true;
		for (AbstractComponent child : component.getComponents()) {
			if (hasGrandchildFeeds(child, 3)) return true;
		}
		return false;
	}

	@Override
	public ExecutionResult execute(PolicyContext context) {
		boolean result = true;
		ViewInfo viewInfo = context.getProperty(PolicyContext.PropertyName.TARGET_VIEW_INFO.getName(), ViewInfo.class);

		if (EarthView.class.isAssignableFrom(viewInfo.getViewClass())) {
			result = canView(context.getProperty(PolicyContext.PropertyName.TARGET_COMPONENT.getName(), AbstractComponent.class));
		}

		return new ExecutionResult(context, result, null);
	}

}
