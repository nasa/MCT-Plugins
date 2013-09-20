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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import jsattrak.utilities.TLE;

/*
 * This class stores the data of a coordinate point; CoordinateComponent takes the data in this model
 * and places it into a feed.  So: this class determines what can go into a feed.
 */
@XmlRootElement()
@XmlAccessorType(XmlAccessType.FIELD)
public class CoordinateModel {
	
	private static final String AXIS_KEY[] = {"x", "y", "z"};
	
	private boolean velocity = false;	//represents whether this coordinate is associated with a velocity component, or a position component
	private int     axis = 0;			//to determine if this is in the x, y, or z directoion.
	private String  parent = "";
	
	/*
	 * Note: because of JABX, we must store the TLE data as individual strings and not as a TLE object
	 */
	private String tleSatName;
	private String tleLine1;
	private String tleLine2;
	
	public CoordinateModel() {
		
	}
	
	public CoordinateModel(int axis, boolean velocity, String parent) {
		this.axis     = axis;
		this.velocity = velocity;
		this.parent   = parent;
	}
	
	public CoordinateModel(int axis, boolean velocity, String parent, TLE current) {
		this.axis     = axis;
		this.velocity = velocity;
		this.parent   = parent;

		this.tleSatName=current.getSatName();
		this.tleLine1=current.getLine1();
		this.tleLine2=current.getLine2();
	}
	
	//whether this coordinate represents position or velocity
	public String getParameterKey() {
		return (velocity ? "v" : "p") + AXIS_KEY[axis % AXIS_KEY.length];
	}
	
	public String getParent() {
		return parent;
	}
	
	//turns the individual strings into a TLE object; so we do not have to work with individual strings
	public TLE getTLE(){
		return (new TLE(this.tleSatName, this.tleLine1, this.tleLine2));
	}
	
}
