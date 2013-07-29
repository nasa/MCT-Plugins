package gov.nasa.arc.mct.scenario.component;
import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.components.PropertyEditor;

import java.util.List;


/**
 * Property editor to support specification of an Activity's communications cost in the Info View.
 *
 */
public final class TypePropertyEditor implements PropertyEditor<Object> {

	ActivityComponent activityComponent = null;

	public TypePropertyEditor(AbstractComponent component) {
		activityComponent = (ActivityComponent)component;
	}

	@Override
	public String getAsText() {
		String typeData = activityComponent.getModel().getData().getActivityType();
		return typeData;
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
			businessModel.setActivityType(newValue);
			
	}

	@SuppressWarnings("unused")
	private String verify(String s) {
		assert s != null;
		if (s.isEmpty()) {
			return "Cannot be unspecified";
		}
		/*try { // add a test if only certain types / type format is permitted
			DurationFormatter.parse(s);
		} catch (ParseException e) {
			return "Duration incorrectly formatted. See Scenario Plug-in Documentation";
		}*/
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

