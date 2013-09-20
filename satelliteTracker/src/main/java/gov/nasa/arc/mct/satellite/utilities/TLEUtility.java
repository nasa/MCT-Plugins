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
package gov.nasa.arc.mct.satellite.utilities;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jsattrak.utilities.TLE;

/*
 * A small class to handle the accessing-of TLE files.  This class does not store
 * the TLE files in memory, rather: you give this class a Celestrak name and
 * it downloads a fresh copy right off of Celestrak's website.
 * 
 * This class is used in the satellite wizard; its purpose: to download the TLEs
 * that the user has reqested.  See the only public method, 'getTLEs'
 */
public class TLEUtility {	
	
	//Used to access TLEs on Celestrak's website
	private static final String urlTLELocation = "http://celestrak.com/NORAD/elements/";
	
	/*
	 * Given a Celestrak TLE group-name (like 'Space Stations' for example) return the
	 * file where said TLEs are located
	 */
	@SuppressWarnings("serial")
	private static final Map<String, String> FileLookup = new HashMap<String,String>() {
		{
			//Special-Interest Satellites
			put("Last 30 Days' Launches", "tle-new.txt");
			put("Space Stations", "stations.txt");
			put("100 (or so) Brightest", "visual.txt");
			put("FENGYUN 1C Debris","1999-025.txt");
			put("IRIDIUM 33 Debris", "iridium-33-debris.txt");
			put("COSMOS 2251 Debris", "cosmos-2251-debris.txt");
			put("BREEZE-M R/B Breakup (2012-044C)", "2012-044.txt");
		
			//Weather & Earth Resources Satellites
			put( "Weather", "weather.txt");
			put("NOAA", "noaa.txt");
			put("GOES", "goes.txt");
			put("Earth Resources", "resource.txt");
			put("Search & Rescue (SARSAT)", "sarsat.txt");
			put("Disaster Monitoring", "dmc.txt");
			put("Tracking and Data Relay Satellite System (TDRSS)", "tdrss.txt");

			//Communications Satellites
			put("Geostationary", "geo.txt");
			put("Intelsat", "intelsat.txt");
			put("Gorizont", "gorizont.txt");
			put("Raduga", "raduga.txt");
			put("Molniya", "molniya.txt");
			put("Iridium", "iridium.txt");
			put("Orbcomm", "orbcomm.txt");
			put("Globalstar", "globalstar.txt");
			put("Amateur Radio", "amateur.txt");
			put("Experimental","x-comm.txt");
			put("Other", "other-comm.txt");
			
			//Navigation Satellites
			put("GPS Operational","gps-ops.txt");
			put("Glonass Operational","glo-ops.txt");
			put("Galileo","galileo.txt");
			put("Beidou","beidou.txt");
			put("Satellite-Based Augmentation System (WAAS/EGNOS/MSAS)","sbas.txt");
			put("Navy Navigation Satellite System (NNSS)","nnss.txt");
			put("Russian LEO Navigation","musson.txt");
			
			//Scientific Satellites
			put("Space & Earth Science","science.txt");
			put("Geodetic","geodetic.txt");
			put("Engineering","engineering.txt");
			put("Education","education.txt");

			//Miscellaneous Satellites
			put("Miscellaneous Military","military.txt");
			put("Radar Calibration","radar.txt");
			put("CubeSats","cubesat.txt");
			put("Other","other.txt");
		}
	};
	
	/*
	 * Given the full path to a file containing TLE data, e.g.: "stations.txt", this method returns
	 * a list of strings, each string is a line of the TLE file
	 */
	private  List<String>
	readFile(URL tleLoc)
	{
	    List<String> records = new ArrayList<String>();
	  try
	  {
		  BufferedReader reader = new BufferedReader(new InputStreamReader(tleLoc.openStream()));
	      String line;
	    while ((line = reader.readLine()) != null)
	    {
	      records.add(line);
	    }
	    reader.close();
	    return records;
	  }
	  catch (Exception e)
	  {
	    System.err.format("Exception occurred trying to read '%s'.", tleLoc.toString());
	    e.printStackTrace();
	    return null;
	  }
	}//--end readFile
	
	/**
	 * Grab all the TLEs from a given TLE-file (from Celestrak) and return them as a List of TLEs
	 * 
	 * @param filename file containing TLE data
	 * @return list of TLE objects in the order given in the given TLE file
	 */
	private List<TLE>
	grab_tles(URL tleLoc){
		
		List<String> raw_tle_data = readFile(tleLoc);
		/*
		 * all tle data are in three-line increments, so the size of raw_tle_data is 3 times the number
		 * of tle's in the file
		 * 
		 * Hence the number of tle's in the file is the number of elements in raw_tle_data divided by three
		 */
		int num_lines = raw_tle_data.size();
		List<TLE> tle_recs = new ArrayList<TLE>();
		//grab each tle:  lines 0 1 2 is the first tle, then lines 3 4 5 is the second tle, ... etc 
		for(int i = 0; i<num_lines; i =i+3) {
			TLE data = new TLE(raw_tle_data.get(i).trim(), raw_tle_data.get(i+1), raw_tle_data.get(i+2)); //why trim()? Well, we need to trim the trailing whitespace from the satellite name
			tle_recs.add(data);
		}//end for loop
		
		return tle_recs; 	//list of tles
	}
	
	/*
	 * Given a Celestrak satellite category, return the URL where the said TLE data is located; for example,
	 * if we want the "FENGYUN 1C Debris" TLE data, then we return "http://celestrak.com/NORAD/elements/1999-025.txt"
	 */
	private
	String
	getTLEurl(String RequestedTLEData){
		String fileName = FileLookup.get(RequestedTLEData);
		if(fileName==null)
			try {
				Exception up = new Exception("Celestrak Satellite Category \""+ RequestedTLEData +"\" Not Found."); 
				throw up;
			} catch (Exception e) {
				e.printStackTrace();
			}
		else
			return urlTLELocation + fileName;
		return "";
	}
	
	
	
	/**
	 * 
	 * @param CelestrakCategory The Satellite Category as defined from 'http://celestrak.com/NORAD/elements/'
	 * @return A list of TLE objects associated with the Celestrak satellite category (in order as they appear
	 *         on Celestrak
	 */
	public
	List<TLE>
	getTLEs(String CelestrakCategory) {
		
		String tleLoc = getTLEurl(CelestrakCategory);
		
		try {
			URL urlTLE = new URL(tleLoc);
			List<TLE> tles =  grab_tles(urlTLE);
			return tles;
		} catch (MalformedURLException e) {
			return null;
		}
		
	}
	
	
	/*
	 * Simple main for testing purposes :)
	 */
	public static void main(String[] args) {
		TLEUtility tUtil = new TLEUtility();
		List<TLE> myTLEs = tUtil.getTLEs("Space Stations");
		System.out.println("Size of TLE list: " + myTLEs.size());
		System.out.println("Done");
	}
	
}