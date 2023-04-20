package io.exonym.utils.adapters;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import eu.abc4trust.xml.InspectorPublicKey;
import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="InspectorParametersAdapter")
public class InspectorParametersAdapter implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@XmlElement(name = "InspectorParameters", namespace = Namespace.ABC)
	private InspectorPublicKey inspectorParameters;

	public InspectorPublicKey getInspectorParameters() {
		return inspectorParameters;
	}

	public void setInspectorParameters(InspectorPublicKey inspectorParameters) {
		this.inspectorParameters = inspectorParameters;
	}
		
}