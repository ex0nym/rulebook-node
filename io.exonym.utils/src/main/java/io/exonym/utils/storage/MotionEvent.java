package io.exonym.utils.storage;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlType(name = "MotionEvent", namespace = Namespace.EX, 
		propOrder = { "motionEventUid", "dependencyUid", "structuredAction", "eventPublicKey", 
					"requiredAssets", "descriptionParagraphs", "justificationParagraphs"})
public class MotionEvent implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@XmlElement(name = "MotionEventUID", namespace = Namespace.EX)
	private URI motionEventUid;
	
	@XmlElement(name = "MotionEventDependencyUID", namespace = Namespace.EX)
	private URI dependencyUid;
	
	@XmlElement(name = "StructuredAction", namespace = Namespace.EX)
	private byte[] structuredAction;
	
	@XmlElement(name = "EventPublicKey", namespace = Namespace.EX)
	private byte[] eventPublicKey;
	
	@XmlElement(name = "RequiredAssets", namespace = Namespace.EX)
	private ArrayList<Asset> requiredAssets = new ArrayList<>();

	@XmlElement(name = "Descrption", namespace = Namespace.EX)
	private ArrayList<String> descriptionParagraphs;
	
	@XmlElement(name = "Justification", namespace = Namespace.EX)
	private ArrayList<String> justificationParagraphs;
	
	public ArrayList<Asset> getRequiredAssets() {
		return requiredAssets;
	}

	public void setRequiredAssets(ArrayList<Asset> requiredAssets) {
		this.requiredAssets = requiredAssets;
	}
	public URI getMotionEventUid() {
		return motionEventUid;
	}

	public void setMotionEventUid(URI motionEventUid) {
		this.motionEventUid = motionEventUid;
	}

	public ArrayList<String> getDescriptionParagraphs() {
		return descriptionParagraphs;
	}

	public void setDescriptionParagraphs(ArrayList<String> descriptionParagraphs) {
		this.descriptionParagraphs = descriptionParagraphs;
	}

	public ArrayList<String> getJustificationParagraphs() {
		return justificationParagraphs;
	}

	public void setJustificationParagraphs(ArrayList<String> justificationParagraphs) {
		this.justificationParagraphs = justificationParagraphs;
	}

	public byte[] getStructuredAction() {
		return structuredAction;
	}

	public void setStructuredAction(byte[] structuredAction) {
		this.structuredAction = structuredAction;
	}

	public URI getDependencyUid() {
		return dependencyUid;
	}

	public void setDependencyUid(URI dependencyUid) {
		this.dependencyUid = dependencyUid;
	}

	public byte[] getEventPublicKey() {
		return eventPublicKey;
	}

	public void setEventPublicKey(byte[] eventPublicKey) {
		this.eventPublicKey = eventPublicKey;
	}
	
	

}