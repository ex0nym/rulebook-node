package io.exonym.utils.storage;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import eu.abc4trust.xml.FriendlyDescription;
import io.exonym.lite.pojo.Namespace;
import io.exonym.utils.storage.ActionParameters.STATE;


@XmlType(name = "ActionHistory", namespace = Namespace.EX, 
	propOrder = {"utcDateTimeOfAction", "previousState", "description", "nym"})
public class ActionParametersHistoryItem {
	
	@XmlElement(name = "TimeOfActionUTC", namespace = Namespace.EX)
	private String utcDateTimeOfAction;
	
	@XmlElement(name = "PreviousState", namespace = Namespace.EX)
	private STATE previousState;
	
	@XmlElement(name = "Description", namespace = Namespace.EX)
	private FriendlyDescription description;
	
	@XmlElement(name = "Pseudonym", namespace = Namespace.EX)
	private byte[] nym;

	public String getUtcDateTimeOfAction() {
		return utcDateTimeOfAction;
	}

	public void setUtcDateTimeOfAction(String utcDateTimeOfAction) {
		this.utcDateTimeOfAction = utcDateTimeOfAction;
	}

	public STATE getPreviousState() {
		return previousState;
	}

	public void setPreviousState(STATE previousState) {
		this.previousState = previousState;
	}

	public FriendlyDescription getDescription() {
		return description;
	}

	public void setDescription(FriendlyDescription description) {
		this.description = description;
		
	}

	public byte[] getNym() {
		return nym;
		
	}

	public void setNym(byte[] nym) {
		this.nym = nym;
		
	}
}
