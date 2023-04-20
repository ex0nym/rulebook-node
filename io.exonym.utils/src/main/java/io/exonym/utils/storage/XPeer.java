package io.exonym.utils.storage;

import java.net.URI;
import java.util.HashSet;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.XKey;
import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="Peer")
@XmlType(name = "Peer", namespace = Namespace.EX)
public class XPeer {
	
	@XmlElement(name = "Facade", namespace = Namespace.EX)
	private NetworkFacade facade;
	
	@XmlElement(name = "OriginNote", namespace = Namespace.EX)
	private String originNote;
	
	@XmlElement(name = "Endonym", namespace = Namespace.EX)
	private String theirNym;
	
	@XmlElement(name = "Endoscope", namespace = Namespace.EX)
	private String theirScope; 
	
	@XmlElement(name = "MyScope", namespace = Namespace.EX)
	private String myScope;
	
	@XmlElement(name = "MyNym", namespace = Namespace.EX)
	private String myNym;
	
	@XmlElement(name = "ContainerName", namespace = Namespace.EX)
	private String containerName;
	
	@XmlElement(name = "MyKey", namespace = Namespace.EX)
	private XKey myKey;
	
	@XmlElement(name = "TheirKey", namespace = Namespace.EX)
	private XKey theirKey; 
	
	@XmlElement(name = "GroupUID", namespace = Namespace.EX)
	private HashSet<URI> groupUids; 
	
	@XmlElement(name = "Proof", namespace = Namespace.EX)
	private HashSet<Proof> proofs;
	
	@XmlElement(name = "ChatEnabled", namespace = Namespace.EX)
	private boolean chatEnabled = false; 
	
	@XmlElement(name = "Visible", namespace = Namespace.EX)
	private boolean visible = true; 
	
	@XmlElement(name = "ChatUID", namespace = Namespace.EX)
	private URI chatUid;
	
	@XmlElement(name = "ChatQuietUntil", namespace = Namespace.EX)
	private String chatQuietUntil;
	
	@XmlElement(name = "PushNotifications", namespace = Namespace.EX)
	private boolean pushNotifications = true; 

	public XKey getMyKey() {
		return myKey;
	}

	public void setMyKey(XKey myKey) {
		this.myKey = myKey;
	}

	public XKey getTheirKey() {
		return theirKey;
	}

	public void setTheirKey(XKey theirKey) {
		this.theirKey = theirKey;
	}

	public NetworkFacade getFacade() {
		return facade;
	}

	public void setFacade(NetworkFacade facade) {
		this.facade = facade;
	}

	public String getOriginNote() {
		return originNote;
	}

	public void setOriginNote(String originNote) {
		this.originNote = originNote;
	}

	public String getTheirNym() {
		return theirNym;
	}

	public void setTheirNym(String theirNym) {
		this.theirNym = theirNym;
	}

	public String getTheirScope() {
		return theirScope;
	}

	public void setTheirScope(String theirScope) {
		this.theirScope = theirScope;
	}

	public String getMyScope() {
		return myScope;
	}

	public void setMyScope(String myScope) {
		this.myScope = myScope;
	}

	public String getMyNym() {
		return myNym;
	}

	public void setMyNym(String myNym) {
		this.myNym = myNym;
	}

	public HashSet<URI> getGroupUids() {
		if (groupUids==null){
			groupUids = new HashSet<>();
			
		}
		return groupUids;
	}

	public void setGroupUids(HashSet<URI> groupUids) {
		this.groupUids = groupUids;
	}

	public HashSet<Proof> getProofs() {
		if (proofs==null){
			proofs = new HashSet<>();
		}
		return proofs;
		
	}

	public void setProofs(HashSet<Proof> proofs) {
		this.proofs = proofs;
	}

	public boolean isChatEnabled() {
		return chatEnabled;
	}

	public void setChatEnabled(boolean chatEnabled) {
		this.chatEnabled = chatEnabled;
	}

	public URI getChatUid() {
		return chatUid;
	}

	public void setChatUid(URI chatUid) {
		this.chatUid = chatUid;
	}

	public String getChatQuietUntil() {
		return chatQuietUntil;
	}

	public void setChatQuietUntil(String chatQuietUntil) {
		this.chatQuietUntil = chatQuietUntil;
	}

	public String getContainerName() {
		return containerName;
	}

	public void setContainerName(String containerName) {
		this.containerName = containerName;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean isPushNotifications() {
		return pushNotifications;
	}

	public void setPushNotifications(boolean pushNotifications) {
		this.pushNotifications = pushNotifications;
	}
}