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

import jsattrak.objects.SatelliteTleSGP4;
import name.gano.astro.time.Time;


/*
 * This class provides the functionality to track a satellite at a specific time,
 * based on a TLE file.  The satellite can be tracked with latitude/longitude as
 * well as Earth Centered Earth Fixed coordinates.
 * 
 * Note: Maybe we want to move the ECEF to LLA coordinates to the ConvertECEFtoLLA
 *       class.
 * 
 * Note: this class is used in the SatelliteDataProvider: there, feeds are read; these
 * feeds contain TLE data (see CoordinateComponent for more info on feeds). Then this
 * class is used with the TLE data to calculate the location of the satellites.
 */
public class SatTrak {
	/*Units returned from the satellite object:
	 *    getLatitude is in radians and is in the range [-90,90]
	 *    getLongitude is in radians and is in the range [-180,180]
	 *    getAltitude is in meters
	 */
	private SatelliteTleSGP4 prop;
	
	/* This object is used to propagate the satellite to a specific time so all of its
	 * data reflects the data for that specific time. 
	 */
	private Time julianTime;
	
	/*This is the satellite number found in the TLE; this number is Unique0
	 */
	private String SatNum;

	/* These constants are for converting latitude and longitude to Earth Centered
	 * Earth Fixed coordinates.
	 *    WGS 84 Earth radius (in meters)
	 *    WGS 84 first eccentricity squared (no units)
	 */
	private final double SEMI_MAJOR_AXIS = 6378137.0;
	private final double FIRST_ECCENTRICITY_SQUARED = 6.69437999014E-3;
	
	/**
	 * Creates a satellite object with respect to a given TLE. While the name can be
	 * arbitrary, note that lines 1 and 2 of the TLE for your satellite must be complete:
	 *    Example: For the International Space Station, we have
	 *             satName = "ISS (ZARYA)"
	 *             tleLine1= "1 25544U 98067A   13241.39990741  .00008176  00000-0  14800-3 0  4862"
	 *             tleLine2= "2 25544  51.6499 105.5504 0004476  12.3451 332.2743 15.50589082846053"
	 * 
	 * Note that the satellite number (25544 in the above example) is a unique key.
	 * 
	 * @param satName  The name for your satellite.
	 * @param tleLine1 The complete first line of the TLE file associated with your satellite.
	 * @param tleLine2 The complete second line of the TLE file associated with your satellite.
	 */
	public
	SatTrak	(String satName, String tleLine1, String tleLine2) {
		julianTime = new Time();
		try {
			prop = new SatelliteTleSGP4(satName, tleLine1, tleLine2);
		} catch (Exception e) {
			System.out.println("Error Creating SGP4 Satellite");
			e.printStackTrace();
		}
		prop.setShowGroundTrack(false); // if we arn't using the JSatTrak plots midas well turn this off to save CPU time
		
		SatNum = tleLine2.split("\\s+")[1]; //the satellite number is the second token on the  2nd line of the TLE
	}
	
	/**
	 * Updates the satellite object to reflect values with respect to the current time.  
	 */
	public void
	updateSat2CurTime() {
		julianTime.update2CurrentTime();
		prop.propogate2JulDate(julianTime.getJulianDate());
	}
	
	private void setSatTime (long timeInMilli) {
		julianTime.set(timeInMilli);
		prop.propogate2JulDate(julianTime.getJulianDate());
	}
	
	/*precondition:  lat is in radians*/
	private double
	N(double lat){
		return SEMI_MAJOR_AXIS/(1-FIRST_ECCENTRICITY_SQUARED*Math.pow(Math.sin(lat), 2));
	}
	
	/**
	 * This method updates the satellite object to the current system time (in milliseconds) and 
	 * returns the ECEF x-coordinate of the satellite (in km)
	 * 
	 * Use this method if you do not want to be bothered by updating your satellite to the current
	 * time
	 * 
	 * @return: Earth Centered Earth Fixed x-coordinate (in km) at the current system time
	 */
	public double getECEFx() {
		updateSat2CurTime();
		return ((N(prop.getLatitude())+prop.getAltitude())*Math.cos(prop.getLatitude())*Math.cos(prop.getLongitude())/1000);
	}
	
	/**
	 * This method updates the satellite object to the current system time (in milliseconds) and 
	 * returns the ECEF y-coordinate of the satellite (in km)
	 * 
	 * Use this method if you do not want to be bothered by updating your satellite to the current
	 * time
	 * 
	 * @return: Earth Centered Earth Fixed y-coordinate (in km) at the current system time
	 */
	public double getECEFy() {
		updateSat2CurTime();
		return ((N(prop.getLatitude())+prop.getAltitude())*Math.cos(prop.getLatitude())*Math.sin(prop.getLongitude())/1000);
	}
	
	/**
	 * This method updates the satellite object to the current system time (in milliseconds) and 
	 * returns the ECEF z-coordinate of the satellite (in km)
	 * 
	 * Use this method if you do not want to be bothered by updating your satellite to the current
	 * time
	 * 
	 * @return: Earth Centered Earth Fixed z-coordinate (in km) at the current system time
	 */
	public double getECEFz() {
		updateSat2CurTime();
		return ((N(prop.getLatitude())*(1-FIRST_ECCENTRICITY_SQUARED)+prop.getAltitude())*Math.sin(prop.getLatitude())/1000);
	}
	
	/**
	 * Use this method to access the latitude position of the satellite object at the current system time
	 * 
	 * @return latitude of satellite (in radians) at the current system time in the range [-90,90]
	 */
	public double getLatitude() {
		updateSat2CurTime();
		return prop.getLatitude();
	}
	
	/**
	 * Use this method to access the longitude position of the satellite object at the current system time
	 * 
	 * @return longitude of satellite (in radians and in the range [-180,180]) at the current system time 
	 */
	public double getLongitude() {
		updateSat2CurTime();
		return prop.getLongitude();
	}
	
	/**
	 * Use this method to access the altitude of the satellite object at the current system time
	 * 
	 * @return altitude of satellite (in km) at the current system time
	 */
	public double getAltitude() {
		updateSat2CurTime();
		return prop.getAltitude();
	}
	
	/**
	 * Use this method to find the ECEF (Earth Centered Earth Fixed) x coordinate (in km) of the satellite
	 * object at a specified time (in milliseconds)
	 * 
	 * @param timeInMilli the time (in milliseconds) when you want the satellites position
	 * @return the ECEF x coordinate location (in km) of the satellite object at the specified time
	 */
	public double getECEFx(long timeInMilli) {
		setSatTime(timeInMilli);
		return ((N(prop.getLatitude())+prop.getAltitude())*Math.cos(prop.getLatitude())*Math.cos(prop.getLongitude())/1000);
	}
	
	/**
	 * Use this method to find the ECEF (Earth Centered Earth Fixed) y coordinate of the satellite
	 * object at a specified time (in milliseconds)
	 * 
	 * @param timeInMilli the time (in milliseconds) when you want the satellites position
	 * @return the ECEF y coordinate location (in km) of the satellite object at the specified time
	 */
	public double getECEFy(long timeInMilli) {
		setSatTime(timeInMilli);
		return ((N(prop.getLatitude())+prop.getAltitude())*Math.cos(prop.getLatitude())*Math.sin(prop.getLongitude())/1000);
	}
	
	/**
	 * Use this method to find the ECEF (Earth Centered Earth Fixed) z coordinate of the satellite
	 * object at a specified time (in milliseconds)
	 * 
	 * @param timeInMilli the time (in milliseconds) when you want the satellites position
	 * @return the ECEF z coordinate location (in km) of the satellite object at the specified time
	 */
	public double getECEFz(long timeInMilli) {
		setSatTime(timeInMilli);
		return ((N(prop.getLatitude())*(1-FIRST_ECCENTRICITY_SQUARED)+prop.getAltitude())*Math.sin(prop.getLatitude())/1000);
	}
	
	/**
	 * Use this method to find the latitude of the satellite object at a specified time (in milliseconds)
	 * 
	 * @param timeInMilli the time (in milliseconds) when you want the satellites position
	 * @return the latitude (in radians) of the satellite object at the specified time
	 */
	public double getLatitude(long timeInMilli) {
		setSatTime(timeInMilli);
		return prop.getLatitude();
	}
	
	/**
	 * Use this method to find the longitude of the satellite object at a specified time (in milliseconds)
	 * 
	 * @param timeInMilli the time (in milliseconds) when you want the satellites position
	 * @return the longitude (in radians) of the satellite object at the specified time
	 */
	public double getLongitude(long timeInMilli) {
		setSatTime(timeInMilli);
		return prop.getLongitude();
	}
	
	/**
	 * Use this method to find the altitude (in meters) at a specified time (in milliseconds)
	 * 
	 * @param timeInMilli the time (in milliseconds) when you want the satellites position
	 * @return altitude (in meters) of the satellite object at the specified time
	 */
	public double getAltitude(long timeInMilli) {
		setSatTime(timeInMilli);
		return prop.getAltitude();
	}
	
	
	
	/**
	 * This method does nothing but return the name of the satellite object.  The name of the satellite was set
	 * at the constructor for this class
	 * @return the name of the satellite object 
	 */
	public String getSatName() {
		return prop.getName();
	}
	
	/**
	 * This method returns the TEME (true equator, mean equinox) of the satellite object at the current system time
	 * in meters per second
	 * @return An array { x, y, z } each in units of meters per second (m/s)
	 */
	public double[] getTEMEvelocity() {
		updateSat2CurTime();
		return prop.getTEMEVelocity();
	}
	
	/**
	 * This method returns the TEME (true equator, mean equinox) of the satellite object a specified time in meters
	 * per second
	 * @param timeInMilli the time (in milliseconds) when you want the satellites velocity
	 * @return An array { x, y, z } each in units of meters per second (m/s)
	 */
	public double[] getTEMEvelocity(long timeInMilli) {
		setSatTime(timeInMilli);
		return prop.getTEMEVelocity();
	}
	
	public String getSatNumber() {
		return this.SatNum;
	}
	
	
	/*--------Testing---------
	 * Here is a little main to test the features of SatTrak and to verify that it is getting correct
	 * data.  the ISS TLE file is located at http://celestrak.com/NORAD/elements/stations.txt
	 * be sure to have the most recent TLE and then pop onto http://www.isstracker.com/ to see if the
	 * below code is tracking the ISS properly
	 */
	public static void main(String[] args) {
		String name = "ISS (ZARYA)             ";
		String tleLine1 = "1 25544U 98067A   13241.39990741  .00008176  00000-0  14800-3 0  4862";
		String tleLine2 = "2 25544  51.6499 105.5504 0004476  12.3451 332.2743 15.50589082846053";
		
		SatTrak sat = new SatTrak(name, tleLine1, tleLine2);
		
		for( int i=0; i< 1000; i=i+1) {
			 //sat.updateSat2CurTime();
			
			 System.out.println( "Name: " + sat.getSatName() + 
 					"\nLatitude[Deg]: "    + sat.getLatitude()*180.0/Math.PI +
 		            "\nLongitude[Deg]: " + sat.getLongitude()*180.0/Math.PI +
 		            "\nAltitude [m]: "   + sat.getAltitude() +
 		            "\nECEF x[m]:"       + sat.getECEFx() +
 		            "\nECEF y[m]:"       + sat.getECEFy() +
 		            "\nECEF z[m]:"       + sat.getECEFz() +
 		            "\n");
			 
			 //sleep for one second
			 try {
			 	    Thread.sleep(1000);
			 } catch(InterruptedException ex) {
			 	    Thread.currentThread().interrupt();
			 }
			
		}
	}
}
