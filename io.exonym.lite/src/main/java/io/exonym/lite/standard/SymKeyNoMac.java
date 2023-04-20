package io.exonym.lite.standard;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;


public class SymKeyNoMac implements Serializable{
	
	private static final Logger logger = LogManager.getLogger(SymKeyNoMac.class);
	private static final long serialVersionUID = 1L;
	private SecretKey key;
	
	public SymKeyNoMac() {
		try {
			KeyGenerator keyGen = KeyGenerator.getInstance(Const.SYM_ENCRYPTION_ALGORITHM);
			keyGen.init(128, new SecureRandom());
			this.key = keyGen.generateKey();	 

		} catch (Exception e) {
			logger.error("Error", e);

		}
		
	}
	
	public byte[] encrypt(byte[] raw) throws Exception{
		try {
			Cipher cipher = Cipher.getInstance(Const.SYM_ENCRYPTION_ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, key); //*/
			return cipher.doFinal(raw);
			
		} catch (Exception e) {
			throw e;  
			
		}
	}	
	
	public byte[] decipher(byte[] raw) throws Exception{	
		try {
			Cipher decipher = Cipher.getInstance(Const.SYM_ENCRYPTION_ALGORITHM);
			decipher.init(Cipher.DECRYPT_MODE, key);//*/
			return decipher.doFinal(raw);
				
		} catch (Exception e) {
			throw e; 
			
		}
	}		
}
