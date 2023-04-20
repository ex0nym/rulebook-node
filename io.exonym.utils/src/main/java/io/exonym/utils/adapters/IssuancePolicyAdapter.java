package io.exonym.utils.adapters;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import eu.abc4trust.xml.IssuancePolicy;
import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="IssuerPolicyAdapter")
public class IssuancePolicyAdapter implements Serializable {

	private static final long serialVersionUID = 1L;
	@XmlElement(name = "IssuerPolicy", namespace = Namespace.ABC)
	private IssuancePolicy policy;
	
	public IssuancePolicy getPolicy() {
		return policy;
	}
	public void setPolicy(IssuancePolicy policy) {
		this.policy = policy;
	}
	
	public static IssuancePolicyAdapter create(IssuancePolicy i){
		IssuancePolicyAdapter ipa = new IssuancePolicyAdapter();
		ipa.setPolicy(i);
		return ipa;
		
	}
	
	
	
}