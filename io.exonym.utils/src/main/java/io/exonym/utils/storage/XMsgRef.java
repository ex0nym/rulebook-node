package io.exonym.utils.storage;

import java.net.URI;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="XMsgRef")
@XmlType(name = "XMsgRef", namespace = Namespace.EX)
public class XMsgRef {
	
	public static XMsgRef make(URI deviceUid, String context, URI peerUid, String username){
		XMsgRef result = new XMsgRef();
		result.setContext(context);;
		result.setDeviceUid(deviceUid);
		result.setPeerUid(peerUid);
		result.setUsername(username);
		return result; 
		
	} 

	@XmlElement(name="DeviceUID", namespace=Namespace.EX)
	private URI deviceUid;
	@XmlElement(name="Context", namespace=Namespace.EX)
	private String context; 
	@XmlElement(name="PeerUID", namespace=Namespace.EX)
	private URI peerUid;
	@XmlElement(name="RequestingUsername", namespace=Namespace.EX)
	private String username;
	@XmlElement(name="CallingFunction", namespace=Namespace.EX)
	private String function;
	
	public URI getDeviceUid() {
		return deviceUid;
	}
	public void setDeviceUid(URI deviceUid) {
		this.deviceUid = deviceUid;
	}
	public String getContext() {
		return context;
	}
	public void setContext(String context) {
		this.context = context;
	}
	public URI getPeerUid() {
		return peerUid;
	}
	public void setPeerUid(URI peerUid) {
		this.peerUid = peerUid;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getFunction() {
		return function;
	}
	public void setFunction(String function) {
		this.function = function;
	}

}
