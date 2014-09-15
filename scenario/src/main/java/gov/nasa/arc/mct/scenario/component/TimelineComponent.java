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
import gov.nasa.arc.mct.components.JAXBModelStatePersistence;
import gov.nasa.arc.mct.components.ModelStatePersistence;
import gov.nasa.arc.mct.components.ObjectManager;
import gov.nasa.arc.mct.components.PropertyDescriptor;
import gov.nasa.arc.mct.components.PropertyEditor;
import gov.nasa.arc.mct.components.PropertyDescriptor.VisualControlDescriptor;
import gov.nasa.arc.mct.scenario.util.Battery;
import gov.nasa.arc.mct.scenario.util.BatteryVoltageTable;
import gov.nasa.arc.mct.scenario.util.CostType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A Timeline serves as a container for activities. Multiple timelines may be arranged 
 * within a Scenario.
 * 
 *
 */
public class TimelineComponent extends CostFunctionComponent implements DurationCapability {
	private ObjectManager objectManager = new ObjectManager.ExplicitObjectManager();
    private final AtomicReference<BatteryModel> model = new AtomicReference<BatteryModel>(new BatteryModel());
	
	public String getDisplay(){
		return this.getDisplayName();
	}

	public BatteryModel getModel() {
		return model.get();
	}
	
	public void setBatteryModel(double capacity, double stateOfCharge) {
		model.set(new BatteryModel(capacity, stateOfCharge));
	}
	
	private boolean isLegacy() {
		return getModel().isUninitialized();
	}
	
	@Override
	protected <T> T handleGetCapability(Class<T> capability) {
		if (capability.isAssignableFrom(getClass())) {
			return capability.cast(this);
		} else if (capability.isAssignableFrom(objectManager.getClass())) {
			return capability.cast(objectManager);
		} else if (capability.isAssignableFrom(ScenarioCSVExportCapability.class)) {
			return capability.cast(new ScenarioCSVExportCapability(this));
		} else if (capability.isAssignableFrom(GraphViewCapability.class)) {
			if (isLegacy() || !BatteryVoltageTable.isBatteryFileExists()) return capability.cast(new LegacyGraphData(this));
			else return capability.cast(new TimelineGraphData());
		} else if (capability.isAssignableFrom(ModelStatePersistence.class)) {
		    JAXBModelStatePersistence<BatteryModel> persistence = new JAXBModelStatePersistence<BatteryModel>() {

				@Override
				protected BatteryModel getStateToPersist() {
					return model.get();
				}

				@Override
				protected void setPersistentState(BatteryModel modelState) {
					model.set(modelState);
				}

				@Override
				protected Class<BatteryModel> getJAXBClass() {
					return BatteryModel.class;
				}
		        
			};
			
			return capability.cast(persistence);
		}
		return super.handleGetCapability(capability);
	}
	
	@Override
	public List<PropertyDescriptor> getFieldDescriptors()  {
		// Provide an ordered list of fields to be included in the MCT Platform's InfoView.
		List<PropertyDescriptor> fields = new ArrayList<PropertyDescriptor>();

		// We specify a mutable text field.  The control display's values are maintained in the business model
		// via the PropertyEditor object.  When a new value is to be set, the editor also validates the prospective value.
		PropertyDescriptor batteryState = new PropertyDescriptor("Battery State of Charge (%)", 
				new BatteryStatePropertyEditor(), 
				VisualControlDescriptor.TextField);
		batteryState.setFieldMutable(true);
		fields.add(batteryState);
		
		PropertyDescriptor batteryCapacity = new PropertyDescriptor("Battery Total Capacity", 
				new BatteryCapacityPropertyEditor(), 
				VisualControlDescriptor.TextField);
		batteryCapacity.setFieldMutable(true);
		fields.add(batteryCapacity);

		return fields;		
	} 
	
	@Override
	public long getStart() {		
		// Time is measured as "milliseconds since start of timeline", so this is always 0
		return 0l;
	}
	@Override
	public long getEnd() {
		// The end of a Timeline is the end of its last activity
		long end = 0;
		Set<String> ignore = new HashSet<String>();
		for (AbstractComponent child : getComponents()) {
			end = Math.max(end, getEndFor(child, ignore));
		}
		return end;
	}
	
	// Recurse into collections to find possible end points
	// until base case of a component with a duration or a 
	// component with no children is reached.
	// Maintain list of things to ignore, to avoid cycles
	private long getEndFor(AbstractComponent child, Set<String> ignore) {
		ignore.add(child.getComponentId());
		DurationCapability dc = child.getCapability(DurationCapability.class);
		if (dc != null) {
			return dc.getEnd();
		} else {
			long end = 0;
			for (AbstractComponent grandchild : child.getComponents()) {
				if (!ignore.contains(grandchild.getComponentId())) {
					end = Math.max(end, getEndFor(grandchild, ignore));
				}
			}
			return end;
		}
	}
	
	@Override
	public void setStart(long start) {
		// TODO Auto-generated method stub		
	}
	@Override
	public void setEnd(long end) {
		// TODO Auto-generated method stub		
	}
	
	private class BatteryStatePropertyEditor implements PropertyEditor<Object> {

		@Override
		public String getAsText() {
			return String.valueOf(getModel().getInitialStateOfCharge());
		}

		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			String result = verify(text);
			if (result != null) {
				throw new IllegalArgumentException(result);
			}
			// verify() took care of a possible number format exception
			double state = Double.parseDouble(text);
			getModel().setInitialStateOfCharge(state);
		}
		
		private String verify(String s) {
			String message = null;
			assert s != null;
			if (s.isEmpty()) {
				message =  "Cannot be unspecified";
			} 
			try {
				double state = Double.parseDouble(s);
				if ((state < 0.0) || (state > 100.0)) {
					message = "Must be within 0.0 ~ 100.0";
				}
			} catch (NumberFormatException e) {
				message =  "Must be a numeric";
			}
			return message;
		}

		@Override
		public Object getValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setValue(Object value) throws IllegalArgumentException {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Object> getTags() {
			return Collections.emptyList();
		}
	}
	
	private class BatteryCapacityPropertyEditor implements PropertyEditor<Object> {

		@Override
		public String getAsText() {
			return String.valueOf(getModel().getBatteryCapacity());
		}

		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			String result = verify(text);
			if (result != null) {
				throw new IllegalArgumentException(result);
			}
			// verify() took care of a possible number format exception
			double capacity = Double.parseDouble(text);
			getModel().setBatteryCapacity(capacity);
		}
		
		private String verify(String s) {
			String message = null;
			assert s != null;
			if (s.isEmpty()) {
				message =  "Cannot be unspecified";
			} 
			try {
				double capacity = Double.parseDouble(s);
				if (capacity < 0.0) {
					message = "Cannot be negative";
				}
			} catch (NumberFormatException e) {
				message =  "Must be a numeric";
			}
			return message;
		}

		@Override
		public Object getValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setValue(Object value) throws IllegalArgumentException {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Object> getTags() {
			return Collections.emptyList();
		}
	}
	
	/**
	 * graph data associated with TimelineComponent
	 * 
	 * @author jdong2
	 *
	 */
	private class TimelineGraphData implements GraphViewCapability {
		
		String POWER_INSTANTANEOUS_NAME = "Current";
		String POWER_ACCUMULATIVE_NAME = "Battery Capacity";
		String POWER_INSANTANEOUS_UNITS = "A";
		String POWER_ACCUMULATIVE_UNITS = "%";
		
		public static final long SECOND_TO_MILLIS = 1000l;
		public static final long MINUTE_TO_MILLIS = 60000l;
		private static final int TIME_INTERVAL = 5; // update battery state in every 5 minutes
		private final long TIME_SCALE = TIME_INTERVAL * MINUTE_TO_MILLIS;
		
		private List<CostFunctionCapability> costs;
		
		// use TreeMap to enable ordering of data
		private Map<Long, Double> currentMap = new TreeMap<Long, Double> ();
		private Map<Long, Double> capacityMap = new TreeMap<Long, Double> ();
		
		public TimelineGraphData() {
			costs = getCapabilities(CostFunctionCapability.class);
		}
		
		private CostFunctionCapability getCost(CostType type) {
			for (CostFunctionCapability cost: costs) {
				if (cost.getCostType().equals(type)) return cost;
			}
			return costs.get(0);
		}

		// break up the change times to every 5 minutes
		private Collection<Long> getChangeTimes(CostType type) {
			Collection<Long> changeTimes = new TreeSet<Long>(getCost(type).getChangeTimes());
			if (type.equals(CostType.POWER)) { // decrease time interval according to battery value precision (currently 5 mins)
				int size = changeTimes.size();
				long start, end = 0;
				Long[] timeType = new Long[] {};
				Long[] timeArray = changeTimes.toArray(timeType);
				for (int i = 0; i < size - 1; i++ ) {
					start = timeArray[i];
					end = timeArray[i + 1];
					for (int j = 0; j < (end - start) / TIME_SCALE; j++) {
						changeTimes.add(start + j * TIME_SCALE);
					}
				}
				changeTimes.add(end);
			}
			return changeTimes;
		}

		@Override
		public Map<Long, Double> getData(CostType type, boolean isInstantaneous) {
			if (type.equals(CostType.POWER)) return getPowerData(getCost(type), isInstantaneous);
			else if (type.equals(CostType.COMM)) return getCommData(getCost(type), isInstantaneous);
			return null;
		}
		
		private void initCurrentAndCapacity(CostFunctionCapability powerCost) {
			Battery battery = new Battery(getModel());
			double initialStateOfCharge = battery.getInitialStateOfCharge();
			double stateOfCharge = initialStateOfCharge;
		    double voltage, current = 0.0, power;		    
		    
		    Collection<Long> changeTimes = getChangeTimes(CostType.POWER);
		    Long previousTime = -1l;
				
		    // init the starting point of graph		    
	    	capacityMap.put(getStart(), initialStateOfCharge);
		    currentMap.put(getStart(), 0.0);
		    
		    
			for (Long t: changeTimes) {						
				power = powerCost.getValue(t);
				
				// battery stateOfCharge changes when the battery is not full, or when the battery is full and there is consumption
				if ((battery.getStateOfCharge() < 100.0) || ((battery.getStateOfCharge() >= 100.0) && (power > 0.0))) {
					if (!capacityMap.containsKey(previousTime)) { // for the first timeStamp
						capacityMap.put(t, stateOfCharge);
					} else {
						if (stateOfCharge != capacityMap.get(previousTime)) { // avoid saving when capacity is the same
							capacityMap.put(t, stateOfCharge);
						}
					}	
					
					voltage = battery.getVoltage();	
					current = BatteryVoltageTable.getNearestState(power / voltage); // use formula A = P / V, in 0.5 precision
					
					if (!currentMap.containsKey(previousTime)) { // for the first timestamp
						currentMap.put(t, current);
					} else {
						if (current != currentMap.get(previousTime)) { // avoid saving when current is the same
							currentMap.put(t, current);
						}
					}					
					stateOfCharge = battery.setStateOfCharge(power, (double)TIME_INTERVAL); // use 5 mins as interval to update battery stateOfCharge	
				} else {
					currentMap.put(t, 0.0);
				}	
				previousTime = t;
			}	
			
			// init the end point of graph
			capacityMap.put(getEnd(), stateOfCharge);			
			currentMap.put(getEnd(), 0.0);		
		}
		
		private Map<Long, Double> getPowerData(CostFunctionCapability powerCost, boolean isInstantaneous) {
			currentMap.clear();
			capacityMap.clear();
			initCurrentAndCapacity(powerCost);
			return isInstantaneous? currentMap : capacityMap;
		}
		
		private Map<Long, Double> getCommData(CostFunctionCapability commCost, boolean isInstantaneous) {
			Collection<Long> changeTimes = getChangeTimes(CostType.COMM);
			Map<Long, Double> data = new TreeMap<Long, Double> ();
			if (isInstantaneous) {
				for (Long t : changeTimes) {
					data.put(t, commCost.getValue(t));
				}
			} else {
				Long[] timeType = new Long[] {};
				Long[] time = changeTimes.toArray(timeType);
				int size = changeTimes.size();
				double currentValue = 0;
				for (int i = 0; i < size; i++) {
					long t = time[i];
					data.put(t, currentValue);
					double commValue = commCost.getValue(t);
					double increase = ((i < size - 1) ? commValue * (time[i + 1] - t) / SECOND_TO_MILLIS : 0.0);
					currentValue += increase;
					if (currentValue < 0) currentValue = 0;
				}
				
				// init starting and ending point
				if (getStart() != time[0]) data.put(getStart(), 0.0);
				if (getEnd() != time[changeTimes.size() - 1]) data.put(getEnd(), currentValue);
			}
			return data;
		}

		@Override
		public String getUnits(CostType type, boolean isInstantaneous) {
			if (type.equals(CostType.POWER)) {
				return isInstantaneous? POWER_INSANTANEOUS_UNITS : POWER_ACCUMULATIVE_UNITS;
			} else if (type.equals(CostType.COMM)) {
				return isInstantaneous? type.getInstantaniousUnits() : type.getAccumulativeUnits();
			}
			return "";
		}

		@Override
		public String getDisplayName(CostType type, boolean isInstantaneous) {
			if (type.equals(CostType.POWER)) {
				return isInstantaneous? POWER_INSTANTANEOUS_NAME : POWER_ACCUMULATIVE_NAME;
			} else if (type.equals(CostType.COMM)) {
				return isInstantaneous? type.getName() : type.getAccumulativeName();
			}
			return "";
		}

		@Override
		public boolean hasInstantaneous(CostType type) {
			return true;
		}

		@Override
		public boolean hasAccumulative(CostType type) {
			return true;
		}

		@Override
		public boolean hasInstantaneousGraph() {
			return true;
		}

		@Override
		public boolean hasAccumulativeGraph() {
			return true;
		}
		
	}
}