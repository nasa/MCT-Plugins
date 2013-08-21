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
		
		private void setTime(DurationCapability dc, long time) {
			switch (this) {
			case START:
				dc.setStart(time);
				break;
			case END:
				dc.setEnd(time);
				break;
			case DURATION:
				dc.setEnd(dc.getStart() + time);
				break;				
			}
		}
	}
}
