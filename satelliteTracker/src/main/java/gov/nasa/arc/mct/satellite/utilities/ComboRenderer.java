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

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;

/*
 * Part of the package that implements a JComboBox that allows elements to be
 * enabled or disabled.  This class draws the disabled elements blue, and
 * draws the enabled elements the way the List draws them
 */
@SuppressWarnings("serial")
public class ComboRenderer extends JLabel implements ListCellRenderer {
	
	public ComboRenderer() {
      setOpaque(true);
      setBorder(new EmptyBorder(1, 1, 1, 1));
    }

    public Component
    getListCellRendererComponent
    ( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
	      if (isSelected) {
	        setBackground(list.getSelectionBackground());
	        setForeground(list.getSelectionForeground());
	      } else {
	    	setBackground(list.getBackground());
	        setForeground(list.getForeground());
	      }
	      if (!((CanEnable) value).isEnabled()) { //if we are a disabled element
	    	 setBackground(list.getBackground());
	        //setForeground(UIManager.getColor("Label.disabledForeground"));
	        setForeground(Color.BLUE);
	      }
	      setFont(list.getFont());
	      setText((value == null) ? "" : value.toString()); //trim off that extra whitespace
	      return this;
    }
}