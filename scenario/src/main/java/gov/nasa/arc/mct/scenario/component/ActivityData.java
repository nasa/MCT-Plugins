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
package gov.nasa.arc.mct.scenario.component;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Underlying model data for Activity components.
 * These includes costs (power/data), time/duration, and type.
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ActivityData {

	private double power;
	private double comm;
	private String type;
	private String notes;
	private Date startDate;
	private Date endDate;

	public double getPower() {
		return power;
	}

	public void setPower(double power) {
		this.power = power;
	}

	public double getComm() {
		return comm;
	}

	public void setComm(double comm) {
		this.comm = comm;
	}
	
	public long getDurationTime()
	{
		return endDate.getTime() - startDate.getTime();
	}
	
	public void setDurationTime(long duration)
	{		
		Date endDate = new Date(this.startDate.getTime() + duration);
		this.endDate = endDate;
	}
	
	public String getActivityType() {
		return type != null ? type : ""; // Never return null
	}
	
	public void setActivityType(String type) {
		this.type = type;
	}
	
	public String getNotes() {
		return notes != null ? notes : ""; // Never return null
	}
	
	public void setNotes(String notes) {
		this.notes = notes;
	}

	public Date getStartTime() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndTime() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	
}
