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

import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.gui.View;
import gov.nasa.arc.mct.scenario.component.CostFunctionCapability;
import gov.nasa.arc.mct.scenario.component.TimelineComponent;
import gov.nasa.arc.mct.services.component.ViewInfo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class ScenarioView extends AbstractTimelineView {
	private static final long serialVersionUID = 4734756748449290286L;

	public ScenarioView(AbstractComponent ac, ViewInfo vi) {
		super(ac, vi);
		
		setOpaque(false);
		
		JPanel upperPanel = new JPanel();
		upperPanel.setLayout(new BoxLayout(upperPanel, BoxLayout.Y_AXIS));
		upperPanel.setOpaque(false);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(upperPanel, BorderLayout.NORTH);
		getContentPane().setBackground(Color.WHITE);
		//getContentPane().setOpaque(false);
		
		//TODO: Use clone strategy, set work unit delegate for kids
		
		for (AbstractComponent child : ac.getComponents()) {
			if (child instanceof TimelineComponent) {
				upperPanel.add(createTimeline((TimelineComponent) child));
			}
		}
		
		List<CostFunctionCapability> costs = ac.getCapabilities(CostFunctionCapability.class);
		if (costs != null && !costs.isEmpty()) {
			upperPanel.add(new CollapsibleContainer(GraphView.VIEW_INFO.createView(getManifestedComponent())));
		}
	}
	
	private Component createTimeline(TimelineComponent component) {
		View view = TimelineView.VIEW_INFO.createView(component);
		View label = LabelView.VIEW_INFO.createView(component);
		JComponent container = new CollapsibleContainer(view, label);

		container.setOpaque(true);
		container.setBackground(Color.LIGHT_GRAY);
		
		return container;
	}

	
}
