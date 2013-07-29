package gov.nasa.arc.mct.scenario.component;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Container for an Decision's model.
 * TODO: Should this class be omitted? Can't DecisionComponent just reference DecisionData instead?
 */
@XmlRootElement()
@XmlAccessorType(XmlAccessType.FIELD)
public class DecisionModelRole {
	
	// This is the model data. 
	// You can choose whether or not your model data will be persisted using setPersistable().
	//
	// In this example, we will persist data by setting persistable to true, and annotating our model role.
	// The View Model Role associated with this component allows a user to modify and save the data. When the user commits the
	// change, MCT persists model data using JAXB.  The XML text written to the MCT database is similar to 
	// <exampleModelRole><data><doubleData>46.91</doubleData><dataDescription>100 free</dataDescription></data></exampleModelRole>
	
	private DecisionData data = new DecisionData();

	public DecisionData getData() {
		return data;
	}
}
