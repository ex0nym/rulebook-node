package io.exonym.utils.storage;

import java.math.BigInteger;
import java.net.URI;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="Delegate")
@XmlType(name = "Delegate", namespace = Namespace.EX)
public class Delegate {
	
	@XmlElement(name="ActionUid", namespace=Namespace.EX)
	private URI actionUid;
	
	@XmlElement(name="PublicKey", namespace=Namespace.EX)
	private byte[] publicKey;
	
	@XmlElement(name="PrivateKey", namespace=Namespace.EX)
	private byte[] privateKey;

	@XmlElement(name="Signature", namespace=Namespace.EX)
	private byte[] signature;
	
	@XmlElement(name="VoteShare", namespace=Namespace.EX)
	private BigInteger share;
	
	@XmlElement(name="Sequence", namespace=Namespace.EX)
	private Integer sequence;
	
	public Delegate() {}
	
	public PublicKey assemblePublicKey() throws Exception {
		return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));

	}
	public byte[] getPublicKey() {
		return publicKey;
		
	}

	public void setPublicKey(byte[] publicKey) {
		this.publicKey = publicKey;
		
	}

	public URI getActionUid() {
		return actionUid;
	}

	public void setActionUid(URI actionUid) {
		this.actionUid = actionUid;
	}

	public BigInteger getShare() {
		return share;
	}

	public void setShare(BigInteger share) {
		this.share = share;
	}

	public byte[] getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(byte[] privateKey) {
		this.privateKey = privateKey;
	}

	public int getSequence() {
		return sequence;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	public byte[] getSignature() {
		return signature;
	}

	public void setSignature(byte[] signature) {
		this.signature = signature;
	}
	
}
