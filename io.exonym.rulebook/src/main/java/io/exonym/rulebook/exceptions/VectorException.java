package io.exonym.rulebook.exceptions;

public class VectorException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String sessionId; 
	
	public VectorException(String sessionId) {
		super("Vector Alert");
		this.sessionId=sessionId;
		
	}
	
	public String getSessionId(){
		return sessionId;
		
	}

}
