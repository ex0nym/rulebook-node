package io.exonym.utils.storage;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlType(name = "HistoryItem", namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
// , propOrder={"newState", "previousState", "dateTimeUtc"})
public class HistoryItem {
	
	@XmlElement(name = "DateTimeUtc", namespace = Namespace.EX)
	private String dateTimeUtc;
	@XmlElement(name = "NewState", namespace = Namespace.EX)
	private String newState;
	@XmlElement(name = "OldState", namespace = Namespace.EX)
	private String previousState;

	public HistoryItem() {}

	public String getDateTimeUtc() {
		return dateTimeUtc;
	}

	public void setDateTimeUtc(String dateTimeUtc) {
		this.dateTimeUtc = dateTimeUtc;
	}

	public String getNewState() {
		return newState;
	}

	public void setNewState(String newState) {
		this.newState = newState;
	}

	public String getPreviousState() {
		return previousState;
	}

	public void setPreviousState(String previousState) {
		this.previousState = previousState;
	}
}
