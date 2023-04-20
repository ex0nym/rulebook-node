package io.exonym.exceptions;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="AssetLibrary")
@XmlType(name = "AssetLibrary", namespace = Namespace.EX)
public class UxExceptionMsgLibrary {

	public enum LANG { EN, DE }
	
	private LANG lang;
	

}
