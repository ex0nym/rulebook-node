package io.exonym.utils.storage;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="Attempt")
@XmlType(name = "Attempt", namespace = Namespace.EX) 
public class Attempt {
	
	@XmlElement(name = "TimeOfAttemptUTC", namespace = Namespace.EX)
	private String timeOfAttempt;
	
	@XmlElement(name = "AttemptResult", namespace = Namespace.EX)
	private String resultOfConnect;

	public String getTimeOfAttempt() {
		return timeOfAttempt;
	}

	public void setTimeOfAttempt(String timeOfAttempt) {
		this.timeOfAttempt = timeOfAttempt;
	}

	public String getResultOfConnect() {
		return resultOfConnect;
	}

	public void setResultOfConnect(String resultOfConnect) {
		this.resultOfConnect = resultOfConnect;
	}
	
	

}
