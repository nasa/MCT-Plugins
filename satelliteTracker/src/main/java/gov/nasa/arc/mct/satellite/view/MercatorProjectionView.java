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
package gov.nasa.arc.mct.satellite.view;

import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.components.ExtendedProperties;
import gov.nasa.arc.mct.components.FeedProvider;
import gov.nasa.arc.mct.components.FeedProvider.RenderingInfo;
import gov.nasa.arc.mct.gui.FeedView;
import gov.nasa.arc.mct.gui.FeedView.RenderingCallback;
import gov.nasa.arc.mct.roles.events.PropertyChangeEvent;
import gov.nasa.arc.mct.satellite.MercatorPanel;
import gov.nasa.arc.mct.satellite.MercatorPanel.ViewChangeListener;
import gov.nasa.arc.mct.satellite.Trajectory;
import gov.nasa.arc.mct.satellite.Vector;
import gov.nasa.arc.mct.services.component.ViewInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;


/*
 * While MercatorPanel handles the drawing of the Mercator Map, as well handling all the drawings of
 * trajectories, MercatorProjectionView is the middle-man between MCT and the MercatorPanel.  This class
 * takes the trajectories from MCT and sends them to the MercatorPanel to be drawn.
 * 
 * Note: Who can be viewed on a MercatorProjection? The policy takes care of that, see MercatorProjectionViewPolicy.
 *       How does MCT know that this is a view? SatelliteComponentProvider tells MCT that this is a view.
 */
@SuppressWarnings("serial")
public class MercatorProjectionView extends FeedView implements RenderingCallback, ViewChangeListener {

	private static final int MAX_FEEDS = 3;
	
	private Map<String, DoubleField> fieldProviders = new HashMap<String, DoubleField>();
	private List<FeedProvider> feedProviders = new ArrayList<FeedProvider>();
	
	private MercatorPanel mercatorPanel;
	
	
	private static final String X_ROTATION_KEY = "GLOBAL_X_ROTATION";
	private static final String Y_ROTATION_KEY = "GLOBAL_Y_ROTATION";
	private static final String ZOOM_KEY       = "GLOBAL_ZOOM";
	private static final String X_POSITION_KEY = "GLOBAL_X_POSITION";
	private static final String Y_POSITION_KEY = "GLOBAL_Y_POSITION";
	
	
	public MercatorProjectionView(AbstractComponent component, ViewInfo info) {
		super(component, info);
		
		Map<JComponent, Trajectory> trajectories = new HashMap<JComponent, Trajectory>();
		
		List<AbstractComponent> potentiallyViewable = new ArrayList<AbstractComponent>();
		potentiallyViewable.add(component);
		potentiallyViewable.addAll(component.getComponents());
		
		for (AbstractComponent candidate : potentiallyViewable) {
			int i = 0;
			TrajectoryProvider tp = new TrajectoryProvider();			
			for (AbstractComponent vector : candidate.getComponents()) {
				for (AbstractComponent element : vector.getComponents()) {
					FeedProvider fp = element.getCapability(FeedProvider.class);
					if (fp != null && i < MAX_FEEDS) {
						feedProviders.add(fp);
						fieldProviders.put(fp.getSubscriptionId(), tp.fields[i++]);
					}
				}
			}
			
			//there must be at least three feeds associated with the candidate for it to be drawn on the mercator projection 
			if ( i >= 3) { // Did we find enough feeds to plot the course of this object?
				trajectories.put(TinyView.VIEW_INFO.createView(candidate), tp);
			}
		}

		requestData(feedProviders, System.currentTimeMillis(), System.currentTimeMillis(), 
				new DataTransformation() {
					@Override
					public void transform(
							Map<String, List<Map<String, String>>> data,
							long startTime, long endTime) {}
				},
				this, 
				false);	
		
		mercatorPanel = new MercatorPanel(trajectories);
		add (mercatorPanel);
		// earth.addViewChangeListener(this);
	}

	
	/*
	 * (non-Javadoc)
	 * @see gov.nasa.arc.mct.gui.FeedView#updateFromFeed(java.util.Map)
	 */
	@Override
	public void updateFromFeed(Map<String, List<Map<String, String>>> data) {
		for (FeedProvider fp : feedProviders) {
			DoubleField field = fieldProviders.get(fp.getSubscriptionId());
			List<Map<String, String>> subdata = data.get(fp.getSubscriptionId());
			if (field != null && subdata != null && subdata.size() > 0) {
				Map<String, String> packet = subdata.get(subdata.size() - 1);
				RenderingInfo ri = fp.getRenderingInfo(packet);
				try {
					field.set(Double.parseDouble(ri.getValueText()));
				} catch (NumberFormatException nfe) {
					nfe.printStackTrace();  //TODO: Just ignore or show some kind of status?
				}
			}
		}
		repaint();
	}

	/*
	 * (non-Javadoc)
	 * @see gov.nasa.arc.mct.gui.FeedView#synchronizeTime(java.util.Map, long)
	 */
	@Override
	public void synchronizeTime(Map<String, List<Map<String, String>>> data,
			long syncTime) {
		updateFromFeed(data);
	}

	/*
	 * (non-Javadoc)
	 * @see gov.nasa.arc.mct.gui.FeedView#getVisibleFeedProviders()
	 */
	@Override
	public Collection<FeedProvider> getVisibleFeedProviders() {
		return feedProviders;
	}

	/*
	 * (non-Javadoc)
	 * @see gov.nasa.arc.mct.gui.FeedView.RenderingCallback#render(java.util.Map)
	 */
	@Override
	public void render(Map<String, List<Map<String, String>>> data) {
		updateFromFeed(data);	
	}
	
	/*
	 * (non-Javadoc)
	 * @see gov.nasa.arc.mct.gui.View#updateMonitoredGUI(gov.nasa.arc.mct.roles.events.PropertyChangeEvent)
	 */
	@Override
	public void updateMonitoredGUI(PropertyChangeEvent event) {
	}

	/*
	 * (non-Javadoc)
	 * @see gov.nasa.arc.mct.satellite.MercatorPanel.ViewChangeListener#viewChanged(double, double, double, double, double)
	 */
	@Override
	public void viewChanged(double xr, double yr, double px, double py,
			double zoom) {
		ExtendedProperties vp = getViewProperties();
		vp.setProperty(X_ROTATION_KEY, Double.toString(xr  ));
		vp.setProperty(Y_ROTATION_KEY, Double.toString(yr  ));
		vp.setProperty(X_POSITION_KEY, Double.toString(px  ));
		vp.setProperty(Y_POSITION_KEY, Double.toString(py  ));
		vp.setProperty(ZOOM_KEY,       Double.toString(zoom));
		getManifestedComponent().save();
	}
	
	/*
	 * This private class holds the ECEF coordinate positions coming in from the feeds, and
	 * is used in the TrajectoryProvider class
	 */
	private class DoubleField {
		private double value;
		public void set(double v) { value = v; }
		public double get () { return value; }
	}

	/*This class stores the three-ECEF-coordinates of the object being displayed on the Mercator Projection 
	 *Note: Velocity is not used for the MercatorView (as the view is just being updated every second)
	 */
	private class TrajectoryProvider implements Trajectory{
		public DoubleField fields[] = new DoubleField[MAX_FEEDS];
		
		public TrajectoryProvider() {
			for (int i = 0; i < fields.length; i++) fields[i] = new DoubleField();
		}
		
		@Override
		public Vector getPosition() {
			return new Vector(fields[0].get(), fields[1].get(), fields[2].get());
		}
		@Override
		public Vector getVelocity() {
			return new Vector(0, 0, 0);		
		}	
	}
}