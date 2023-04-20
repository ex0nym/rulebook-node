package io.exonym.utils.storage;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.joda.time.DateTime;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="PeerMessage")
@XmlType(name = "PeerMessage", namespace = Namespace.EX)
public class PeerMessage implements Comparable<PeerMessage>{
	
	@XmlElement(name = "Content", namespace = Namespace.EX)
	private String text;
	
	@XmlElement(name = "Image", namespace = Namespace.EX)
	private byte[] image;
	
	@XmlElement(name = "IsDelivered", namespace = Namespace.EX)
	private boolean delivered; 
	
	@XmlElement(name = "IsRead", namespace = Namespace.EX)
	private boolean read;
	
	@XmlElement(name = "SentUTC", namespace = Namespace.EX)
	private String sentUtc;
	
	@XmlElement(name = "DeliveredUTC", namespace = Namespace.EX)
	private String deliveredUtc;
	
	@XmlElement(name = "ReadUTC", namespace = Namespace.EX)
	private String readUtc;

	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public boolean isDelivered() {
		return delivered;
	}
	public void setDelivered(boolean delivered) {
		this.delivered = delivered;
	}
	public boolean isRead() {
		return read;
	}
	public void setRead(boolean read) {
		this.read = read;
	}
	public String getSentUtc() {
		return sentUtc;
	}
	public void setSentUtc(String sentUtc) {
		this.sentUtc = sentUtc;
	}
	public String getDeliveredUtc() {
		return deliveredUtc;
	}
	public void setDeliveredUtc(String deliveredUtc) {
		this.deliveredUtc = deliveredUtc;
	}
	public String getReadUtc() {
		return readUtc;
	}
	public void setReadUtc(String readUtc) {
		this.readUtc = readUtc;
	}
	@Override
	public int compareTo(PeerMessage m) {
		DateTime thisDt = new DateTime(sentUtc);
		DateTime theirDt = new DateTime(m.sentUtc);
		return thisDt.compareTo(theirDt);
		
	}
}
