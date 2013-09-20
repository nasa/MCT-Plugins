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

import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.components.FeedProvider;
import gov.nasa.arc.mct.components.JAXBModelStatePersistence;
import gov.nasa.arc.mct.components.ModelStatePersistence;
import gov.nasa.arc.mct.services.activity.TimeService;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/*
 * This class takes the data represented in CoordinateModel and stores it into feeds that can
 * be read by DataProviders; SatelliteDataProvider takes these feeds and performs operations
 * on them.
 * 
 *  This class takes the TLE data found in CoordinateModel and stores it into a feed, and then
 *  SatelliteDataProvider takes that TLE data found in the feed and performs satellite-tracking.
 */
public class CoordinateComponent extends AbstractComponent implements FeedProvider {
	
	/**
	 * <code>FEED_KEY_ID</code> is the identification code which says that this feed is for a satellite orbit 
	 */
	public static final String FEED_KEY_ID = "SatOrbit";
	
	/**
	 * <code>FEED_SEPERATOR</code> is the unique single character that separates the fields of the FeedID
	 */
	public static String FEED_SEPERATOR = "=";
	
	/*
	 * model contains data of which we want to turn into a feed.
	 */
	private AtomicReference<CoordinateModel> model = new AtomicReference<CoordinateModel>();
	
	
	/*
	 * the time service for the satellite objects is just the current system time
	 */
	private static final TimeService TIME_SERVICE = new TimeService() {

		@Override
		public long getCurrentTime() {
			return System.currentTimeMillis();
		}
		
	};	
	
	public void setModel(CoordinateModel m) {
		model.set(m);
	}

	protected <T> T handleGetCapability(Class<T> capability) {
		if (ModelStatePersistence.class.isAssignableFrom(capability)) {
		    JAXBModelStatePersistence<CoordinateModel> persistence = new JAXBModelStatePersistence<CoordinateModel>() {

				@Override
				protected CoordinateModel getStateToPersist() {
					return model.get();
				}

				@Override
				protected void setPersistentState(CoordinateModel modelState) {
					model.set(modelState);
				}

				@Override
				protected Class<CoordinateModel> getJAXBClass() {
					return CoordinateModel.class;
				}
		        
			};
			
			return capability.cast(persistence);
		}
		
		if (capability.isAssignableFrom(FeedProvider.class)) {
			return capability.cast(this);
		}
		
		return null;
	}
	
	/*
	 * This method returns the feedID; the data provider will see this FeedID
	 * See SatelliteDataProvider
	 */
	@Override
	public String getSubscriptionId() {
		return  FEED_KEY_ID + ":" + model.get().getTLE().getSatName() +FEED_SEPERATOR
		                    + model.get().getTLE().getLine1() + FEED_SEPERATOR
		                    + model.get().getTLE().getLine2() + FEED_SEPERATOR
		                    + model.get().getParameterKey();
	}
	
	
	@Override
	public TimeService getTimeService() {
		return TIME_SERVICE;
	}

	@Override
	public String getLegendText() {
		return getDisplayName();
	}

	@Override
	public int getMaximumSampleRate() {
		return 1;
	}

	@Override
	public FeedType getFeedType() {
		return FeedType.FLOATING_POINT;
	}

	@Override
	public String getCanonicalName() {
		return getDisplayName();
	}

	@Override
	public RenderingInfo getRenderingInfo(Map<String, String> data) {
		String riAsString = data.get(FeedProvider.NORMALIZED_RENDERING_INFO);
		RenderingInfo ri = null;  
		ri = FeedProvider.RenderingInfo.valueOf(riAsString);   
		return ri;
	}

	@Override
	public long getValidDataExtent() {
		return System.currentTimeMillis();
	}

	@Override
	public boolean isPrediction() {
		return false;
	}
	
	@Override
	public boolean isLeaf() {
		return true;
	}

	@Override
	public boolean isNonCODDataBuffer() {
		// TODO Auto-generated method stub
		return false;
	}

}
