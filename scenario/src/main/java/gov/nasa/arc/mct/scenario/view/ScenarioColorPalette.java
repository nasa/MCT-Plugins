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
package gov.nasa.arc.mct.scenario.view;

import java.awt.Color;

/**
 * Serves colors to various views related to Scenarios. Users of this 
 * class include Cost graphs and colorized Activities in a timeline. 
 * 
 * @author vwoeltje
 *
 */
public class ScenarioColorPalette {

	private static final Color GRAPH_PALETTE[] = {
		new Color(203, 217, 77), 
		new Color(242, 163, 16), 
		new Color(77, 217, 203), 
		new Color(16, 163, 242),
		new Color(163, 16, 242),
		new Color(242, 16, 163),
		new Color(16, 242, 163),
		new Color(163, 242, 16),
		new Color(217, 77, 217)
	};
	
	/**
	 * Get a color for the provided key.
	 * 
	 * This key may be arbitrary. This method is guaranteed to return the same 
	 * color for the same key across different calls to the method.
	 * 
	 * @param key
	 * @return
	 */
	public static final Color getColor(String key) {
		return GRAPH_PALETTE[Math.abs(key.hashCode()) % GRAPH_PALETTE.length];
	}
	
	/**
	 * Get a color, mixed toward some target color.
	 * 
	 * (Not every view makes sense with the default palette - for instance, it 
	 * is too bright for use on Activities - so this method is provided to 
	 * allow a bias toward a neutral color.)
	 * 
	 * @param key
	 * @param reference
	 * @param bias
	 * @return
	 */
	public static final Color getColorMixed(String key, Color reference, float bias) {
		Color palette = getColor(key);		
		
		return new Color(
				(int) ((float)palette.getRed() * (1.0f-bias) + (float)reference.getRed() * bias),
				(int) ((float)palette.getGreen() * (1.0f-bias) + (float)reference.getGreen() * bias),
				(int) ((float)palette.getBlue() * (1.0f-bias) + (float)reference.getBlue() * bias)
				);
	}
}
