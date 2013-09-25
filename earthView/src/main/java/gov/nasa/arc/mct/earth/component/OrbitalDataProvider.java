package gov.nasa.arc.mct.earth.component;

import gov.nasa.arc.mct.api.feed.DataProvider;
import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.components.FeedProvider;
import gov.nasa.arc.mct.components.FeedProvider.RenderingInfo;
import gov.nasa.arc.mct.earth.Trajectory;
import gov.nasa.arc.mct.earth.Vector;
import gov.nasa.arc.mct.platform.spi.PlatformAccess;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class OrbitalDataProvider implements DataProvider {
	private static final double SPEEDUP          = 25.0;
//	private static final Vector INITIAL_POSITION = new Vector( 6141.37,  -159.60, -2857.83);
//	private static final Vector INITIAL_VELOCITY = new Vector( 2212.64, -5313.39,  5073.58).multiply(0.001);
//
//	public  static final long   INITIAL_TIME     = System.currentTimeMillis(); //1338422590000l; //5/31 00:03:10 GMT
	
	//private static final ArrayList<Vector> positions = new ArrayList<Vector>();
	
	private static final Map<String, OrbitalModel>     models       = new HashMap<String, OrbitalModel>();
	private static final Map<String, List<Trajectory>> trajectories = new HashMap<String, List<Trajectory>>();   
	private static final Map<String, Long>             initialTime  = new HashMap<String, Long>();
	
	public static void register(String name, OrbitalModel model) {
		models.put(name, model);
	}
	
	public Trajectory getTrajectory(String name, long timestamp) {		
		if (!models.containsKey(name)) findAndRegister(name);
		
		return getIndexedTrajectory(name, (int) (timestamp - models.get(name).getInitialTime()) / 1000) ;
	}
	
	private void findAndRegister(String name) {
		AbstractComponent comp = PlatformAccess.getPlatform().getComponentRegistry().getComponent(name);
		if (comp instanceof UserOrbitalComponent) {
			OrbitalModel m = ((UserOrbitalComponent) comp).getModel();
			models.put(name, m);
			initialTime.put(name, m.getInitialTime());
		}
	}
	
	private Trajectory getIndexedTrajectory(String name, int index) {
		if (!trajectories.containsKey(name)) trajectories.put(name, new ArrayList<Trajectory>());
		if (index < 0) index = 0;
		
		List<Trajectory> history = trajectories.get(name);
		
		while (history.size() <= index) {
			if (history.isEmpty()) history.add( models.get(name).getInitialTrajectory() );
			
			Trajectory t = history.get(history.size() - 1);
			final Vector position = t.getPosition().add( t.getVelocity().add(gravity(t.getPosition()).multiply(SPEEDUP)).multiply(SPEEDUP) );
			final Vector velocity = position.add(t.getPosition().multiply(-1.0)).multiply(1.0 / SPEEDUP);
			history.add(new Trajectory() {
				@Override
				public Vector getPosition() {
					return position;
				}
				@Override
				public Vector getVelocity() {
					return velocity;
				}			
			});
		}
		
		return history.get(index);
	}
	
//	static {		
//		Vector position = INITIAL_POSITION;
//		Vector velocity  = INITIAL_VELOCITY;
//		for (int i = 0; i < 60*60*24*30; i++) {
//			positions.add(position);
//			position = position.add(velocity);
//			velocity = velocity.add(gravity(position));
//		}
//	}

	
	private static Vector gravity (Vector v) {
		return v.normal().multiply(-0.0084);
	}
	
//	private static Vector drift (Vector v1, Vector v2) {
//		return new Vector (v2.getZ(), 0.0, v2.getX()).normal().multiply(0.00008);
//	}
	
	@Override
	public Map<String, List<Map<String, String>>> getData(Set<String> feedIDs,
			TimeUnit timeUnit, long startTime, long endTime) {
		Map<String, List<Map<String, String>>> out = new HashMap<String, List<Map<String, String>>>();
		
		for (Entry<String, SortedMap<Long, Map<String, String>>> entry :
			getData(feedIDs, startTime, endTime, timeUnit).entrySet()) {
			
			List<Map<String, String>> list = new ArrayList<Map<String, String>>();
			list.addAll(entry.getValue().values());

			out.put(entry.getKey(), list);
		}
		
		return out;
	}

	@Override
	public Map<String, SortedMap<Long, Map<String, String>>> getData(
			Set<String> feedIDs, long startTime, long endTime, TimeUnit timeUnit) {
		Map<String, SortedMap<Long, Map<String, String>>> out = new HashMap<String, SortedMap<Long, Map<String, String>>>();
		
		for (String feedID : feedIDs) {
			if (feedID.startsWith("orbit:") && feedID.contains("/")) {
				int paramIndex = feedID.lastIndexOf("/");
				String params = feedID.substring(paramIndex);
				String name   = feedID.substring(6, paramIndex);
				boolean velocity = params.contains("v");
				SortedMap<Long, Map<String, String>> data = new TreeMap<Long, Map<String, String>>();
				for (long time = startTime; time < endTime; time += 1000) {
					double dataValue = 0.0;
					//-----start concern
					Trajectory t = this.getTrajectory(name, time);
					//Vector  position = positions.get(index);
					Vector position = velocity ? t.getVelocity() : t.getPosition();
					//if (velocity) position = position.add(positions.get(index-1).multiply(-1.0));
					if (params.contains("x")) dataValue = position.getX();
					if (params.contains("y")) dataValue = position.getY();
					if (params.contains("z")) dataValue = position.getZ();
					//------end concern 
					
					Map <String, String> dataItem = new HashMap<String, String> ();
					RenderingInfo ri = new RenderingInfo(Double.toString(dataValue), Color.ORANGE, " ", Color.ORANGE, true);
					dataItem.put(FeedProvider.NORMALIZED_RENDERING_INFO, ri.toString());
					dataItem.put(FeedProvider.NORMALIZED_TIME_KEY,  Long.toString(time));
					dataItem.put(FeedProvider.NORMALIZED_VALUE_KEY, Double.toString(dataValue));
					dataItem.put(FeedProvider.NORMALIZED_TELEMETRY_STATUS_CLASS_KEY, "1");
					data.put(Long.valueOf(time), dataItem);
				}
				if (!data.isEmpty()) {
					out.put(feedID, data);
				}
			}
		}
		return out;
	}

	@Override
	public boolean isFullyWithinTimeSpan(String feedID, long startTime,
			TimeUnit timeUnit) {
		return true;
	}

	@Override
	public LOS getLOS() {
		return LOS.medium;
	}
	

}
