package io.exonym.actor.storage;

import java.net.URI;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="Transfer", namespace = Namespace.EX)
@XmlType(name = "Transfer", namespace = Namespace.EX)
public class Transfer {

	
	private URI transferUid = URI.create("urn:transfer");
	private URI destinationUrl;
	private String transferRequestTime;
	private String transferToBeCompletedBy;
	private String status; 


	@XmlElement(name = "DestinationUrl", namespace = Namespace.EX)
	public URI getDestinationUrl() {
		return destinationUrl;
	}

	public void setDestinationUrl(URI destinationUrl) {
		this.destinationUrl = destinationUrl;
	}

	public String getTransferRequestTime() {
		return transferRequestTime;
	}

	@XmlElement(name = "TransferRequestTimeUtc", namespace = Namespace.EX)
	public void setTransferRequestTime(String transferRequestTime) {
		this.transferRequestTime = transferRequestTime;
	}

	@XmlElement(name = "TransferToBeCompleteByUtc", namespace = Namespace.EX)
	public String getTransferToBeCompletedBy() {
		return transferToBeCompletedBy;
	}

	public void setTransferToBeCompletedBy(String transferToBeCompletedBy) {
		this.transferToBeCompletedBy = transferToBeCompletedBy;
	}

	@XmlElement(name = "Status", namespace = Namespace.EX)
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@XmlElement(name = "TransferUid", namespace = Namespace.EX)
	public URI getTransferUid() {
		return transferUid;
	}
}
