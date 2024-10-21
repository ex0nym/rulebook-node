package io.exonym.lite.standard;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Formatter;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

public class CryptoUtils {
	
	private static final Logger logger = LogManager.getLogger(CryptoUtils.class);

	public static String computeMd5HashAsHex(String string){
		return computeMd5HashAsHex(string.getBytes(StandardCharsets.UTF_8));

	}

	public static String computeMd5HashAsHex(byte[] bytes){
			try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(bytes);
			return byteArrayToHex(digest);

		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);

		}
	}

	public static String byteArrayToHex(final byte[] hash) {
		Formatter formatter = new Formatter();
		for (byte b : hash) {
			formatter.format("%02x", b);
		}
		String result = formatter.toString();
		formatter.close();
		return result;
	}

	public static String computeSha256HashAsHex(String string)  {
		try {
			return computeSha256HashAsHex(string.getBytes("UTF-8"));
			
		} catch (Exception e) {
			logger.error("error", e);
			return null; 
			
		} 
	}

	public static String computeSha256HashAsHex(byte[] bytes) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(bytes);
			byte[] digest = md.digest();
			BigInteger bigI = new BigInteger(1, digest);
			return String.format("%064x", bigI);

		} catch (NoSuchAlgorithmException e) {
			return null;

		}
	}

	public static byte[] computeSha256HashAsBytes(byte[] bytes) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(bytes);
			byte[] digest = md.digest();
			BigInteger bigI = new BigInteger(1, digest);
			return bigI.toByteArray();

		} catch (NoSuchAlgorithmException e) {
			return null;

		}
	}

	public static String toHex(byte[] bytes){
		BigInteger bigI = new BigInteger(1, bytes);
		return String.format("%064x", bigI);

	}
	
	public static BigInteger computeSha256HashAsBigInteger(byte[] bytes) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(bytes);
			byte[] digest = md.digest();
			return new BigInteger(1, digest);
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;

		}
	}
	
	public static BigInteger randomNumberGreaterThanOne(int bitSize, BigInteger order, SecureRandom rnd){
		BigInteger r = new BigInteger(bitSize, rnd).mod(order);
		while (r.equals(BigInteger.ZERO) || r.equals(BigInteger.ONE)){
			r = new BigInteger(bitSize, rnd).mod(order);
			
		}
		return r;
		
	}
	
	public static int[] gcd(int p, int q) {
		if (q == 0){
		   return new int[] { p, 1, 0 };
		   
		}
		int[] vals = gcd(q, p % q);
		int d = vals[0];
		int a = vals[2];
		int b = vals[1] - (p / q) * vals[2];
		return new int[] { d, a, b };
		
	}
	
	public static byte[] generateNonce(int size){
		try {
			byte[] randomNonce = new byte[size];
			SecureRandom rnd = SecureRandom.getInstance("NativePRNG");
			rnd.setSeed(rnd.generateSeed(4));
			rnd.nextBytes(randomNonce);
			return randomNonce;

		} catch (NoSuchAlgorithmException e) {
			logger.error("--------- Critical Error ---------- ", e);
			return null;

		}
	}

	public static String tempPassword(){
		return tempPassword(6);
	}

	public static String tempPassword(int length){
		return Base64.encodeBase64String(CryptoUtils.generateNonce(length));

	}
	
	public static String generateCode(int length) throws RuntimeException {
		String code = generateCodeWithSpace(length);
		return code.replaceAll(" ", "");
		
	}

	
	public static String generateCodeWithSpace(int length) throws RuntimeException {
		if (length%2!=0){
			throw new RuntimeException("The length must be even.");
			
			
		}
		SecureRandom sr = new SecureRandom();
		String code = ""; 
		int lengthPlusOne = length + 1; 
		while (code.length() < lengthPlusOne){
			code += sr.nextInt(9);
			if (code.length()==(length/2)){
				code += " ";
				
			}
		}
		return code;
		
	}	
	
	public static BigInteger sqrt(BigInteger x) {
	    BigInteger div = BigInteger.ZERO.setBit(x.bitLength()/2);
	    BigInteger div2 = div;
	    while(true) {
	        BigInteger y = div.add(x.divide(div)).shiftRight(1);
	        if (y.equals(div) || y.equals(div2))
	            return y;
	        div2 = div;
	        div = y;
	    }
	}	
	
	public static Cipher generatePasswordCipher(int cipherMode, String password, byte[] salt) throws Exception {
        PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator();
        generator.init(PBEParametersGenerator.PKCS5PasswordToBytes(password.toCharArray()),
													                salt,
													                Const.SYM_KEY_SIZE);
        
        KeyParameter cipherParam = (KeyParameter) generator.generateDerivedParameters(Const.SYM_KEY_SIZE);
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(cipherMode, new SecretKeySpec(cipherParam.getKey(), Const.SYM_ENCRYPTION_ALGORITHM));
        return cipher;

	}	
}
