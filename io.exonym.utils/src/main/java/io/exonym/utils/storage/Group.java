package io.exonym.utils.storage;

import java.net.URI;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="Group")
@XmlType(name = "Group", namespace = Namespace.EX)
public class Group {
	
	@XmlElement(name = "GroupUID", namespace = Namespace.EX)
	private URI groupUid;
	
	@XmlElement(name = "FacadeUID", namespace = Namespace.EX)
	private URI facadeUid;
	
	@XmlElement(name = "Name", namespace = Namespace.EX)
	private String name;
	
	@XmlElement(name = "Category", namespace = Namespace.EX)
	private ArrayList<Category> categories = new ArrayList<>();

	public URI getGroupUid() {
		return groupUid;
	}

	public void setGroupUid(URI groupUid) {
		this.groupUid = groupUid;
	}

	public URI getFacadeUid() {
		return facadeUid;
	}

	public void setFacadeUid(URI facadeUid) {
		this.facadeUid = facadeUid;
	}

	public String getName() {
		return name;
		
	}

	public void setName(String name) {
		this.name = name;
	}

	public ArrayList<Category> getCategories() {
		return categories;
	}

	public void setCategories(ArrayList<Category> categories) {
		this.categories = categories;
		
	}
	
}

