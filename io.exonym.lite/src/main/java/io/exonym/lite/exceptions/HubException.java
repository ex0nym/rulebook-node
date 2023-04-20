package io.exonym.lite.exceptions;

import java.net.URI;

public class HubException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String context;
	private URI connectUid;
	
	public HubException(String msg) {
		super(msg);
		
	}

	public HubException(String msg, Throwable e) {
		super(msg, e);
		
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public URI getConnectUid() {
		return connectUid;
	}

	public void setConnectUid(URI connectUid) {
		this.connectUid = connectUid;
	}

	@Override
	public synchronized Throwable initCause(Throwable cause) {
		return super.initCause(cause);
	}
}