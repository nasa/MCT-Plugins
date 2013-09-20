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
package gov.nasa.arc.mct.satellite.component;

import gov.nasa.arc.mct.api.feed.DataProvider;
import gov.nasa.arc.mct.components.FeedProvider;
import gov.nasa.arc.mct.components.FeedProvider.RenderingInfo;
import gov.nasa.arc.mct.satellite.utilities.SatTrak;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/*
 * This class takes-in data-feeds and then grabs the feed associated with a satellite coordinate component (it does
 * this by looking for the unique identifier that CoordinateComponent placed into its feeds).  With the feeds, this
 * class constructs the satellite data.
 * 
 *   Note: The feeds contain TLE data, this class grabs that data and calculates the satellite data based on the TLEs
 *   
 *   How does MCT know that this class is a dataProvider? Well, services.xml (found in OSGI-INF) tells MCT that this
 *   class reads feeds.
 */
public class SatelliteDataProvider implements DataProvider {
	

	//for the purpose of not re-creating already created satellites in the getData method
	private static final ThreadLocal<Map<String, SatTrak>> unique = new ThreadLocal<Map<String,SatTrak>>() {
            @Override protected Map<String, SatTrak> initialValue() {
                return new HashMap<String, SatTrak>();
        }
    };
	
	/* 
	 * (non-Javadoc)
	 * @see gov.nasa.arc.mct.api.feed.DataProvider#isFullyWithinTimeSpan(java.lang.String, long, java.util.concurrent.TimeUnit)
	 * 
	 * CoordinateComponent is the class making the FeedID
	 */
	@Override
	public boolean isFullyWithinTimeSpan(String feedID, long startTime,
			TimeUnit timeUnit) {
		if( feedID.startsWith(CoordinateComponent.FEED_KEY_ID) && feedID.contains(CoordinateComponent.FEED_SEPERATOR))
			return true;
		else
			return false;
	}

	/*
	 * This Satellite data provider can provide data fast 
	 */
	@Override
	public LOS getLOS() {
		return LOS.fast;
	}
	
/*
 * This method reads the feeds comming off from CoordinateComponent.java (which is located in the package
 * gov.nasa.arc.mct.satellite.component)
 * 
 * Notes: *	 	pieces[0] satellite name
 *  			pieces[1] TLE line 1
 *  			pieces[2] TLE line 2
 *  			pieces[3] position or velocity, of x y or z component
 *  
 *        *		Do not change the value of the feedID, for doing so will confuse MCT; for example,  notice I used
 *         		String tleFeed = = feedID.substring(feedID.indexOf(":")+1), that is, a new variable rather than
 *         		writing over feedID
 */
	@Override
	public Map<String, SortedMap<Long, Map<String, String>>> getData(
			Set<String> feedIDs, long startTime, long endTime, TimeUnit timeUnit) {
		
		Map<String, SatTrak> createdSats = unique.get();
		
		Map<String, SortedMap<Long, Map<String, String>>> out = new HashMap<String, SortedMap<Long, Map<String, String>>>();

		for (String feedID : feedIDs) {
			if (feedID.startsWith(CoordinateComponent.FEED_KEY_ID) && feedID.contains(CoordinateComponent.FEED_SEPERATOR)) {
				
				String tleFeed = feedID.substring(feedID.indexOf(":")+1);//+1 so we do not include ':'
				String[] pieces = tleFeed.split(CoordinateComponent.FEED_SEPERATOR);//see notes above method for 'pieces' array
				boolean isVelocity = pieces[3].contains("v");
				SortedMap<Long, Map<String, String>> data = new TreeMap<Long, Map<String, String>>();
				
				for (long time = startTime; time < endTime; time += 1000) {
					
					double dataValue = 0.0;
					
					//pieces[2] is used as a key to the satellites in the hashmap
					String SatNum = pieces[2].split("\\s+")[1]; //the satellite number is the second token of the second line of the TLE; it is Unique
					
					
					//determine whether if we have already created the satellite
					SatTrak sat;
					if(createdSats.containsKey(SatNum)) {
						sat = createdSats.get(SatNum);
					}
					else { //we have not created this satellite yet
						sat = new SatTrak(pieces[0], pieces[1], pieces[2]);
						createdSats.put(SatNum, sat);	//note that the satellite number is unique
					}
			
					
					if( isVelocity) {
						double[] TEMEvel = sat.getTEMEvelocity(time);
						if (pieces[3].contains("x"))
							dataValue = TEMEvel[0];
						else if (pieces[3].contains("y"))
							dataValue = TEMEvel[1];
						else	// here we have (pieces[3].contains("z"))
							dataValue = TEMEvel[2];
					}
					else {
						if (pieces[3].contains("x"))
							dataValue = sat.getECEFx(time);
						else if (pieces[3].contains("y"))
							dataValue = sat.getECEFy(time);
						else	// (pieces[3].contains("z"))
							dataValue = sat.getECEFz(time);
					}
					
					//Now we pack our data so a View can use the data to display it to the user
					//as an example MercatorView takes this data and draws the satellite positions
					//on a Mercator graph
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
	public Map<String, List<Map<String, String>>> getData(Set<String> feedIDs,
			TimeUnit timeUnit, long startTime, long endTime) {
		// TODO Auto-generated method stub
		return null;
	}
	
}