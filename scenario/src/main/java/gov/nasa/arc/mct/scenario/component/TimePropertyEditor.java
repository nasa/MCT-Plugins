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

import gov.nasa.arc.mct.components.PropertyEditor;
import gov.nasa.arc.mct.scenario.util.DurationFormatter;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;

/**
 * Serves as a property editor for start times, end times, durations, 
 * as exposed by activities and decisions.
 *   
 * @author vwoeltje
 */
public class TimePropertyEditor implements PropertyEditor<Object> {
	private DurationCapability durationCapability;
	private TimeProperty property;

	public TimePropertyEditor(DurationCapability durationCapability, TimeProperty property) {
		super();
		this.durationCapability = durationCapability;
		this.property = property;
	}

	@Override
	public String getAsText() {
		return DurationFormatter.formatDuration(getTime());
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		try {
			setTime(DurationFormatter.parse(text));
		} catch (ParseException e) {
			throw new IllegalArgumentException("Invalid format for time", e);
		}
	}

	@Override
	public Object getValue() {
		return getAsText();
	}

	@Override
	public void setValue(Object value) throws IllegalArgumentException {
		setAsText(value == null ? "null" : value.toString());
	}

	@Override
	public List<Object> getTags() {
		return Collections.emptyList();
	}
	
	private long getTime() {
		return property.getTime(durationCapability);
	}
	
	private void setTime(long time) {
		property.setTime(durationCapability, time);
	}
	
	/**
	 * Describes various time elements that can be gotten/set
	 */
	public enum TimeProperty {
		START, END, DURATION;
		
		private long getTime(DurationCapability dc) {
			switch (this) {
			case START:
				return dc.getStart();
			case END:
				return dc.getEnd();
			case DURATION:
				return dc.getEnd() - dc.getStart();
			}
			return 0;
		}
		
		private void setTime(DurationCapability dc, long time) throws IllegalArgumentException {
			switch (this) {
			case START:
				if (time > dc.getEnd() || time < 0) {
					throw new IllegalArgumentException("Invalid start time.");
				}
				dc.setStart(time);
				break;
			case END:
				if (time < dc.getStart() || time < 0) {
					throw new IllegalArgumentException("Invalid end time.");
				}
				dc.setEnd(time);
				break;
			case DURATION:
				if (time < 0) {
					throw new IllegalArgumentException("Invalid duration.");
				}
				dc.setEnd(dc.getStart() + time);
				break;				
			}
		}
	}
}
