package io.exonym.rulebook.schema;

public class Administrator {

	private String clazz = "list-button-delete";
	private String username;
	private String id; 
	private String tel;

	public String getClazz() {
		return clazz;
	}

	public void setClazz(String clazz) {
		this.clazz = clazz;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTel() {
		return tel;
	}

	public void setTel(String tel) {
		this.tel = tel;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Administrator) {
			Administrator in = (Administrator)obj; 
			return in.getUsername().equals(this.username);
			
		} else {
			return false; 
			
		}
	}

	@Override
	public int hashCode() {
		return this.username.hashCode();
		
	}
	
	
	

}
