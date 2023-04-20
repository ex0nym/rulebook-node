package io.exonym.utils.storage;

import java.net.URI;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="Category")
@XmlType(name = "Category", namespace = Namespace.EX)
public class Category {
	
	@XmlElement(name = "CategoryUID", namespace = Namespace.EX)
	private URI categoryUid;
	
	// So "Status Updates, Photo Albums, Blogs, Music Files"
	@XmlElement(name = "Name", namespace = Namespace.EX)
	private String name;

	// So "Status Updates~Random Moans, ~Political Commentary..."
	@XmlElement(name = "SubCategory", namespace = Namespace.EX)
	private TreeSet<SubCategory> subCategories = new TreeSet<>();

	public URI getCategoryUid() {
		return categoryUid;
	}

	public void setCategoryUid(URI categoryUid) {
		this.categoryUid = categoryUid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TreeSet<SubCategory> getSubCategories() {
		return subCategories;
		
	}

	public void setSubCategories(TreeSet<SubCategory> subCategories) {
		this.subCategories = subCategories;
		
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof Category){
			Category c = (Category)arg0;
			return c.getCategoryUid().equals(this.getCategoryUid());
			
		} else {
			return false; 
			
		}
		
		
	}

	@Override
	public int hashCode() {
		return this.getCategoryUid().hashCode();
		
	}
}
