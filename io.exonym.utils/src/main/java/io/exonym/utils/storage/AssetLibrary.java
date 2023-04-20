package io.exonym.utils.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


import io.exonym.abc.util.JaxbHelper;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.Namespace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@XmlRootElement(name="AssetLibrary")
@XmlType(name = "AssetLibrary", namespace = Namespace.EX)
public final class AssetLibrary {
	
	/*
	 * Singlton Object
	 */
	private final static AssetLibrary instance;
	
	private static final Logger logger = LogManager.getLogger(AssetLibrary.class);
	private AssetLibrary() {}
	static {
		AssetLibrary tmp = null; 
		try {
			/*
			 * Stub for the couchdb contents
			 */
			File file = new File("resource\\asset-library.json"); 
			if (file.exists()){
				FileInputStream fis = new FileInputStream(file);
				byte[] in = new byte[fis.available()];
				fis.read(in);
				fis.close();
				String json = new String(in, "UTF-8");
				tmp = JaxbHelper.jsonToClass(json, AssetLibrary.class);
				
			} else {
				tmp = new AssetLibrary();
				
			}
		} catch (Exception e) {
			logger.error("Error", e);
			
		} 
		instance = tmp;
		
		if (instance == null){
			logger.error("Programming error: instance is null");
			
		}
	}
	
	/**
	 * The asset library is a singleton object
	 * 
	 * @return
	 */
	public synchronized static AssetLibrary getInstance(){
		return instance; 
		
	}
	
	@XmlElement(name="DemUid", namespace=Namespace.EX)
	private URI demUid;
	
	@XmlElement(name="Credentials", namespace=Namespace.EX)
	private HashSet<Asset> credentials = new HashSet<>();
	
	@XmlElement(name="IssuerKeys", namespace=Namespace.EX)
	private HashSet<Asset> issuerKeys = new HashSet<>();
	
	@XmlElement(name="InspectorKeys", namespace=Namespace.EX)
	private HashSet<Asset> inspectorKeys = new HashSet<>();
	
	@XmlElement(name="RevocationKeys", namespace=Namespace.EX)
	private HashSet<Asset> revocationKeys = new HashSet<>();
	
	@XmlElement(name="IndexLedgerKeys", namespace=Namespace.EX)
	private HashSet<Asset> indexLegerKeys = new HashSet<>();

	public synchronized void addAsset(Asset asset) throws Exception {
		if (asset.getAssetType().equals(Asset.TYPE.CREDENTIAL)){
			this.credentials.add(asset);
			
		} else if (asset.getAssetType().equals(Asset.TYPE.INDEX_LEDGER_KEY)){
			this.indexLegerKeys.add(asset);
			
		} else if (asset.getAssetType().equals(Asset.TYPE.INSPECTOR_KEY)){
			this.inspectorKeys.add(asset);
			
		} else if (asset.getAssetType().equals(Asset.TYPE.ISSUER_KEY)){
			this.issuerKeys.add(asset);
			
		} else if (asset.getAssetType().equals(Asset.TYPE.REVOCATION_KEY)){
			this.revocationKeys.add(asset);
			
		} else {
			throw new RuntimeException("Could not add asset to library " + asset.toString());
			
		}
	}
	
	public synchronized void addDescription(){
		
	}
	
	public synchronized void archiveAsset(URI assetUid, boolean archive) throws Exception {
		Asset a = findAppropriateAsset(assetUid);
		if (a!=null){
			a.setArchived(archive);
			
		} else {
			throw new UxException(String.format("Unable to archive asset with UID %s. The asset cannot be found.", assetUid));
			
		}
	}
	
	private synchronized Asset findAppropriateAsset(URI assetUid){
		HashSet<Asset> set = findAppropriateSet(assetUid);
		for (Asset a : set){
			if (assetUid.equals(a.getAssetUid())){
				return a;
				
			}
		}
		logger.warn("Failed to archive uid " + assetUid + " from set " + set);
		return null; 
		
	} 
	
	private synchronized HashSet<Asset> findAppropriateSet(URI uid){
		if (credentials.contains(uid)){
			return credentials;
			
		} else if (issuerKeys.contains(uid)){
			return issuerKeys;
			
		} else if (inspectorKeys.contains(uid)){
			return inspectorKeys;
			
		} else if (revocationKeys.contains(uid)){
			return revocationKeys;
			
		} else if (indexLegerKeys.contains(uid)){
			return indexLegerKeys;
			
		} else {
			return null; 
			
		}
	}
	
	private void setArchiveValue(HashSet<Asset> set, URI assetUid, boolean archive) {

		
	}

	/**
	 * An asset compares on UID <b>only</b> therefore you can create an 
	 * asset with only a uid (full urn notation) and request whether or
	 * not the library has it. 
	 *  
	 * @param asset
	 * @return
	 * 
	 */
	public synchronized boolean isAvailable(Asset asset){
		if (credentials.contains(asset)){
			return true;
			
		} else if (issuerKeys.contains(asset)){
			return true;
			
		} else if (inspectorKeys.contains(asset)){
			return true;
			
		} else if (revocationKeys.contains(asset)){
			return true;
			
		} else if (indexLegerKeys.contains(asset)){
			return true;
			
		} else {
			return false;
			
		}
	}
	
	public synchronized ArrayList<Asset> getAllAssets(){
		ArrayList<Asset> result = new ArrayList<>();
		result.addAll(credentials);
		result.addAll(issuerKeys);
		result.addAll(inspectorKeys);
		result.addAll(revocationKeys);
		result.addAll(indexLegerKeys);
		return result;

	}
	
	/**
	 * Stub method to simulate commitment to CouchDb.  
	 * In addition to this Asset Library, the value should be 
	 * hashed and signed by this instance of the Dem so that
	 * editing causes the DEM to fail with error. This is 
	 * so that if someone edits this file, delegates will not be
	 * bothered with votes that can't carry because one or 
	 * more references are incorrect. 
	 * 
	 * @throws Exception
	 */
	public synchronized void commit() throws Exception {
		File file = new File("resource\\asset-library.json");
		try (FileOutputStream fos = new FileOutputStream(file)){
			String json = JaxbHelper.serializeToJson(this, AssetLibrary.class);
			fos.write(json.getBytes());
			
		} catch (Exception e) {
			logger.error("Error", e);
			
		}
	}
}