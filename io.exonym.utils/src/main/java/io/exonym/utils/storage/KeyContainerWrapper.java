package io.exonym.utils.storage;

import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.XKey;
import io.exonym.lite.time.DateHelper;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.SymStoreKey;
import io.exonym.lite.standard.PassStore;
import io.exonym.utils.Morph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public final class KeyContainerWrapper {
	

	private static final Logger logger = LogManager.getLogger(KeyContainerWrapper.class);
	private HashMap<URI, XKey> keyRing = new HashMap<>();
	private HashMap<URI, HashMap<URI, XKey>> dynamicKeyRing = new HashMap<>();
	private KeyContainer keyContainer;
	private ConnectKeyContainer connectKeyContainer;

	// AsymStoreKey
	public static final URI SIGN_VOTE_PARTS =
			URI.create("urn:keys:rsa:sign:vote-share");

	// todo store the key with the UID of the node.
	// Duplicates can happen when testing and they cause hellish 'bugs'.
	public static final URI TN_ROOT_KEY =
			URI.create("urn:keys:rsa:trust-network-root");

	/*
	 *  SymStoreKey
	 */
	public static final URI BOOT_KEY = URI.create("urn:keys:aes:boot");
	public static final URI ASSEMBLE_VOTE_PARTS = URI.create("urn:keys:aes:assemble-vote-share");
	public static final URI COMM_PREFIX = URI.create("urn:keys:aes:comms");
	public static final URI SIG_CHECKSUM = URI.create("urn:sig-checksum");
	
	private static final ArrayList<URI> prefixList = new ArrayList<>();
	
	/**
	 * 
	 */
	public KeyContainerWrapper(KeyContainer container) {
		prefixList.add(COMM_PREFIX);
		this.keyContainer=container;
		openKeySet(container.getKeyPairs());
		
	}
	
	public KeyContainerWrapper(ConnectKeyContainer container) {
		prefixList.add(COMM_PREFIX);
		this.connectKeyContainer = container;
		openKeySet(container.getKeyPairs());
		
	}
	
	private void openKeySet(ArrayList<XKey> keys){
		for (XKey key: keys){
			URI d = dynamicUri(key.getKeyUid());
			if (d==null){
				keyRing.put(key.getKeyUid(), key);
				
			} else {
				HashMap<URI, XKey> map = dynamicKeyRing.get(d);
				if (map==null){
					map = new HashMap<URI, XKey>();
					dynamicKeyRing.put(d, map);
					
				}
				map.put(key.getKeyUid(), key);
				
			}
		}
	}
	
	private URI dynamicUri(URI target){
		String fullUri = target.toString();
		for (URI prefix: prefixList){
			if (fullUri.startsWith(prefix.toString())){
				return prefix;
				
			}
		}
		return null;
		
	}
	

	public void addKey(XKey key) throws Exception{
		if (keyRing.containsKey(key.getKeyUid())){
			throw new UxException("Key Uid already exists " + key.getKeyUid());
			
		}
		updateKey(key);
		
	}
	
	public void updateKey(XKey key) throws Exception{
		if (key.getKeyUid()==null){
			throw new UxException("Uid was null");
			
		}
		if (this.keyContainer!=null){
			if (keyRing.containsKey(key.getKeyUid())){
				keyRing.remove(key.getKeyUid());
				keyContainer.getKeyPairs().remove(key);	
				
			}
			this.keyContainer.getKeyPairs().add(key);	
			
		} else if (this.connectKeyContainer!=null){
			String today = DateHelper.currentIsoUtcDate();
			if (!today.equals(this.connectKeyContainer.getUtcDate())){
				this.connectKeyContainer = ConnectKeyContainer.make(this.connectKeyContainer.getDeviceUid());
				
			} 
			this.connectKeyContainer.getKeyPairs().add(key);
			
		} else {
			throw new Exception("Not correctly initialized.");
			
		}
		URI d = dynamicUri(key.getKeyUid());
		if (d==null){
			this.keyRing.put(key.getKeyUid(), key);
			
		} else {
			HashMap<URI, XKey> map = this.dynamicKeyRing.get(d);
			if (map==null){
				map = new HashMap<URI, XKey>();
				this.dynamicKeyRing.put(d, map);
				
			}
			map.put(key.getKeyUid(), key);
			
		}
	}

	public XKey getKey(URI name) throws Exception{
		URI d = dynamicUri(name);
		if (d==null){
			return keyRing.get(name);
			
		} else {
			HashMap<URI, XKey> map = this.dynamicKeyRing.get(d);
			if (map!=null){
				return map.get(name);
				
			} else {
				return null;
				
			}
		}
	}
	
	//TODO efficiency
	public XKey getMostRecentKey(URI prefix) throws Exception{
		URI d = dynamicUri(prefix);
		if (d!=null){
			HashMap<URI, XKey> map = this.dynamicKeyRing.get(d);
			
			if (map!=null){
				long latest = 0L;
				for (URI uid: map.keySet()){
					String[] split = uid.toString().split(":");
					long stamp = Long.parseLong(split[split.length-1]);
					if (stamp > latest){
						latest = stamp;
						
					}
				}
				return map.get(URI.create(prefix.toString() + ":" + latest));
				
			} else {
				return null;
				
			}
		} else {
			return null;

		}
	}
	
	
	public ArrayList<XKey> getAllKeysOfPrefix(URI fullOrPrefix){
		ArrayList<XKey> result = new ArrayList<>();
		URI d = dynamicUri(fullOrPrefix);
		if (d!=null){
			HashMap<URI, XKey> map = this.dynamicKeyRing.get(d);
			if (map != null){
				 for (URI uid : map.keySet()){
					 result.add(map.get(uid));
					 
				 }
			}
			return result;

		} else {
			return null;
			
		}
	}

	public KeyContainer getKeyContainer() {
		keyContainer.setLastUpdateTime(DateHelper.currentIsoUtcDateTime());
		return keyContainer;
		
	}
	
	public ConnectKeyContainer getConnectKeyContainer() {
		return connectKeyContainer;
		
	}
	
	public static KeyContainer createDefaultKeySet(PassStore pass) throws Exception{
		KeyContainer k = new KeyContainer();
		Morph<SymStoreKey> m = new Morph<>();
		try {
			AsymStoreKey k0 = new AsymStoreKey();
			XKey kp0 = new XKey();
			kp0.setKeyUid(ASSEMBLE_VOTE_PARTS);
			kp0.setPrivateKey(pass.encrypt(k0.getEncryptedEncodedForm(pass.getEncrypt())));
			kp0.setPublicKey(k0.getPublicKey().getEncoded());
			
			SymStoreKey k1 = new SymStoreKey();
			XKey kp1 = new XKey();
			kp1.setKeyUid(SIGN_VOTE_PARTS);
			kp1.setPrivateKey(pass.encrypt(m.toByteArray(k1)));
			
			SymStoreKey k2 = new SymStoreKey();
			XKey kp2 = new XKey();
			kp2.setKeyUid(BOOT_KEY);
			kp2.setPrivateKey(pass.encrypt(m.toByteArray(k2)));
			
			k.getKeyPairs().add(kp0);
			k.getKeyPairs().add(kp1);
			k.getKeyPairs().add(kp2);
			
		} catch (Exception e) {
			throw e; 
			
		}
		return k;
		
	}
	
	public Set<URI> getKeyRingUids(){
		return this.keyRing.keySet();
		
	}

	public Set<URI> getDynamicKeyRingUids(){
		return this.dynamicKeyRing.keySet();
		
	}

	
	public static XKey wrapKeysForStorage(SymComKeys keys, PassStore store) throws Exception{
		if (keys==null || store==null){
			throw new RuntimeException("Error defining keys");
			
		}
		Morph<SymStoreKey> m = new Morph<>();
		long ms = System.currentTimeMillis();
		XKey kp = new XKey();
		kp.setKeyIn(store.encrypt(m.toByteArray(keys.getKeyIn())));
		kp.setKeyOut(store.encrypt(m.toByteArray(keys.getKeyOut())));
		kp.setKeyUid(URI.create(COMM_PREFIX.toString() + ":" + ms));
		return kp;
		
	}
	
	public static SymComKeys unwrapCommKeys(XKey key, PassStore store) throws Exception{
		Morph<SymStoreKey> m = new Morph<>();
		SymComKeys result = new SymComKeys();
		result.setKeyIn(m.construct(store.decipher(key.getKeyIn())));
		result.setKeyOut(m.construct(store.decipher(key.getKeyOut())));
		
		return result;
		
	}
}
