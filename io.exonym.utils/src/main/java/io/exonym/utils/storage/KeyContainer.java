package io.exonym.utils.storage;

import io.exonym.lite.pojo.XKey;
import io.exonym.lite.pojo.Namespace;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;

@XmlRootElement(name="KeyContainer", namespace=Namespace.EX)
@XmlType(name = "KeyContainer")
public class KeyContainer {

	private String lastUpdateTime = null;
	private ArrayList<XKey> keyPairs = new ArrayList<>();

	@XmlElement(name="KeyPairs", namespace=Namespace.EX)
	public ArrayList<XKey> getKeyPairs() {
		return keyPairs;
		
	}

	public void setKeyPairs(ArrayList<XKey> keyPairs) {
		this.keyPairs = keyPairs;
		
	}

	public String getLastUpdateTime() {
		return lastUpdateTime;
	}

	public void setLastUpdateTime(String lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}
}
