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

import gov.nasa.arc.mct.gui.View;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A collapsible Swing container for an MCT view. Includes a little triangle 
 * to control the visibility of the view beneath.
 * 
 * @author vwoeltje
 *
 */
public class CollapsibleContainer extends JPanel {
	private static final long serialVersionUID = -7397365143392342779L;
	
	private View view;
	private Component label;

	/**
	 * Create a new collapsible container for the specified view. 
	 * This will be labeled with the view's name.
	 * @param view
	 */
	public CollapsibleContainer(View view) {
		this(view, new JLabel(view.getInfo().getViewName()));
	}
	
	/**
	 * Create a new collapsible container for the specified view; 
	 * this will be labeled by the provided Swing component.
	 * @param view
	 * @param label
	 */
	public CollapsibleContainer(View view, Component label) {
		this.view = view;
		this.label = label;
		
		setLayout(new BorderLayout());
		add(view, BorderLayout.CENTER);
		add(createTopPanel(), BorderLayout.NORTH);
		
		setOpaque(false);
	}
	
	private JComponent createTopPanel() {
		JPanel panel = new JPanel();
		JLabel twister = new JLabel(new TwistIcon());
		panel.setOpaque(false);
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.add(Box.createHorizontalStrut(8));
		panel.add(twister);
		panel.add(label);
		twister.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				view.setVisible(!view.isVisible());
			}			
		});
		return panel;
	}
	
	private class TwistIcon implements Icon {
		private final int size;
	
		public TwistIcon() {
			size = 10;
		}
		
		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			// Draw a triangle pointing either right or left
			int halfSize = getIconWidth() / 2;
			int fromCenter = halfSize - 2;
			int[] xPts = new int[]{ halfSize - fromCenter, halfSize - fromCenter, halfSize + fromCenter};
			int[] yPts = new int[]{ halfSize - fromCenter, halfSize + fromCenter, halfSize };
			for (int i = 0; i < 3; i++) {
				xPts[i] += x;
				yPts[i] += y;
			}
			if (!view.isVisible()) {
				g.fillPolygon(xPts, yPts, 3);
			} else {
				g.fillPolygon(yPts, xPts, 3);
			}
		}

		@Override
		public int getIconWidth() {
			return size;
		}

		@Override
		public int getIconHeight() {
			return size;
		}
		
	}

}
