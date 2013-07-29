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

import gov.nasa.arc.mct.components.AbstractComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Abstract superclass for components which may aggregate the costs exposed 
 * by their children with those they exhibit themselves (such as ActivityComponent, 
 * which may have nested sub-activities with their own costs)
 * 
 * @author vwoeltje
 *
 */
public abstract class CostFunctionComponent extends AbstractComponent {
	// getCapabilities(CostFunctionCapability.class) invokes the same on children,
	// which may result in an infinite loop in the presence of a cycle. This 
	// ignoreList is used to bail out of cycles.
	private static ThreadLocal<HashSet<String>> ignoreList = new ThreadLocal<HashSet<String>>() {
		@Override
		protected HashSet<String> initialValue() {
			return new HashSet<String>();
		}		
	};
	
	/**
	 * Get any cost functions associated with this specific component. Users 
	 * of this component should generally use getCapabilities(CostFunctionCapability.class), 
	 * which will include the cost functions of children as well.
	 * @return a list of cost functions specific to this component
	 */
	public List<CostFunctionCapability> getInternalCostFunctions() {
		return Collections.emptyList();
	}
	
	@Override
	protected <T> List<T> handleGetCapabilities(Class<T> capability) {
		if (capability.isAssignableFrom(CostFunctionCapability.class)) {
			
			ignoreList.get().add(getComponentId()); // Track components visited to avoid cyclic cost functions
			
			Map<String, AggregateCostFunction> costFunctions = new HashMap<String, AggregateCostFunction>();
			for (CostFunctionCapability costFunction : getInternalCostFunctions()) {
				if (!costFunctions.containsKey(costFunction.getName())) {
					costFunctions.put(costFunction.getName(), 
							new AggregateCostFunction(costFunction.getName(), costFunction.getUnits()));
				}
				costFunctions.get(costFunction.getName()).add(costFunction);
			}
			for (AbstractComponent child : getComponents()) {
				if (!ignoreList.get().contains(child.getComponentId())) { // Don't continue down a cycle
					for (CostFunctionCapability costFunction : child.getCapabilities(CostFunctionCapability.class)) {
						if (!costFunctions.containsKey(costFunction.getName())) {
							costFunctions.put(costFunction.getName(), 
									new AggregateCostFunction(costFunction.getName(), costFunction.getUnits()));
						}
						costFunctions.get(costFunction.getName()).add(costFunction);
					}
				}
			}
			
			// Assemble into a nice list
			List<T> aggregateCostFunctions = new ArrayList<T>();
			for (AggregateCostFunction aggregateCostFunction : costFunctions.values()) {
				aggregateCostFunctions.add(capability.cast(aggregateCostFunction));
			}
			
			ignoreList.get().remove(getComponentId());
			
			return aggregateCostFunctions;
		}
		return super.handleGetCapabilities(capability);
	}

	/**
	 * A cost function which acts as a sum or union of other cost functions
	 * which share the same name. This is used to aggregate costs exposed by 
	 * children of a component.
	 * 
	 * @author vwoeltje
	 */
	private class AggregateCostFunction implements CostFunctionCapability {
		private String name;
		private String units;
		private List<CostFunctionCapability> costs = new ArrayList<CostFunctionCapability>();
		
		public AggregateCostFunction(String name, String units) {
			super();
			this.name = name;
			this.units = units;
		}

		void add(CostFunctionCapability cost) {
			costs.add(cost);
		}
		
		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getUnits() {
			return units;
		}

		@Override
		public double getValue(long time) {
			double sum = 0;
			for (CostFunctionCapability c : costs) {
				sum += c.getValue(time);
			}
			return sum;
		}

		@Override
		public Collection<Long> getChangeTimes() {
			Collection<Long> changeTimes = new TreeSet<Long>();
			for (CostFunctionCapability c : costs) {
				changeTimes.addAll(c.getChangeTimes());
			}
			return changeTimes;
		}
		
	}
}
