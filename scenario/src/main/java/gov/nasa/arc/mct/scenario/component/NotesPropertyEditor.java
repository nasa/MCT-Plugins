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

