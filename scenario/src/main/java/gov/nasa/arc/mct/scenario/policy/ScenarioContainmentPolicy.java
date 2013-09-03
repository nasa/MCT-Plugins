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
				   child instanceof DecisionComponent;
		}
		
		if (child instanceof DecisionComponent) {
			return parent instanceof ActivityComponent;
		}
		
		return true;
	}
	
}
