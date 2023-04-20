package io.exonym.exceptions;

import java.net.URI;

import eu.abc4trust.xml.CredentialInPolicy;

public class CredentialInTokenException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final CredentialInPolicy cip;
	
	public CredentialInTokenException(URI credentialSpecificationUid, CredentialInPolicy cip) {
		super(credentialSpecificationUid.toString()); 
		this.cip=cip;
		
	}

	public CredentialInPolicy getCip() {
		return cip;
	}

}
