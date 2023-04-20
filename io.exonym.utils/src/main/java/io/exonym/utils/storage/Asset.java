package io.exonym.utils.storage;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import eu.abc4trust.xml.FriendlyDescription;
import io.exonym.lite.pojo.Namespace;

@XmlType(name="Asset", namespace = Namespace.EX, propOrder = {"assetType", "assetUid", "description", "archived"})
public class Asset implements Comparable<Asset>, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 14L;

	public enum TYPE {CREDENTIAL, ISSUER_KEY, INSPECTOR_KEY, REVOCATION_KEY, INDEX_LEDGER_KEY};
	private URI assetUid;
	private TYPE assetType;
	private Collection<FriendlyDescription> description;
	private boolean archived = false; 
	
	public Asset() {}
	
	public void addAnotherDescription(String description, String lang){
    	FriendlyDescription d = new FriendlyDescription();
    	d.setLang(lang);
    	d.setValue(description);
    	if (description==null){
    		this.description = new ArrayList<>();
    		
    	}
    	this.description.add(d);
		
	}

	public void setAssetUid(URI assetUid) {
		this.assetUid = assetUid;

	}

	public void setAssetType(TYPE assetType) {
		this.assetType = assetType;
		
	}

	public void setDescription(Collection<FriendlyDescription> description) {
		this.description = description;
		
	}

	@XmlElement(name="AssetUID", namespace=Namespace.EX)
	public URI getAssetUid() {
		return assetUid;
		
	}

	@XmlElement(name="AssetType", namespace=Namespace.EX)
	public TYPE getAssetType() {
		return assetType;
	}

	@XmlElement(name="Description", namespace=Namespace.EX)
	public Collection<FriendlyDescription> getDescription() {
		return description;
	}
	
	@XmlElement(name="Archived", namespace=Namespace.EX)
	public boolean isArchived() {
		return archived;
		
	}

	public void setArchived(boolean archived) {
		this.archived = archived;
	}
	
	
	
	public String toString(){
		return this.assetType + " " + this.assetUid;
		
	}
	
	public static Asset createAsset(URI uid, Asset.TYPE type, String description, String lang){
    	Asset asset = new Asset();
    	asset.setAssetType(type);
    	asset.setAssetUid(uid);
    	FriendlyDescription d = new FriendlyDescription();
    	d.setLang(lang);
    	d.setValue(description);
    	ArrayList<FriendlyDescription> list = new ArrayList<>();
    	list.add(d);
    	asset.setDescription(list);
		return asset;
		
	}

	@Override
	public int compareTo(Asset o) {
		return this.assetUid.compareTo(o.assetUid);
		
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Asset){
			Asset a = (Asset)o;
			return a.assetUid.equals(this.assetUid);
			
		} else if (o instanceof URI){
			return this.assetUid.equals(o);
			
		} else {
			return false;
			
		}
	}
	
	@Override
	public int hashCode() {
		return this.assetUid.hashCode();

	}
}
