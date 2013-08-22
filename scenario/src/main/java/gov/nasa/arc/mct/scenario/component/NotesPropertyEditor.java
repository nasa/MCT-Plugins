package gov.nasa.arc.mct.scenario.component;
import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.components.PropertyEditor;

import java.util.List;


/**
 * Property editor to add notes to either an activity or a decision
 *
 */
public final class NotesPropertyEditor implements PropertyEditor<Object> {

	private AbstractComponent component = null;

	public NotesPropertyEditor(AbstractComponent component) {
		this.component = component;
		if (!(component instanceof ActivityComponent) && !(component instanceof DecisionComponent)) {
			throw new IllegalArgumentException("NotesPropertyEditor only valid for activities and decisions");
		}
	}

	
	@Override
	public String getAsText() {
		if (component instanceof ActivityComponent) {
			return ((ActivityComponent) component).getModel().getData().getNotes();
		} else if (component instanceof DecisionComponent) {
			return ((DecisionComponent) component).getModel().getData().getNotes();
		} else {
			return "";
		}
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
		if (newValue == null) {
			throw new IllegalArgumentException("Cannot be null");
		}
		if (component instanceof ActivityComponent) {
			((ActivityComponent) component).getModel().getData().setNotes(newValue);
		} else if (component instanceof DecisionComponent) {
			((DecisionComponent) component).getModel().getData().setNotes(newValue);
		}			
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

