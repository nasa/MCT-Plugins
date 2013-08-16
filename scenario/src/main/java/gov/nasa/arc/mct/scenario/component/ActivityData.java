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
