package io.exonym.exceptions;

import eu.abc4trust.xml.AttributePredicate;

public class AttributePredicateException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final AttributePredicate token;
	
	public AttributePredicateException(AttributePredicate token) {
		this.token=token;
		
	}

	public AttributePredicate getToken() {
		return token;
	}

}
