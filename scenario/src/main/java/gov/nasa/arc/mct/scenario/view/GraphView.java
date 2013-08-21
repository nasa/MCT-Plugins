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
import gov.nasa.arc.mct.scenario.component.CostFunctionCapability;
import gov.nasa.arc.mct.scenario.view.TimelineLocalControls.CostOverlay;
import gov.nasa.arc.mct.services.component.ViewInfo;
import gov.nasa.arc.mct.services.component.ViewType;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

/**
 * A View showing a graph of costs associated with a component, as expressed 
 * by abstractComponent.getCapabilities(CostFunctionCapability.class)
 * 
 * @author vwoeltje
 *
 */
public class GraphView extends AbstractTimelineView {
	private static final long serialVersionUID = -2300291952094003401L;

	public static final String VIEW_ROLE_NAME = "Graph";
	public static final ViewInfo VIEW_INFO = 
			new ViewInfo(GraphView.class, GraphView.VIEW_ROLE_NAME, ViewType.EMBEDDED);
	
	private static final Stroke GRAPH_STROKE = new BasicStroke(2f);
	private static final int GRAPH_HEIGHT = 60;
	private static final int GRAPH_PAD    = 16;
	
	private static final Color DEFAULT_FOREGROUND_COLOR = Color.BLACK;
	
	
	public GraphView(AbstractComponent ac, ViewInfo vi) {
		super(ac, vi);
		
		setForeground(DEFAULT_FOREGROUND_COLOR);
		setOpaque(false);
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		getContentPane().setOpaque(false);
		for (CostFunctionCapability cost : ac.getCapabilities(CostFunctionCapability.class)) {
			getContentPane().add(new CostGraph(cost));
		}
	}

	@Override
	public void viewPersisted() {
		getContentPane().removeAll();		
		for (CostFunctionCapability cost : getManifestedComponent().getCapabilities(CostFunctionCapability.class)) {
			getContentPane().add(new CostGraph(cost));
		}
	}


	private class CostGraph extends JPanel implements CostOverlay {
		private static final long serialVersionUID = 2939539607481881113L;
		private CostFunctionCapability cost;

		private int x[] = {};
		private int y[] = {};
		private double dataPoints[] = {};
		private double minData;
		private double maxData;
		
		//private int cachedWidth = 0;
		
		public CostGraph(CostFunctionCapability cost) {
			super();
			setOpaque(false);
			this.cost = cost;
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			add(Box.createVerticalStrut(GRAPH_HEIGHT + GRAPH_PAD * 2));
			updateGraph();
		}
		
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			// TODO: Updating may too much computation to do every frame
			//if (getWidth() != cachedWidth) {
				updateGraph();
				//cachedWidth = getWidth();
			//}
			
			// Choose color for data line
			// Note that hash ensures that the same cost always gets the same color,
			// but does not ensure color uniqueness. Should be OK since graphs are not overlaid
			g.setColor(ScenarioColorPalette.getColor(cost.getName())); // TODO: Get from CostFunction ? 
			
			// Draw smoothly, if possible
			if (g instanceof Graphics2D) {
				((Graphics2D) g).setStroke(GRAPH_STROKE);
				RenderingHints renderHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				renderHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				((Graphics2D) g).setRenderingHints(renderHints);
			}
						
			// Draw the data line. Note that points have been computed in a separate method
			int charHeight = getFontMetrics(getFont()).getHeight();
			if (x.length > 1 && x.length == y.length) {
				for (int i = 0; i < x.length - 1; i++) {
					if (x[i] >= getLeftPadding() && x[i+1] <= getWidth() - getRightPadding()) {
						g.drawLine(x[i], y[i], x[i+1], y[i]);
						g.drawLine(x[i+1], y[i], x[i+1], y[i+1]);
							
						double maxValue = Math.max(dataPoints[i], dataPoints[i+1]);
						double minValue = Math.min(dataPoints[i], dataPoints[i+1]);
						if (maxValue != minValue && x[i+1] > getLeftPadding() && x[i+1] < getWidth() - getRightPadding()) {
							int maxY = Math.min(y[i], y[i+1]);
							int minY = Math.max(y[i], y[i+1]);
							String maxValueString = Double.toString(maxValue);
							g.drawString(maxValueString, x[i+1] - getFontMetrics(getFont()).charsWidth(maxValueString.toCharArray(), 0, maxValueString.length())/2, maxY - charHeight / 4);
							String minValueString = Double.toString(minValue);
							g.drawString(minValueString, x[i+1] - getFontMetrics(getFont()).charsWidth(minValueString.toCharArray(), 0, minValueString.length())/2, minY + charHeight);
						}
					}
				}
			}
			
			// Draw tick marks
			g.setColor(getForeground());
			g.drawLine(getLeftPadding()-1, GRAPH_PAD, getLeftPadding()-1, GRAPH_PAD + GRAPH_HEIGHT - 1);
			g.drawLine(getLeftPadding()+5, GRAPH_PAD, getLeftPadding()-1, GRAPH_PAD);
			g.drawLine(getLeftPadding()+5, GRAPH_PAD + GRAPH_HEIGHT - 1, getLeftPadding()-1, GRAPH_PAD+ GRAPH_HEIGHT - 1);
			String maxValueString = Double.toString(maxData);
			g.drawString(maxValueString, getLeftPadding() - getFontMetrics(getFont()).charsWidth(maxValueString.toCharArray(), 0, maxValueString.length()) - 8, GRAPH_PAD + charHeight/2);
			String minValueString = Double.toString(minData);
			g.drawString(minValueString, getLeftPadding() - getFontMetrics(getFont()).charsWidth(minValueString.toCharArray(), 0, minValueString.length()) - 8, GRAPH_PAD + GRAPH_HEIGHT + charHeight/2);
			
			String name = cost.getName();
			String units = cost.getUnits();
			g.setFont(getFont().deriveFont(Font.BOLD));
			g.drawString(units, getLeftPadding() - getFontMetrics(getFont()).charsWidth(units.toCharArray(), 0, units.length()) - 8, GRAPH_PAD + GRAPH_HEIGHT /2 + charHeight / 2);
			g.drawString(name, getWidth() - getRightPadding() - getFontMetrics(getFont()).charsWidth(name.toCharArray(), 0, name.length()), GRAPH_PAD + GRAPH_HEIGHT - 2);
		}
		
		private int toX(long t) {
			return (int) (getPixelScale() * (double) (t - getTimeOffset())) + getLeftPadding();
		}
		
		private int toY(double data, double minData, double maxData) {
			return GRAPH_PAD + GRAPH_HEIGHT - (int) (((data - minData) / (maxData - minData)) * (GRAPH_HEIGHT-1)) - 1;
		}
		
		private void updateGraph() {
			// Note: TreeSet is always sorted, meaning subsequent iteration occurs in drawing order
			Collection<Long> changeTimes = new TreeSet<Long>(); 
			changeTimes.addAll(cost.getChangeTimes());
			changeTimes.add(getStart());
			changeTimes.add(getEnd());
			if (changeTimes.size() > 1) {
				double data[] = new double[changeTimes.size()];
				long   time[] = new long[changeTimes.size()];
				double maxData = Double.NEGATIVE_INFINITY;
				double minData = Double.POSITIVE_INFINITY;
				int i = 0;
				for (Long t : changeTimes) {
					data[i]   = cost.getValue(t);
					if (data[i] > maxData) maxData = data[i];
					if (data[i] < minData) minData = data[i];
					time[i++] = t;
				}
				
				this.minData = minData;
				this.maxData = maxData;
				x = new int[i];
				y = new int[i];
				dataPoints = new double[i];
				for (int j = 0 ; j < i ; j++) {
					// Convert to x, y
					x[j] = toX(time[j]);
					y[j] = toY(data[j], minData, maxData);
					dataPoints[j] = data[j];
				}
			}
		}
		
		@Override
		public List<CostFunctionCapability> getCostFunctions() {
			return Arrays.asList(cost);
		}
	}



	
		
}
