package io.exonym.utils.storage;

import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="ConfirmationIndex")
@XmlType(name = "ConfirmationIndex", namespace = Namespace.EX)
public class ConfirmationIndex {
	
	@XmlElement(name="Unconfirmed", namespace=Namespace.EX)
	private ConcurrentHashMap<String, XMsgRef> unconfirmed = new ConcurrentHashMap<>();

	public ConcurrentHashMap<String, XMsgRef> getUnconfirmed() {
		return unconfirmed;
	}

	public void setUnconfirmed(ConcurrentHashMap<String, XMsgRef> unconfirmed) {
		this.unconfirmed = unconfirmed;
	}

}
