package io.exonym.utils.storage;

import java.net.URI;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="InteractiveActionData")
@XmlType(name = "InteractiveActionData", namespace = Namespace.EX)
public class InteractiveActionData {
	
	@XmlElement(name = "ActionUID", namespace = Namespace.EX)
	private URI actionUid;

	@XmlElement(name = "CreationTimeUTC", namespace = Namespace.EX)
	private String creationTimeUtc;

	@XmlElement(name = "InternalStructuredAction", namespace = Namespace.EX)
	private byte[] structuredAction;
	
	@XmlElement(name = "ActionAccessKey", namespace = Namespace.EX)
	private byte[] actionAccessKey;

	@XmlElement(name = "EventPublicKey", namespace = Namespace.EX)
	private byte[] eventPublicKey;

	public URI getActionUid() {
		return actionUid;
	}

	public void setActionUid(URI actionUid) {
		this.actionUid = actionUid;
	}

	public byte[] getStructuredAction() {
		return structuredAction;
	}

	public void setStructuredAction(byte[] structuredAction) {
		this.structuredAction = structuredAction;
	}

	public byte[] getEventPublicKey() {
		return eventPublicKey;
	}

	public void setEventPublicKey(byte[] eventPublicKey) {
		this.eventPublicKey = eventPublicKey;
	}

	public byte[] getActionAccessKey() {
		return actionAccessKey;
	}

	public void setActionAccessKey(byte[] actionAccessKey) {
		this.actionAccessKey = actionAccessKey;
	}

	public String getCreationTimeUtc() {
		return creationTimeUtc;
	}

	public void setCreationTimeUtc(String creationTimeUtc) {
		this.creationTimeUtc = creationTimeUtc;
	}
	
	
	
	
}
