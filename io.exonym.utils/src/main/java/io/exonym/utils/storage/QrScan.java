package io.exonym.utils.storage;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import org.apache.commons.codec.binary.Base64;

public class QrScan {
	
	public static PublicKey recoverPublicKey(String qrScan) throws Exception{
		try {
			byte[] encoded = Base64.decodeBase64(qrScan.getBytes());
			return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
			
		} catch (Exception e) {
			throw e; 
			
		}
	}	
}
