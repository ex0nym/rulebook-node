package io.exonym.utils.storage;

import io.exonym.lite.pojo.Namespace;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URI;
import java.util.LinkedList;

@XmlType(name="NodeInformation", namespace = Namespace.EX)  
/* 
 * propOrder={"nodeName", "nodeUrl", "failOverNodeUrl", "nodeRootUid", 
				"sourceUrl", "failOverSourceUrl", "sourceUid", 
				"lastUpdateReceived", "issuerParameterUids"}
 */
public class NodeInformation {

	private String nodeName;
	private URI staticNodeUrl0;
	private URI staticLeadUrl0;
	private URI nodeUid;
	private URI rulebookNodeUrl;
	private URI broadcastAddress;
	private URI leadUid;
	private String region;
	private String lastUpdateReceived;
	private LinkedList<URI> issuerParameterUids = null;

	@XmlElement(name = "RulebookNodeURL", namespace = Namespace.EX)
	public URI getRulebookNodeUrl() {
		return rulebookNodeUrl;
	}

	public void setRulebookNodeUrl(URI rulebookNodeUrl) {
		this.rulebookNodeUrl = rulebookNodeUrl;
	}

	@XmlElement(name = "NodeName", namespace = Namespace.EX)
	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	@XmlElement(name = "StaticNodeURL0", namespace = Namespace.EX)
	public URI getStaticNodeUrl0() {
		return staticNodeUrl0;
	}

	public void setStaticNodeUrl0(URI staticNodeUrl0) {
		this.staticNodeUrl0 = staticNodeUrl0;
	}

	@XmlElement(name = "BroadcastAddress", namespace = Namespace.EX)
	public URI getBroadcastAddress() {
		return broadcastAddress;
	}

	public void setBroadcastAddress(URI broadcastAddress) {
		this.broadcastAddress = broadcastAddress;
	}

	@XmlElement(name = "NodeUID", namespace = Namespace.EX)
	public URI getNodeUid() {
		return nodeUid;
	}

	public void setNodeUid(URI nodeUid) {
		this.nodeUid = nodeUid;
	}

	@XmlElement(name = "StaticLeadURL0", namespace = Namespace.EX)
	public URI getStaticLeadUrl0() {
		return staticLeadUrl0;
	}

	public void setStaticLeadUrl0(URI staticLeadUrl0) {
		this.staticLeadUrl0 = staticLeadUrl0;
	}

	@XmlElement(name = "LeadUID", namespace = Namespace.EX)
	public URI getLeadUid() {
		return leadUid;
	}

	public void setLeadUid(URI leadUid) {
		this.leadUid = leadUid;
	}

	@XmlElement(name = "LastUpdatedReceivedUTC", namespace = Namespace.EX)
	public String getLastUpdateReceived() {
		return lastUpdateReceived;
	}

	@XmlElement(name = "Jurisdiction", namespace = Namespace.EX)
	public String getRegion() {
		return region;

	}

	public void setRegion(String region) {
		this.region = region;
	}

	public void setLastUpdateReceived(String lastUpdateReceived) {
		this.lastUpdateReceived = lastUpdateReceived;
	}

	@XmlElement(name = "IssuerParameterUID", namespace = Namespace.EX)
	public LinkedList<URI> getIssuerParameterUids() {
		if (issuerParameterUids==null) {
			issuerParameterUids = new LinkedList<URI>();
			
		}
		return issuerParameterUids;
	}

	public void setIssuerParameterUids(LinkedList<URI> issuerParameterUids) {
		this.issuerParameterUids = issuerParameterUids;
		
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NodeInformation) {
			NodeInformation n =(NodeInformation)obj;
			return n.getNodeUid().equals(this.getNodeUid());
			
		} else {
			return false; 
			 
		}
	}

	@Override
	public int hashCode() {
		return this.getNodeUid().hashCode();
		
	}
}
