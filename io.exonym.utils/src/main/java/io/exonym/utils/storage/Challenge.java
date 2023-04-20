package io.exonym.utils.storage;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.CryptoUtils;

@XmlRootElement(name="Challenge")
@XmlType(name = "Challenge", namespace = Namespace.EX)
public class Challenge {
	
	@XmlElement(name = "Context", namespace = Namespace.EX)
	private String context;
	@XmlElement(name = "Challenge", namespace = Namespace.EX)
	private String challenge;
	@XmlElement(name = "PublicKey", namespace = Namespace.EX)
	private byte[] publicKey;
	
	// TODO: Consider extending the challenge to hash the context with the key.
	public static Challenge createChallenge(String context, AsymStoreKey key){
		Challenge challenge = new Challenge();
		challenge.setPublicKey(key.getPublicKey().getEncoded());
		challenge.setContext(context);
		challenge.setChallenge(CryptoUtils.computeSha256HashAsHex(challenge.getPublicKey()));
		return challenge; 
		
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public String getChallenge() {
		return challenge;
	}

	public void setChallenge(String challenge) {
		this.challenge = challenge;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(byte[] publicKey) {
		this.publicKey = publicKey;
	}
	
	
}
