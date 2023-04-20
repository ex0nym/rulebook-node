package io.exonym.utils.adapters;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import eu.abc4trust.xml.IssuerParameters;
import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="IssuerParametersAdapter")
public class IssuerParametersAdapter implements Serializable {

	private static final long serialVersionUID = 1L;
	@XmlElement(name = "IssuerParameters", namespace = Namespace.ABC)
	private IssuerParameters token;
	
	public IssuerParameters getToken() {
		return token;
	}
	public void set(IssuerParameters token) {
		this.token = token;
	}
	
}
