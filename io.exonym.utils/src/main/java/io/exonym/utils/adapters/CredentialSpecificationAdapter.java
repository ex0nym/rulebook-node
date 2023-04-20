package io.exonym.utils.adapters;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import eu.abc4trust.xml.CredentialSpecification;
import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="CredentialSpecificationAdapter")
public class CredentialSpecificationAdapter implements Serializable {

	private static final long serialVersionUID = 1L;
	@XmlElement(name = "CredentialSpecification", namespace = Namespace.ABC)
	private CredentialSpecification spec;

	public CredentialSpecification getSpec() {
		return spec;
	}

	public void setSpec(CredentialSpecification spec) {
		this.spec = spec;
	}

	
	public static CredentialSpecificationAdapter create(CredentialSpecification c){
		CredentialSpecificationAdapter csa = new CredentialSpecificationAdapter();
		csa.setSpec(c);
		return csa;
		
	}

}
