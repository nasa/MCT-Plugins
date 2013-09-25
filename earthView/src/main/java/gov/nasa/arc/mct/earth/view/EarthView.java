package gov.nasa.arc.mct.earth.view;

import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.components.ExtendedProperties;
import gov.nasa.arc.mct.components.FeedProvider;
import gov.nasa.arc.mct.components.FeedProvider.RenderingInfo;
import gov.nasa.arc.mct.earth.EarthPanel;
import gov.nasa.arc.mct.earth.EarthPanel.ViewChangeListener;
import gov.nasa.arc.mct.earth.Trajectory;
import gov.nasa.arc.mct.earth.Vector;
import gov.nasa.arc.mct.gui.FeedView;
import gov.nasa.arc.mct.gui.FeedView.RenderingCallback;
import gov.nasa.arc.mct.roles.events.PropertyChangeEvent;
import gov.nasa.arc.mct.services.component.ViewInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;

public class EarthView extends FeedView implements RenderingCallback, ViewChangeListener {

	private static final long serialVersionUID = -6097838884706105696L;
	private Map<String, DoubleField> fieldProviders = new HashMap<String, DoubleField>();
	private List<FeedProvider> feedProviders = new ArrayList<FeedProvider>();

	private static final String X_ROTATION_KEY = "GLOBAL_X_ROTATION";
	private static final String Y_ROTATION_KEY = "GLOBAL_Y_ROTATION";
	private static final String ZOOM_KEY       = "GLOBAL_ZOOM";
	private static final String X_POSITION_KEY = "GLOBAL_X_POSITION";
	private static final String Y_POSITION_KEY = "GLOBAL_Y_POSITION";
	
	private EarthPanel earth;
	
	public EarthView(AbstractComponent component, ViewInfo info) {
		super(component, info);
		
		Map<JComponent, Trajectory> trajectories = new HashMap<JComponent, Trajectory>();
		//trajectories.put(component.getDisplayName(), new TrajectoryProvider());
		
		List<AbstractComponent> potentiallyViewable = new ArrayList<AbstractComponent>();
		potentiallyViewable.add(component);
		potentiallyViewable.addAll(component.getComponents());
		
		for (AbstractComponent candidate : potentiallyViewable) {
			int i = 0;

			TrajectoryProvider tp = new TrajectoryProvider();			
			for (AbstractComponent vector : candidate.getComponents()) {
				for (AbstractComponent element : vector.getComponents()) {
					FeedProvider fp = element.getCapability(FeedProvider.class);
					if (fp != null && i < 6) {
						feedProviders.add(fp);
						fieldProviders.put(fp.getSubscriptionId(), tp.fields[i++]);
					}
				}
			}
			// This restricts view to UserOrbitalComponent for demo purposes. In principle could
			// work for any appropriate set of three data points
			/*
			 * HarleighSatTrack
			 *     UserOrbitalComponent.class.isAssignableFrom(candidate.getClass()) && 
			 */
			if ( i > 3) { // Did we find enough feeds to plot the course of this object?
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
		
		earth = new EarthPanel(trajectories);
		loadSettings();
		add (earth);
		earth.addViewChangeListener(this);
	}

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

	@Override
	public void synchronizeTime(Map<String, List<Map<String, String>>> data,
			long syncTime) {
		updateFromFeed(data);
	}

	@Override
	public Collection<FeedProvider> getVisibleFeedProviders() {
		return feedProviders;
	}

	@Override
	public void render(Map<String, List<Map<String, String>>> data) {
		updateFromFeed(data);	
	}
	
	
	
	@Override
	public void updateMonitoredGUI(PropertyChangeEvent event) {
		loadSettings();
	}





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
	
	public void loadSettings() {
		double xr, yr, px, py, zoom;
		ExtendedProperties vp = getViewProperties();
		String property;
		
		property = vp.getProperty(X_ROTATION_KEY, String.class);
		if (property == null) return;
		xr       = Double.parseDouble(property);
		
		property = vp.getProperty(Y_ROTATION_KEY, String.class);
		if (property == null) return;
		yr       = Double.parseDouble(property);
		
		property = vp.getProperty(X_POSITION_KEY, String.class);
		if (property == null) return;
		px       = Double.parseDouble(property);
		
		property = vp.getProperty(Y_POSITION_KEY, String.class);
		if (property == null) return;
		py       = Double.parseDouble(property);
		
		property = vp.getProperty(ZOOM_KEY, String.class);
		if (property == null) return;
		zoom     = Double.parseDouble(property);

		earth.setView(xr, yr, px, py, zoom);
	}

	
	private class DoubleField {
		private double value;
		public void set(double v) { value = v; }
		public double get () { return value; }
	}
	
	private class TrajectoryProvider implements Trajectory{
		public DoubleField fields[] = new DoubleField[6];
		
		public TrajectoryProvider() {
			for (int i = 0; i < fields.length; i++) fields[i] = new DoubleField();
		}
		
		@Override
		public Vector getPosition() {
			return new Vector(fields[0].get(), fields[1].get(), fields[2].get());
		}
		@Override
		public Vector getVelocity() {
			return new Vector(fields[3].get(), fields[4].get(), fields[5].get());		
		}	
	}
}
