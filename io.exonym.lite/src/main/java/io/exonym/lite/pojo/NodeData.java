package io.exonym.lite.pojo;

import io.exonym.lite.couchdb.AbstractCouchDbObject;

import java.net.URI;
import java.net.URL;

public class NodeData extends AbstractCouchDbObject  {
	
	// In superclass
	public static final String TYPE_MEMBER = "member";
	public static final String TYPE_NODE = "node";
	public static final String TYPE_NETWORK_NODE = "network-node";
	public static final String TYPE_SOURCE= "source";
	public static final String TYPE_RECIPIENT= "recipient";

	// this network name
	private String networkName;
	
	// [source=containerName || node=internalName || member=containerName || secondary-network=networkName]
	private String name;

	// [source || node || secondary-network || network-node=UID]
	private URL nodeUrl;

	// [source || node || secondary-network || network-node=UID]
	private URL failOverUrl;

	private URI nodeUid;

	private URL sourceUrl;

	private URI sourceUid;

	// hash of the revocation handle
	private String handle;

	// hash of the unique-endonym
	private String endonym;

	private String lastRAIHash;
	private String lastPPHash;

	public String getEndonym() {
		return endonym;
	}

	public void setEndonym(String endonym) {
		this.endonym = endonym;
	}

	public String getHandle() {
		return handle;
	}

	public void setHandle(String handle) {
		this.handle = handle;
	}

	// [member]
	public String fkUser;
	
	public boolean publishAfterReceipt = false;

	public String getNetworkName() {
		return networkName;
	}

	public void setNetworkName(String networkName) {
		this.networkName = networkName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}



	public String getFkUser() {
		return fkUser;
	}

	public void setFkUser(String fkUser) {
		this.fkUser = fkUser;
	}

	public boolean isPublishAfterReceipt() {
		return publishAfterReceipt;
	}

	public void setPublishAfterReceipt(boolean publishAfterReceipt) {
		this.publishAfterReceipt = publishAfterReceipt;
	}



	public URI getNodeUid() {
		return nodeUid;
	}

	public void setNodeUid(URI nodeUid) {
		this.nodeUid = nodeUid;
	}



	public URI getSourceUid() {
		return sourceUid;
	}

	public void setSourceUid(URI sourceUid) {
		this.sourceUid = sourceUid;
	}

	public URL getNodeUrl() {
		return nodeUrl;
	}

	public void setNodeUrl(URL nodeUrl) {
		this.nodeUrl = nodeUrl;
	}

	public URL getFailOverUrl() {
		return failOverUrl;
	}

	public void setFailOverUrl(URL failOverUrl) {
		this.failOverUrl = failOverUrl;
	}

	public URL getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(URL sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	public String getLastRAIHash() {
		return lastRAIHash;
	}

	public void setLastRAIHash(String lastRAIHash) {
		this.lastRAIHash = lastRAIHash;
	}

	public String getLastPPHash() {
		return lastPPHash;
	}

	public void setLastPPHash(String lastPPHash) {
		this.lastPPHash = lastPPHash;
	}


}
