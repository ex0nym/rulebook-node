package io.exonym.utils.storage;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="SharedItem")
@XmlType(name = "SharedItem", namespace = Namespace.EX)
public class SharedItem {
	
	@XmlElement(name = "SharedItemUID", namespace = Namespace.EX)
	private URI sharedItemUid;
	
	@XmlElement(name = "TargetGroupUID", namespace = Namespace.EX)
	private HashSet<URI> groupUid;
	
	@XmlElement(name = "TimeCreatedUTC", namespace = Namespace.EX)
	private String timeCreatedUtc; 
	
	@XmlElement(name = "LastUpdatedUTC", namespace = Namespace.EX)
	private String lastUpdatedUtc;
	
	@XmlElement(name = "File", namespace = Namespace.EX)
	private byte[] file;
	
	@XmlElement(name = "AbsolutePath", namespace = Namespace.EX)
	private String absolutePath;
	
	@XmlElement(name = "Format", namespace = Namespace.EX)
	private String format;
	
	@XmlElement(name = "Description", namespace = Namespace.EX)
	private ArrayList<String> description;

	public URI getSharedItemUid() {
		return sharedItemUid;
	}

	public void setSharedItemUid(URI sharedItemUid) {
		this.sharedItemUid = sharedItemUid;
	}

	public HashSet<URI> getGroupUid() {
		return groupUid;
	}

	public void setGroupUid(HashSet<URI> groupUid) {
		this.groupUid = groupUid;
	}

	public String getTimeCreatedUtc() {
		return timeCreatedUtc;
	}

	public void setTimeCreatedUtc(String timeCreatedUtc) {
		this.timeCreatedUtc = timeCreatedUtc;
	}

	public String getLastUpdatedUtc() {
		return lastUpdatedUtc;
	}

	public void setLastUpdatedUtc(String lastUpdatedUtc) {
		this.lastUpdatedUtc = lastUpdatedUtc;
	}

	public byte[] getFile() {
		return file;
	}

	public void setFile(byte[] file) {
		this.file = file;
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public void setAbsolutePath(String absolutePath) {
		this.absolutePath = absolutePath;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public ArrayList<String> getDescription() {
		return description;
	}

	public void setDescription(ArrayList<String> description) {
		this.description = description;
	}
}
