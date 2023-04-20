package io.exonym.utils.storage;

import java.net.URI;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;
import io.exonym.utils.adapters.PresentationPolicyAdapter;
import io.exonym.utils.adapters.PresentationPolicyAlternativesAdapter;

@XmlRootElement(name="Proof")
@XmlType(name = "Proof", namespace = Namespace.EX)
public class Proof {
	
	@XmlElement(name = "ProofUID", namespace = Namespace.EX)
	private URI proofUid;

	@XmlElement(name = "PresentationPolicyAlternatives", namespace = Namespace.EX)
	private PresentationPolicyAlternativesAdapter presentationPolicyAlternatives;
	
	@XmlElement(name = "PresentationPolicySelection", namespace = Namespace.EX)
	private ArrayList<PresentationPolicyAdapter> presentationPolicy;
	
	@XmlElement(name = "EndonymOfProver", namespace = Namespace.EX)
	private String endonymOfProver;
	
	@XmlElement(name = "IsRequestOfMe", namespace = Namespace.EX)
	private boolean requestOfMe = false;
	
	@XmlElement(name = "IsFillable", namespace = Namespace.EX)
	private boolean fillable = false;
	
	@XmlElement(name = "IsFilled", namespace = Namespace.EX)
	private boolean filled = false;
	
	@XmlElement(name = "Message", namespace = Namespace.EX)
	private ArrayList<String> messages; 
	
	@XmlElement(name = "RequestTimeUTC", namespace = Namespace.EX)
	private String requestTimeUtc;
	
	@XmlElement(name = "FilledTimeUTC", namespace = Namespace.EX)
	private String fillTimeUtc;
	
	@XmlElement(name = "RequestContext", namespace = Namespace.EX)
	private String requestContext;
	
	@XmlElement(name = "ReceiptContext", namespace = Namespace.EX)
	private String receiptContext;
	
	@XmlElement(name = "RequestingDeviceUid", namespace = Namespace.EX)
	private URI requestDevice;

	public PresentationPolicyAlternativesAdapter getPresentationPolicyAlternatives() {
		return presentationPolicyAlternatives;
	}

	public void setPresentationPolicyAlternatives(PresentationPolicyAlternativesAdapter presentationPolicyAlternatives) {
		this.presentationPolicyAlternatives = presentationPolicyAlternatives;
	}

	public ArrayList<PresentationPolicyAdapter> getPresentationPolicy() {
		if (presentationPolicy==null){
			presentationPolicy = new ArrayList<>();
			
		}
		return presentationPolicy;
	}

	public URI getProofUid() {
		return proofUid;
		
	}

	public void setProofUid(URI proofUid) {
		this.proofUid = proofUid;
		
	}

	public void setPresentationPolicy(ArrayList<PresentationPolicyAdapter> presentationPolicy) {
		this.presentationPolicy = presentationPolicy;
	}

	public String getEndonymOfProver() {
		return endonymOfProver;
	}

	public void setEndonymOfProver(String endonymOfProver) {
		this.endonymOfProver = endonymOfProver;
	}

	public boolean isRequestOfMe() {
		return requestOfMe;
	}

	public void setRequestOfMe(boolean requestOfMe) {
		this.requestOfMe = requestOfMe;
	}

	public boolean isFillable() {
		return fillable;
	}

	public void setFillable(boolean fillable) {
		this.fillable = fillable;
	}

	public boolean isFilled() {
		return filled;
	}

	public void setFilled(boolean filled) {
		this.filled = filled;
	}

	public ArrayList<String> getMessages() {
		if (messages==null){
			messages = new ArrayList<>();
			
		}
		return messages;
		
	}

	public URI getRequestDevice() {
		return requestDevice;
	}

	public void setRequestDevice(URI requestDevice) {
		this.requestDevice = requestDevice;
	}

	public String getRequestContext() {
		return requestContext;
	}

	public void setRequestContext(String requestContext) {
		this.requestContext = requestContext;
	}

	public void setMessages(ArrayList<String> messages) {
		this.messages = messages;
	}

	public String getRequestTimeUtc() {
		return requestTimeUtc;
	}

	public void setRequestTimeUtc(String requestTimeUtc) {
		this.requestTimeUtc = requestTimeUtc;
	}

	public String getFillTimeUtc() {
		return fillTimeUtc;
	}

	public void setFillTimeUtc(String fillTimeUtc) {
		this.fillTimeUtc = fillTimeUtc;
	}
	
	public String getReceiptContext() {
		return receiptContext;
	}

	public void setReceiptContext(String receiptContext) {
		this.receiptContext = receiptContext;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Proof){
			Proof p = (Proof)obj;
			if (this.proofUid==null){
				return false; 
				
			} 
			return p.getProofUid().equals(this.proofUid);
			
		} else if (obj instanceof URI){
			return ((URI)obj).equals(this.proofUid);
			
		} else {
			return false;
			
		}
	}

	@Override
	public int hashCode() {
		return this.proofUid.hashCode();
		
	}
}
