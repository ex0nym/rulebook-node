package io.exonym.utils.storage;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;


@XmlRootElement(name="SubCategory")
@XmlType(name = "SubCategory", namespace = Namespace.EX)
public class SubCategory implements Comparable<SubCategory> {

	@XmlElement(name = "Name", namespace = Namespace.EX)
	private String name;
	
	@XmlElement(name = "IsAllowed", namespace = Namespace.EX)
	private boolean allowed = true;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = (name!=null ? name.trim() : name);
	}



	public boolean isAllowed() {
		return allowed;
	}



	public void setAllowed(boolean allowed) {
		this.allowed = allowed;
	}



	@Override
	public int compareTo(SubCategory arg0) {
		try {
			if (arg0!=null && this.name!=null){
				return this.name.compareTo(arg0.name);
				
			} else {
				throw new Exception("Null Name " + arg0 +  " " + this);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
			return -1; 
			
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SubCategory){
			SubCategory s = (SubCategory)obj;
			return s.getName().equals(this.getName());
			
		} else {
			return false; 
			
		}
	}

	@Override
	public int hashCode() {
		return this.getName().hashCode();
		
	}
	
	
	

}
