package io.exonym.utils.adapters;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import eu.abc4trust.xml.RevocationAuthorityParameters;
import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="RevocationAuthorityParametersAdapter")
public class RevocationAuthorityParametersAdapter implements Serializable {

	private static final long serialVersionUID = 1L;
	@XmlElement(name = "RevocationAuthorityParameters", namespace = Namespace.ABC)
	private RevocationAuthorityParameters parameters;
	
	public RevocationAuthorityParameters getParameters() {
		return parameters;
	}
	
	public void setParameters(RevocationAuthorityParameters parameters) {
		this.parameters = parameters;
		
	}
}
