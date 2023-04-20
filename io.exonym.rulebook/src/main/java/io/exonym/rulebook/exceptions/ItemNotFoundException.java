package io.exonym.rulebook.exceptions;

public class ItemNotFoundException extends Exception {

	public ItemNotFoundException(String msg) {
		super(msg);

	}

	public ItemNotFoundException(String msg, Exception e) {
		super(msg, e);

	}
}
