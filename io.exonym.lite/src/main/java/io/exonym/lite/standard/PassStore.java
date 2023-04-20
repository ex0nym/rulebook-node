package io.exonym.lite.standard;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;


import io.exonym.lite.exceptions.UxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class PassStore {

	private static final Logger logger = LogManager.getLogger(PassStore.class);
	private String username;
	private final String oracle;
	private final Cipher encrypt;
	private final Cipher decipher;

	
	public PassStore(String plainText, boolean validate) throws Exception {
		if (validate){
			validatePassword(plainText);
			
		}
		if (plainText==null){
			throw new NullPointerException();

		}
		String sha256 = null;
		if (plainText.length()<32){
			sha256 = CryptoUtils.computeSha256HashAsHex(plainText);

		} else {
			sha256 = plainText;

		}
		encrypt = CryptoUtils.generatePasswordCipher(Cipher.ENCRYPT_MODE, sha256, null);
		decipher = CryptoUtils.generatePasswordCipher(Cipher.DECRYPT_MODE, sha256, null);
		oracle = CryptoUtils.computeSha256HashAsHex(sha256);

	}
	
	/**
	 * Verify plain text password is the same as the plain text 
	 * password used to create these ciphers.
	 *  
	 * @param plainText
	 * @return
	 * @throws UxException
	 */
	public synchronized boolean verifyPassword(String plainText) throws UxException {
		String sha256 = CryptoUtils.computeSha256HashAsHex(plainText);
		sha256 = CryptoUtils.computeSha256HashAsHex(sha256);
		if (sha256.equals(oracle)){
			return true; 
			
		} else {
			throw new UxException("The passwords did not match.");
			
		}
	}
	
	/**
	 * Encrypt bytes with this password store.
	 * 
	 * @param bytes
	 * @return
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	public synchronized byte[] encrypt(byte[] bytes) throws IllegalBlockSizeException, BadPaddingException{
		if (bytes==null || bytes.length==0){
			throw new RuntimeException("You are trying to encrypt empty bytes.");
			
		}
		return encrypt.doFinal(bytes);
		
	}
	
	public synchronized Cipher getEncrypt() {
		return encrypt;
	}

	public synchronized Cipher getDecipher() {
		return decipher;
	}

	/**
	 * Decipher bytes with this password store.
	 * 
	 * @param bytes
	 * @return
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	public synchronized byte[] decipher(byte[] bytes) throws IllegalBlockSizeException, UxException{
		try {
			if (bytes==null || bytes.length==0){
				logger.warn("You are trying to decipher empty bytes.");
				return null; 
				
			}
			return decipher.doFinal(bytes);

		} catch (BadPaddingException e) {
			throw new UxException("Username or Password was incorrect!");
			
		}
	}

	public synchronized String getUsername() {
		return username;
	}

	public synchronized void setUsername(String username) {
		this.username = username;
	}

	public synchronized  void validatePassword(String password) throws UxException{
		boolean valid = true; 
		
		if (!WhiteList.containsLowerCaseLetters(password)){
			valid = false; 
			
		} if (!WhiteList.containsNumbers(password)){
			valid = false; 
			
		} if (!WhiteList.isMinLettersAllowsNumbers(password, 7)){
			valid = false; 
			
		} if (password.length() < 14 && !WhiteList.containsUpperCaseLetters(password)){
			valid = false; 

		} if (!valid){
			throw new UxException("Password must be at least 7 characters, contain "
					+ "upper and lower case letters, and at least one number.");
			
		}
	}
	
	public static AsymStoreKey assembleKeyPair(String password, byte[] publicKey, byte[] privateKey) throws Exception{
		AsymStoreKey key = AsymStoreKey.blank();
		PassStore store = new PassStore(password, false);
		key.assembleKey(publicKey, privateKey, store.getDecipher());
		return key; 
		
	}
}
