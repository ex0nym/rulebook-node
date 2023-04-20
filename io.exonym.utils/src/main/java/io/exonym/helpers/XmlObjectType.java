package io.exonym.helpers;

public class XmlObjectType {
	
	private Class<?> clazz = null;
	private boolean exonym = false;
	
	public Class<?> getClazz() {
		return clazz;
	}
	public void setClazz(Class<?> clazz) {
		this.clazz = clazz;
	}
	public boolean isExonym() {
		return exonym;
	}
	public void setExonym(boolean existence) {
		this.exonym = existence;
	}

}
