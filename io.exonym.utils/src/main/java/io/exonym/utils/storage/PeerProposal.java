package io.exonym.utils.storage;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="PeerProposal")
@XmlType(name = "PeerProposal", namespace = Namespace.EX, 
	propOrder={"nym", "scope", "publicKey", "qrPng"})
public class PeerProposal {
	
	@XmlElement(name = "QRpng", namespace = Namespace.EX)
	private byte[] qrPng;
	
	@XmlElement(name = "Scope", namespace = Namespace.EX)
	private String scope;
	
	@XmlElement(name = "Exonym", namespace = Namespace.EX)
	private String nym;

	@XmlElement(name = "PublicKey", namespace = Namespace.EX)
	private byte[] publicKey;

	
	public byte[] getQrPng() {
		return qrPng;
	}

	public void setQrPng(byte[] qrPng) {
		this.qrPng = qrPng;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String getNym() {
		return nym;
	}

	public void setNym(String nym) {
		this.nym = nym;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(byte[] publicKey) {
		this.publicKey = publicKey;
	}

}
