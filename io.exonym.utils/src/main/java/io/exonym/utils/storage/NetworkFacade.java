package io.exonym.utils.storage;

import java.net.URI;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="NetworkFacade")
@XmlType(name = "NetworkFacade", namespace = Namespace.EX)
public class NetworkFacade {

	@XmlElement(name = "FacadeUid", namespace = Namespace.EX)
	private URI facadeUid;
	
	@XmlElement(name = "ScreenName", namespace = Namespace.EX)
	private String screenName;
	
	@XmlElement(name = "Avatar", namespace = Namespace.EX)
	private byte[] avatar;
	
	@XmlElement(name = "IsSafe", namespace = Namespace.EX)
	private boolean safe = true;
	
	@XmlElement(name = "Endonym", namespace = Namespace.EX)
	private String endonym; 

	public URI getFacadeUid() {
		return facadeUid;
	}

	public void setFacadeUid(URI facadeUid) {
		this.facadeUid = facadeUid;
	}

	public String getScreenName() {
		return screenName;
	}

	public void setScreenName(String screenName) {
		this.screenName = screenName;
	}

	public byte[] getAvatar() {
		return avatar;
	}

	public void setAvatar(byte[] avatar) {
		this.avatar = avatar;
	}

	public boolean isSafe() {
		return safe;
	}

	public void setSafe(boolean safe) {
		this.safe = safe;
	}

	public String getEndonym() {
		return endonym;
	}

	public void setEndonym(String endonym) {
		this.endonym = endonym;
	}
}
