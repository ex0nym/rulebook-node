package io.exonym.utils.storage;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlType(name = "Signature", namespace = Namespace.EX)
public class Signature {
	
	@XmlElement(name="Signature", namespace=Namespace.EX)
	private byte[] signature;
	@XmlElement(name="Hash", namespace=Namespace.EX)
	private byte[] hash;

	public byte[] getSignature() {
		return signature;
		
	}
	public void setSignature(byte[] signature) {
		this.signature = signature;
	}
	public byte[] getHash() {
		return hash;
	}
	public void setHash(byte[] hash) {
		this.hash = hash;
	}
	
	

}
