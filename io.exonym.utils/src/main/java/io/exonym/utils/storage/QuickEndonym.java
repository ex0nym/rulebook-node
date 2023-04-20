package io.exonym.utils.storage;

import java.util.HashSet;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="QuickEndonym")
@XmlType(name = "QuickEndonym", namespace = Namespace.EX)
public class QuickEndonym {
	
	@XmlElement(name = "Endonym", namespace = Namespace.EX)
	private HashSet<String> endonyms = new HashSet<>();

	public HashSet<String> getEndonyms() {
		return endonyms;
	}

	public void setEndonyms(HashSet<String> endonyms) {
		this.endonyms = endonyms;
	}
	
}
