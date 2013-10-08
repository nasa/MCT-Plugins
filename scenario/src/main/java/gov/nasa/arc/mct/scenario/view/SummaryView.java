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
import gov.nasa.arc.mct.scenario.component.CostFunctionComponent;
import gov.nasa.arc.mct.scenario.component.DurationCapability;
import gov.nasa.arc.mct.scenario.component.TagCapability;
import gov.nasa.arc.mct.services.component.ViewInfo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class SummaryView extends View {
	private static final long serialVersionUID = -1683480883187742150L;
	private int MAX_SAMPLES = 2 << 8;
	private Map<String, Summary> costSummaries =
			new HashMap<String, Summary>();
	private TagSet highlighted = null;
	
	
	private PieChart chart;
	private Legend legend;
	
	public SummaryView(AbstractComponent ac, ViewInfo vi) {
		super(ac,vi);

		// Default to one-second samples
		double sampleSize = 1000.0;
		DurationCapability dc = ac.getCapability(DurationCapability.class);
		if (dc != null) {
			sampleSize = (double) (dc.getEnd() - dc.getStart()) / (double) MAX_SAMPLES;
			if (sampleSize < 1.0) {
				sampleSize = 1.0;
			}
		}
		
		summarize(ac, sampleSize, new HashSet<TagCapability>(), new HashSet<String>());		
		
		List<String> costNames = new ArrayList<String>();
		costNames.addAll(costSummaries.keySet());
		Collections.sort(costNames);
		
		setLayout(new BorderLayout());
		
		Summary summary = costNames.isEmpty() ?
				new Summary("") : costSummaries.get(costNames.get(0));

		chart = new PieChart(summary);
		legend = new Legend(summary);
		
		JPanel upperPanel = new JPanel(new BorderLayout());
		upperPanel.setOpaque(false);
		
		upperPanel.add(legend, BorderLayout.EAST);
		add(upperPanel, BorderLayout.NORTH);
		add(chart, BorderLayout.CENTER);
		setBackground(Color.DARK_GRAY);	
	}
		
	
	private void summarize(AbstractComponent ac, double sampleSize, Set<TagCapability> tagContext, Set<String> ignore) {
		boolean visitChildren = true;
		Collection<CostFunctionCapability> costs;
		if (ac instanceof CostFunctionComponent) {
			costs = ((CostFunctionComponent) ac).getInternalCostFunctions();
			visitChildren = true;
		} else {
			// Recognize costs from non-CostFunctionComponents, but don't dig deeper
			costs = ac.getCapabilities(CostFunctionCapability.class);
			if (costs != null && !costs.isEmpty()) {
				visitChildren = false;
			}
		}
		Set<TagCapability> activeTagContext = tagContext;
		Collection<TagCapability> tagCapabilities = ac.getCapabilities(TagCapability.class);
		if (tagCapabilities != null && !tagCapabilities.isEmpty()) {
			activeTagContext = new HashSet<TagCapability>();
			activeTagContext.addAll(tagContext);
			for (TagCapability tag : tagCapabilities) {
				activeTagContext.add(tag);
			}
		}
		if (costs != null) {
			for (CostFunctionCapability cost : costs) {
				Collection<Long> changeTimes = cost.getChangeTimes();
				if (changeTimes.size() > 1) {
					String name = cost.getName();
					if (!costSummaries.containsKey(name)) {
						costSummaries.put(name, new Summary(name));
					}			
					Iterator<Long> it = changeTimes.iterator();
					long first = it.next();
					long last  = it.next();
					while (it.hasNext()) {
						last = it.next();
					}
					// TODO: Maybe just add getTotalCost to CostFunctionCapability
					for (double t = first; t < last; t += sampleSize) {
						double thisSampleSize = (t + sampleSize) > last ?
								(last - t) : sampleSize;
						double value = cost.getValue((long)(t + thisSampleSize/2)) * thisSampleSize;
						costSummaries.get(name).add(value, activeTagContext);
					}					
				}
			}
		}
		ignore.add(ac.getComponentId());
		if (visitChildren == true) {
			for (AbstractComponent child : ac.getComponents()) {
				if (!ignore.contains(child.getComponentId())) {
					summarize(child, sampleSize, activeTagContext, ignore);
				}
			}
		}
	}
	
	private class Highlighter extends MouseAdapter {
		private TagSet tagSet;

		public Highlighter(TagSet tagSet) {
			this.tagSet = tagSet;
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			highlighted = tagSet;
			Object source = e.getSource();
			if (source instanceof JComponent) {
				((JComponent) source).repaint();
			}
			chart.repaint();
		}

		@Override
		public void mouseExited(MouseEvent e) {
			if (tagSet.equals(highlighted)) {
				highlighted = null;
			}
			Object source = e.getSource();
			if (source instanceof JComponent) {
				((JComponent) source).repaint();
			}
			chart.repaint();			
		}
	}
	
	private class Legend extends JPanel {
		private static final long serialVersionUID = 4396877097465579358L;
		
		public Legend(Summary summary) {
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			setOpaque(false);
			setSummary(summary);
		}
		
		public void setSummary(Summary summary) {
			List<TagSet> entries = new ArrayList<TagSet>();
			for (Set<TagCapability> tags : summary.getContributions().keySet()) {
				entries.add(new TagSet(tags));
			}
			Collections.sort(entries);
			
			removeAll();
			
			for (TagSet tagSet : entries) {
				add(makeEntry(tagSet));
			}
		}
		
		private JPanel makeEntry(TagSet tagSet) {
			JPanel entry = new JPanel();
			entry.setOpaque(false);
			entry.setLayout(new BoxLayout(entry, BoxLayout.LINE_AXIS));	
						
			entry.add(new JLabel(new LegendIcon(tagSet)));
			for (TagCapability tag : tagSet.tags) {
				JComponent view = 
						LabelView.VIEW_INFO.createView(tag.getComponentRepresentation());
				view.setForeground(Color.LIGHT_GRAY);						
				entry.add(view);
			}
			if (tagSet.tags.isEmpty()) {
				JLabel label = new JLabel(tagSet.toString());
				label.setForeground(Color.LIGHT_GRAY);				
				entry.add(label);
			}
			entry.setAlignmentX(LEFT_ALIGNMENT);
			entry.addMouseListener(new Highlighter(tagSet));
			return entry;
		}
		
	}

	private static int ICON_HEIGHT = 14;
	private static int ICON_WIDTH = 20;
	private class LegendIcon implements Icon {
		private TagSet tagSet;
		private Color color;
		
		public LegendIcon(TagSet tagSet) {
			this.tagSet = tagSet;
			this.color = ScenarioColorPalette.getColor(tagSet.toString());
		}
				
		@Override
		public int getIconHeight() {
			return ICON_HEIGHT;
		}

		@Override
		public int getIconWidth() {
			return ICON_WIDTH;
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			if (g instanceof Graphics2D) {
				((Graphics2D) g).setRenderingHint(
						RenderingHints.KEY_ANTIALIASING, 
						RenderingHints.VALUE_ANTIALIAS_ON
				);
			}
			g.setColor(tagSet.equals(highlighted) ? color.brighter() : color);
			g.fillRoundRect(x, y, ICON_WIDTH-1, ICON_HEIGHT-1, ICON_WIDTH/2, ICON_WIDTH/2);
			g.setColor(color.darker());
			g.drawRoundRect(x, y, ICON_WIDTH-1, ICON_HEIGHT-1, ICON_WIDTH/2, ICON_WIDTH/2);
		}
		
	}
	
	private class PieChart extends JComponent {
		private static final long serialVersionUID = -3249915607722487317L;
		private List<PieSlice> slices;
		private double total;
		
		public PieChart(Summary summary){
			setSummary(summary);
		}
		
		public void setSummary(Summary summary) {
			slices = new ArrayList<PieSlice>();
			for (Entry<Set<TagCapability>, Double> contribution : summary.getContributions().entrySet()) {
				slices.add(new PieSlice(contribution.getKey(), contribution.getValue()));
			}
			Collections.sort(slices);
			this.total = summary.costTotal;
		}
		
		@Override
		public void paintComponent (Graphics g) {
			if (slices != null && !slices.isEmpty()) {
				Arc2D arc2D = null;
				double start = 0;
				int size = Math.min(getWidth(), getHeight()) - 10;
				int x    = getWidth() / 2 - size / 2;
				int y    = getHeight() / 2 - size / 2;
				if (g instanceof Graphics2D) {
					arc2D = new Arc2D.Double(Arc2D.PIE);
					arc2D.setFrame(x, y, size, size);
					((Graphics2D) g).setRenderingHint(
							RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				}
				for (PieSlice slice : slices) {
					double arc = (slice.cost / total) * 360.0;
					if (g instanceof Graphics2D) {
						arc2D.setAngleStart(start);
						arc2D.setAngleExtent(arc);
						Color c = ScenarioColorPalette.getColor(slice.toString());						
						g.setColor(slice.equals(highlighted) ? c.brighter() : c);
						((Graphics2D) g).fill(arc2D);
						g.setColor(Color.BLACK);
						((Graphics2D) g).draw(arc2D);
					} else {
						g.setColor(ScenarioColorPalette.getColor(slice.toString()));
						g.fillArc(x, y, size, size, (int) start, (int) arc);
						g.setColor(Color.BLACK);
						g.drawArc(x, y, size, size, (int) start, (int) arc);
					}
					start += arc;
				}
				g.setColor(ScenarioColorPalette.getColor("Untagged"));
				g.fillArc(x, y, size, size, (int) start, (int) (360.0 - start));				
			}
		}
		
		private class PieSlice extends TagSet {
			private double cost;
			
			public PieSlice(Collection<TagCapability> tags, double cost) {
				super(tags);
				this.cost = cost;
			}
		}
		
	}
	
	private static class TagSet implements Comparable<TagSet> {
		private List<TagCapability> tags = new ArrayList<TagCapability>();
		private String string = "";

		public TagSet(Collection<TagCapability> tags) {
			this.tags.addAll(tags);
			Collections.sort(this.tags, new Comparator<TagCapability>() {
				@Override
				public int compare(TagCapability a, TagCapability b) {
					return a.getTag().compareTo(b.getTag());
				}				
			});
			for (TagCapability tag : this.tags){
				string += tag.getTag() + " ";
			}
		}	
		
		public String toString() {
			return string.isEmpty() ? "Untagged" : string;
		}

		@Override
		public int compareTo(TagSet other) {
			return toString().compareTo(other.toString());
		}		
		
		@Override
		public int hashCode() {
			return toString().hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			return o instanceof TagSet && compareTo((TagSet)o) == 0;
		}
	}
	
	private static class Summary {
		private String costName;
		private double costTotal;
		private Map<Set<TagCapability>,Set<TagCapability>> tagSets =
				new HashMap<Set<TagCapability>,Set<TagCapability>>();
		private Map<Set<TagCapability>, Double> contributions = 
				new HashMap<Set<TagCapability>, Double>();
		// Note: Set does override equals as expected
		
		public Summary(String costName) {
			this.costName = costName;
			this.costTotal = 0;
		}
		
		public void add(double value, Set<TagCapability> tagContext) {
			if (!contributions.containsKey(tagContext)) {
				Set<TagCapability> set = new HashSet<TagCapability>();
				set.addAll(tagContext);
				contributions.put(set, 0.0);
				tagSets.put(set,set);
			}
			contributions.put(tagSets.get(tagContext), 
					contributions.get(tagContext) + value);
			costTotal += value;
		}
		
		/**
		 * Create a new summary, only considering a specific set of tags.
		 * @param tags the tags to be considered in the summary
		 * @return a new summary, considering only those tags
		 */
		public Summary subSummary(Set<TagCapability> tags) {
			Summary summary = new Summary(costName);
			for (Entry<Set<TagCapability>, Double> contribution : 
				 contributions.entrySet()) {
				summary.add(contribution.getValue(), intersection(tags, contribution.getKey()));
			}
			return summary;
		}
		
		public Map<Set<TagCapability>, Double> getContributions() {
			return contributions;
		}
		
		public double getTotal() {
			return costTotal;
		}
		
		// Utility method - set intersection
		private <T> Set<T> intersection(Set<T> a, Set<T> b) {
			if (a.containsAll(b)) {
				return b;
			} else if (b.containsAll(a)) {
				return a;
			} else {
				Set<T> intersection = new HashSet<T>();
				for (T item : a) {
					if (b.contains(item)) {
						intersection.add(item);
					}
				}
				return intersection;
			}			
		}
	}
	
}
