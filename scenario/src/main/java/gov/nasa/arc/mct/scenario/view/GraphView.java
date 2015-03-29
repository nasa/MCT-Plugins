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
import gov.nasa.arc.mct.scenario.component.GraphViewCapability;
import gov.nasa.arc.mct.scenario.util.CostType;
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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
	private static final NumberFormat FORMAT = new DecimalFormat();
	private static final Color DEFAULT_FOREGROUND_COLOR = Color.BLACK;
	
	public static final long SECOND_TO_MILLIS = 1000l;
	public static final long MINUTE_TO_MILLIS = 60000l;
	public static final long HOUR_TO_MILLIS = 3600000l;
	
	// whether has instantaneous/accumulative view for the selected component
	private boolean hasInstantaneous = true;
	private boolean hasAccumulative = true;
	
	// whether need to show instantaneous/accumulative for the selected component
	private boolean isInstantaneous = true;
	private boolean isAccumulative = true;
	
	private GraphViewCapability graphData;
	
	public GraphView(AbstractComponent ac, ViewInfo vi) {
		super(ac, vi);
		this.graphData = ac.getCapability(GraphViewCapability.class);;
		assert graphData != null;
		
		setForeground(DEFAULT_FOREGROUND_COLOR);
		setOpaque(false);
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		getContentPane().setOpaque(false);
		setHasGraph();
		addGraphs();
	}

	@Override
	public void viewPersisted() {
		rebuild();
	}
	
	// check whether draw instantaneous and accumulative graphs
	private void setHasGraph() {
		hasInstantaneous = graphData.hasInstantaneousGraph();
		hasAccumulative = graphData.hasAccumulativeGraph();
	}
	
	private void addGraphs() {
		for (CostFunctionCapability cost : getManifestedComponent().getCapabilities(CostFunctionCapability.class)) {
			CostType type = cost.getCostType();
			if (isInstantaneous && graphData.hasInstantaneous(type)) getContentPane().add(new CostGraph(cost, true));
			if (isAccumulative && graphData.hasAccumulative(type)) getContentPane().add(new CostGraph(cost, false));
		} 
	}
	
	class CostGraph extends JPanel implements CostOverlay {
		private static final long serialVersionUID = 2939539607481881113L;
		private CostFunctionCapability cost;
		private CostType type;
		private boolean isInstantaneous;

		private int x[] = {};
		private int y[] = {};
		private Double[] dataPoints = {};
		private Long[] time = {};
		private double minData;
		private double maxData;
		
		//private int cachedWidth = 0;
		
		public CostGraph(CostFunctionCapability cost, boolean isInstantaneous) {
			super();
			setOpaque(false);
			this.cost = cost;
			this.type = cost.getCostType();
			this.isInstantaneous = isInstantaneous;
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			add(Box.createVerticalStrut(GRAPH_HEIGHT + GRAPH_PAD * 2));
			updateGraph();
		}
		
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			int rightX = getPixelPosition(getEnd());
			
			// TODO: Updating may too much computation to do every frame
			//if (getWidth() != cachedWidth) {
				updateGraph();
				//cachedWidth = getWidth();
			//}
			
			// Choose color for data line
			// Note that hash ensures that the same cost always gets the same color,
			// but does not ensure color uniqueness. Should be OK since graphs are not overlaid
			g.setColor(ScenarioColorPalette.getColor(type.getName())); // TODO: Get from CostFunction ? 
			
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
			drawDataLine(g, charHeight, rightX);
			
			// Draw tick marks
			g.setColor(getForeground());
			g.drawLine(getLeftPadding()-1, GRAPH_PAD, getLeftPadding()-1, GRAPH_PAD + GRAPH_HEIGHT - 1);
			g.drawLine(getLeftPadding()+5, GRAPH_PAD, getLeftPadding()-1, GRAPH_PAD);
			g.drawLine(getLeftPadding()+5, GRAPH_PAD + GRAPH_HEIGHT - 1, getLeftPadding()-1, GRAPH_PAD+ GRAPH_HEIGHT - 1);
			String maxValueString = FORMAT.format(maxData);
			g.drawString(maxValueString, getLeftPadding() - getFontMetrics(getFont()).charsWidth(maxValueString.toCharArray(), 0, maxValueString.length()) - 8, GRAPH_PAD + charHeight/2);
			String minValueString = FORMAT.format(minData);
			g.drawString(minValueString, getLeftPadding() - getFontMetrics(getFont()).charsWidth(minValueString.toCharArray(), 0, minValueString.length()) - 8, GRAPH_PAD + GRAPH_HEIGHT + charHeight/2);
			
			String name = graphData.getDisplayName(type, isInstantaneous);
			String units = graphData.getUnits(type, isInstantaneous);
			g.setFont(getFont().deriveFont(Font.BOLD));
			g.drawString(units, getLeftPadding() - getFontMetrics(getFont()).charsWidth(units.toCharArray(), 0, units.length()) - 8, GRAPH_PAD + GRAPH_HEIGHT /2 + charHeight / 2);
			g.drawString(name, rightX - getFontMetrics(getFont()).charsWidth(name.toCharArray(), 0, name.length()), GRAPH_PAD + GRAPH_HEIGHT - 2);
		}
		
		protected void updateGraph() {			
			// Note: TreeSet is always sorted, meaning subsequent iteration occurs in drawing order
			Collection<Long> timeCollection = new TreeSet<Long>();
			Collection<Double> dataCollection = new ArrayList<Double>();
			
			Map<Long, Double> values = graphData.getData(type, isInstantaneous);
			if (values != null) {								
				dataCollection = values.values();
				timeCollection = values.keySet();
			}
					
			int size = timeCollection.size();						
			if (size > 1) {
				Double[] dataType = new Double[] {};
			    dataPoints = dataCollection.toArray(dataType);
			    Long[] timeType = new Long[] {};
			    time = timeCollection.toArray(timeType);
			    
			    setMinAndMax(dataPoints);
				int[] x = new int[size];
				int[] y = new int[size];
				for (int j = 0 ; j < size ; j++) {
					// Convert to x, y
					x[j] = toX(time[j]);
					y[j] = toY(dataPoints[j], minData, maxData);
				}
				this.x = x;
				this.y = y;
			}
		}
		
		protected void setMinAndMax(Double data[]) {
			double maxData = 0.0;
			double minData = 0.0;
			for (int i = 0; i < data.length; i++) {
				if (data[i] > maxData) maxData = data[i];
				if (data[i] < minData) minData = data[i];
			}
			this.minData = minData;
			this.maxData= maxData;
		}
		
		protected void drawDataLine(Graphics g, int charHeight, int rightX) {
			if (this.isInstantaneous) drawInstantaniousDataLine(g, charHeight, rightX);
			else drawAccumulativeDataLine(g, charHeight, rightX);
		}
		
		protected void drawInstantaniousDataLine(Graphics g, int charHeight, int rightX) {
			if (x.length > 1 && x.length == y.length) {
				for (int i = 0; i < x.length - 1; i++) {
					if (x[i] >= getLeftPadding() && x[i+1] <= rightX) {
						g.drawLine(x[i], y[i], x[i+1], y[i]);
						g.drawLine(x[i+1], y[i], x[i+1], y[i+1]);	
						
						double maxValue = Math.max(dataPoints[i], dataPoints[i+1]);
						double minValue = Math.min(dataPoints[i], dataPoints[i+1]);
						if (maxValue != minValue && x[i+1] > getLeftPadding() && x[i+1] < rightX) {
							int maxY = Math.min(y[i], y[i+1]);
							int minY = Math.max(y[i], y[i+1]);
							String maxValueString = FORMAT.format(maxValue);
							g.drawString(maxValueString, x[i+1] - getFontMetrics(getFont()).charsWidth(maxValueString.toCharArray(), 0, maxValueString.length())/2, maxY - charHeight / 4);
							String minValueString = FORMAT.format(minValue);
							g.drawString(minValueString, x[i+1] - getFontMetrics(getFont()).charsWidth(minValueString.toCharArray(), 0, minValueString.length())/2, minY + charHeight);
						}
					}
				}
			}
		}
		
		protected void drawAccumulativeDataLine(Graphics g, int charHeight, int rightX) {
			if (x.length > 1 && x.length == y.length) {
				for (int i = 0; i < x.length - 1; i++) {
					if (x[i] >= getLeftPadding() && x[i+1] <= rightX) {
						g.drawLine(x[i], y[i], x[i+1], y[i+1]);	
						double value = dataPoints[i+1];
						if (x[i+1] > getLeftPadding() && x[i+1] < rightX) {
							String valueString = FORMAT.format(value);
							g.drawString(valueString, x[i+1] - getFontMetrics(getFont()).charsWidth(valueString.toCharArray(), 0, valueString.length())/2, y[i+1] - charHeight / 4);
						}
					}
				}
			}
		}

		protected int toX(long t) {
			return (int) (getPixelScale() * (double) (t - getTimeOffset())) + getLeftPadding();
		}
		
		protected int toY(double data, double minData, double maxData) {
			return GRAPH_PAD + GRAPH_HEIGHT - (int) (((data - minData) / (maxData - minData)) * (GRAPH_HEIGHT-1)) - 1;
		}		
		
		@Override
		public List<CostFunctionCapability> getCostFunctions() {
			return Arrays.asList(cost);
		} 
	}

	@Override
	protected void rebuild() {
		getContentPane().removeAll();			
		addGraphs();
		getContentPane().revalidate();
	}

	public void setInstantanious(boolean isInstantanious) {
		this.isInstantaneous = isInstantanious;
	}

	public void setAccumulative(boolean isAccumulative) {
		this.isAccumulative = isAccumulative;
	}	
	

	public boolean hasInstantaneousGraph() {
		return hasInstantaneous;
	}

	public boolean hasAccumulativeGraph() {
		return hasAccumulative;
	}

		
}
