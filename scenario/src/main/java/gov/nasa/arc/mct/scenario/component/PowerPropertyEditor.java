package gov.nasa.arc.mct.scenario.component;

import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.components.PropertyEditor;

import java.util.List;


/**
 * Property editor to support specification of an Activity's power cost in the Info View.
 *
 */
public final class PowerPropertyEditor implements PropertyEditor<Object> {
	ActivityComponent activityComponent = null;

	public PowerPropertyEditor(AbstractComponent component) {
		activityComponent = (ActivityComponent)component;
	}

	@Override
	public String getAsText() {
		double numericData = activityComponent.getModel().getData().getPower();
		return String.valueOf(numericData);
	}

	/**
	 * Set and save the value in the business model.
	 * 
	 * @param newValue the new value
	 * @throws exception if the new value is invalid.  MCT platform will handle this exception and
	 * disallow the prospective edit.
	 */
	@Override
	public void setAsText(String newValue) throws IllegalArgumentException {
		String result = verify(newValue);
		if (verify(newValue) != null) {
			throw new IllegalArgumentException(result);
		}
		ActivityData businessModel = activityComponent.getModel().getData();
		double d = Double.parseDouble(newValue); // verify() took care of a possible number format exception
		businessModel.setPower(d);
	}

	private String verify(String s) {
		assert s != null;
		if (s.isEmpty()) {
			return "Cannot be unspecified";
		}
		try {
			Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return "Must be a numeric";
		}
		return null;
	}

	@Override
	public String getValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setValue(Object selection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Object> getTags() {
		throw new UnsupportedOperationException();
	}
} 
