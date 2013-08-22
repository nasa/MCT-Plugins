package gov.nasa.arc.mct.scenario.component;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Underlying data (start and end time) for decision components
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class DecisionData {

	private Date startDate;
	private Date endDate;
	private String notes;

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
	
	public String getNotes() {
		return notes != null ? notes : ""; // Never return null
	}
	
	public void setNotes(String notes) {
		this.notes = notes;
	}
}
