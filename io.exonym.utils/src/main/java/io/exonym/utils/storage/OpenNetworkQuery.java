package io.exonym.utils.storage;

import java.net.URI;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="OpenNetworkQuery")
@XmlType(name = "OpenNetworkQuery", namespace = Namespace.EX) 
public class OpenNetworkQuery {
	
	@XmlElement(name = "QueryUID", namespace = Namespace.EX)
	private URI queryUid;
	
	@XmlElement(name = "Username", namespace = Namespace.EX)
	private String username; 
	
	@XmlElement(name = "IsDelivered", namespace = Namespace.EX)
	private boolean delivered;

	@XmlElement(name = "PresentationTokenUID", namespace = Namespace.EX)
	private URI presentationTokenUid;
	
	@XmlElement(name = "TargetPeer", namespace = Namespace.EX)
	private XPeer targetPeer;
	
	@XmlElement(name = "MinutesBetweenSearches", namespace = Namespace.EX)
	private int minutesBetweenSearches = 1; 

	@XmlElement(name = "Increments", namespace = Namespace.EX)
	private int increment = 0;  

	@XmlElement(name = "OutboundXNodeMsg", namespace = Namespace.EX)
	private XNodeMsg outboundMessage;
	
	@XmlElement(name = "Attempt", namespace = Namespace.EX)
	private ArrayList<Attempt> attempt;
	

	public URI getQueryUid() {
		return queryUid;
	}

	public void setQueryUid(URI queryUid) {
		this.queryUid = queryUid;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public XPeer getTargetPeer() {
		return targetPeer;
	}

	public void setTargetPeer(XPeer targetPeer) {
		this.targetPeer = targetPeer;
	}

	public ArrayList<Attempt> getAttempt() {
		if (attempt==null){
			attempt = new ArrayList<>();
			
		}
		return attempt;
	}

	public void setAttempt(ArrayList<Attempt> attempt) {
		this.attempt = attempt;
	}

	public boolean isDelivered() {
		return delivered;
	}

	public void setDelivered(boolean delivered) {
		this.delivered = delivered;
	}
	
	public static URI generateUid(String context, String targetNym){
		return URI.create("urn:" + context + ":" + targetNym + ":onq");
		
	}

	public int getMinutesBetweenSearches() {
		return minutesBetweenSearches;
	}

	public void setMinutesBetweenSearches(int minutesBetweenSearches) {
		this.minutesBetweenSearches = minutesBetweenSearches;
	}

	public int getIncrement() {
		return increment;
	}

	public void setIncrement(int increment) {
		this.increment = increment;
	}

	public URI getPresentationTokenUid() {
		return presentationTokenUid;
	}

	public void setPresentationTokenUid(URI presentationTokenUid) {
		this.presentationTokenUid = presentationTokenUid;
	}

	public XNodeMsg getOutboundMessage() {
		return outboundMessage;
	}

	public void setOutboundMessage(XNodeMsg outboundMessage) {
		this.outboundMessage = outboundMessage;
	}
	
	public void incrementMinute(){
		this.minutesBetweenSearches++;
		
	}
	public void incrementCounter(){
		this.increment++;
		
	}
}
