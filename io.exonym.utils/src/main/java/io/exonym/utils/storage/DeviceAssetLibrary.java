package io.exonym.utils.storage;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;
import io.exonym.utils.adapters.CredentialSpecificationAdapter;
import io.exonym.utils.adapters.IssuerParametersAdapter;

@XmlRootElement(name="DeviceAssetLibrary")
@XmlType(name = "DeviceAssetLibrary", namespace = Namespace.EX)
public class DeviceAssetLibrary {
	
	@XmlElement(name = "Username", namespace = Namespace.EX)
	private String username;

	@XmlElement(name = "LastUpdatedTimeUtc", namespace = Namespace.EX)
	private String lastUpdateTimeUtc;

	@XmlElement(name = "AnonymousCredentialCountry", namespace = Namespace.EX)
	private String country;
	
	@XmlElement(name = "CredentialSpecifications", namespace = Namespace.EX)
	private ArrayList<CredentialSpecificationAdapter> credentials = new ArrayList<>();
	
	@XmlElement(name = "IssuerParameters", namespace = Namespace.EX)
	private ArrayList<IssuerParametersAdapter> issuers = new ArrayList<>();
	
	public String getCountry() {
		return country;
		
	}
	public void setCountry(String country) {
		this.country = country;
		
	}
	public ArrayList<CredentialSpecificationAdapter> getCredentials() {
		return credentials;
		
	}
	public void setCredentials(ArrayList<CredentialSpecificationAdapter> credentials) {
		this.credentials = credentials;
		
	}
	public ArrayList<IssuerParametersAdapter> getIssuers() {
		return issuers;
		
	}
	public void setIssuers(ArrayList<IssuerParametersAdapter> issuers) {
		this.issuers = issuers;
		
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getLastUpdateTimeUtc() {
		return lastUpdateTimeUtc;
	}
	public void setLastUpdateTimeUtc(String lastUpdateTimeUtc) {
		this.lastUpdateTimeUtc = lastUpdateTimeUtc;
	}
	
}
