package io.exonym.actor.storage;

import java.math.BigInteger;

public class Member {
	
	private String username;
	private String password, email, contactNumber; // this is all in Coops
	private BigInteger handle;

	public Member() {
		
	}


	public BigInteger getHandle() {
		return handle;
	}

	public void setHandle(BigInteger handle) {
		this.handle = handle;
	}

	public String getUsername() {
		return username;
		
	}

	public void setUsername(String username) {
		this.username = username;
		
	}

	public String getPassword() {
		return password;
		
	}

	public void setPassword(String password) {
		this.password = password;
		
	}

	public String getEmail() {
		return email;
		
	}

	public void setEmail(String email) {
		this.email = email;
		
	}

	public String getContactNumber() {
		return contactNumber;
		
	}

	public void setContactNumber(String contactNumber) {
		this.contactNumber = contactNumber;
		
	}
}
