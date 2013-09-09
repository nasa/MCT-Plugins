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
 * Property editor to support specification of an Activity's communications cost in the Info View.
 *
 */
public final class CommPropertyEditor implements PropertyEditor<Object> {
	ActivityComponent activityComponent = null;

	public CommPropertyEditor(AbstractComponent component) {
		activityComponent = (ActivityComponent)component;
	}

	@Override
	public String getAsText() {
		double numericData = activityComponent.getModel().getData().getComm();
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
		businessModel.setComm(d);
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
