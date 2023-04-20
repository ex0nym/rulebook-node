package io.exonym.utils.storage;

import java.net.URI;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="Chat")
@XmlType(name = "Chat", namespace = Namespace.EX)
public class Chat {

	@XmlElement(name = "ChatUID", namespace = Namespace.EX)
	private URI chatUid;

	@XmlElement(name = "DateUTC", namespace = Namespace.EX)
	private String timeCreatedUtc; 

	@XmlElement(name = "LastMessageTimeUTC", namespace = Namespace.EX)
	private String lastMessageTimeUtc;
	
	@XmlElement(name = "Messages", namespace = Namespace.EX)
	private ArrayList<PeerMessage> messages;

	public URI getChatUid() {
		return chatUid;
	}

	public void setChatUid(URI chatUid) {
		this.chatUid = chatUid;
	}

	public String getTimeCreatedUtc() {
		return timeCreatedUtc;
	}

	public void setTimeCreatedUtc(String timeCreatedUtc) {
		this.timeCreatedUtc = timeCreatedUtc;
	}

	public String getLastMessageTimeUtc() {
		return lastMessageTimeUtc;
	}

	public void setLastMessageTimeUtc(String lastMessageTimeUtc) {
		this.lastMessageTimeUtc = lastMessageTimeUtc;
	}

	public ArrayList<PeerMessage> getMessages() {
		return messages;
	}

	public void setMessages(ArrayList<PeerMessage> messages) {
		this.messages = messages;
	}

}
