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
package gov.nasa.arc.mct.satellite.utilities;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import jsattrak.utilities.TLE;

/*
 * This cell renderer is for the TLE lists, so the TLE satellite names are displayed within the list
 * 
 * TODO: Currently only satellite names are displayed in the lists (see Satellite Wizard). Maybe display
 *       the satellite number as well as the satellite name (as the satellite number is unique).  This
 *       might be cool for identifying the space debris (as each debris grouping has the same name--i.e:
 *       the iridium33 debris)
 *       
 *       This class is used wrt a JComboBox, used in the satellite wizard
 */
@SuppressWarnings("serial")
public class TLEListRenderer extends JLabel implements ListCellRenderer {

	public TLEListRenderer
	() {
		setOpaque(true);
	}
	
	public Component
		getListCellRendererComponent
		( JList list, Object value, 
	  int index,   
      boolean isSelected,  
      boolean cellHasFocus)
	{
		  if (isSelected) {
			  setBackground(list.getSelectionBackground());
			  setForeground(list.getSelectionForeground());
	      } else {
	    	  setBackground(list.getBackground());
	    	  setForeground(list.getForeground());
	      }
		  setText(((TLE)value).getSatName());  
		  setOpaque(true);  
	     
		  return this;
		  
	}//--end getListCellRendererComponent  
	
}
