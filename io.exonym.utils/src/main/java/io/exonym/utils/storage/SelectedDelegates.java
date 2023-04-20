package io.exonym.utils.storage;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="SelectedDelegates")
@XmlType(name = "SelectedDelegates", namespace = Namespace.EX, propOrder={"selectedDelegatesUid", "actionUid", "p", "delegates", "testString"})
public class SelectedDelegates {
	

	@XmlElement(name="SelectedDelegatesUID", namespace=Namespace.EX)
	private URI selectedDelegatesUid;
	
	@XmlElement(name="ActionUID", namespace=Namespace.EX)
	private URI actionUid;

	@XmlElement(name="Delegates", namespace=Namespace.EX)
	private final ArrayList<Delegate> delegates = new ArrayList<>();
	
	@XmlElement(name="prime", namespace=Namespace.EX)
	private BigInteger p;
	
	@XmlElement(name="test", namespace=Namespace.EX)
	private byte[] testString;
	
	protected void addDelegate(Delegate delegate){
		this.delegates.add(delegate);
		
	} 

	public ArrayList<Delegate> getDelegates() {
		return delegates;
		
	}

	public BigInteger getP() {
		return p;
		
	}

	public void setP(BigInteger p) {
		this.p = p;
		
	}

	public byte[] getTestString() {
		return testString;
	}

	public void setTestString(byte[] testString) {
		this.testString = testString;
	}

	public URI getSelectedDelegatesUid() {
		return selectedDelegatesUid;
	}

	public void setSelectedDelegatesUid(URI selectedDelegatesUid) {
		this.selectedDelegatesUid = selectedDelegatesUid;
	}

	public URI getActionUid() {
		return actionUid;
	}

	public void setActionUid(URI actionUid) {
		this.actionUid = actionUid;
	}
	
	
	
	
}
