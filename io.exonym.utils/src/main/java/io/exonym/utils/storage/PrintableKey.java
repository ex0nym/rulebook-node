package io.exonym.utils.storage;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name="printable-public-key")
public class PrintableKey {
	
	private String header; 
	private ArrayList<String> paragraph;
	private byte[] qr;

	public ArrayList<String> getParagraph() {
		return paragraph;
		
	}

	public void setParagraph(ArrayList<String> paragraph) {
		this.paragraph = paragraph;
		
	}

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;

	}

	public byte[] getQr() {
		return qr;
	
	}

	public void setQr(byte[] qr) {
		this.qr = qr;
	
	}
}
