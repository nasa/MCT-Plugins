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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;


/*
 * Part of the package that enables/disables elements within a JComboBox.
 * Here, we do not allow the user to select the disabled elements
 * within a JComboBox
 * 
 * This class is used in the satellite wizard
 */
public class ComboListener implements ActionListener {
	
    JComboBox combo;
    Object currentItem;

    public ComboListener(JComboBox combo) {
      this.combo = combo;
      combo.setSelectedIndex(0);	//have no selection in the comboBox
      currentItem = combo.getSelectedItem();
    }

    /**
     * If the user selected an element (in the JComboBox)
     * that is disabled, do not change the currently selected
     * item.
     */
    public void actionPerformed(ActionEvent e) {
      Object tempItem = combo.getSelectedItem();
      if (!((CanEnable) tempItem).isEnabled()) {
        combo.setSelectedItem(currentItem);
      } else {
        currentItem = tempItem;
      }
    }

}//--end class ComboListener
