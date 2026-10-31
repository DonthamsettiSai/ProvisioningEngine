package com.knot.provisioning;

import java.nio.charset.StandardCharsets;
import java.security.ProviderException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;



public class CryptoConfiguration {
	
	private static final String UNAUTHORIZED_REQUEST = "Unauthorized Request";

	private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
	
	private static String keyBytes="35d610649b2a8fe162b80cf049dfd1eb";
	
	private static String ivKeyBytes="35d610649b2a8fe1";
	private CryptoConfiguration()
	{
		
	}
	public static String encrypt(String encrypt) {
		String str="";
		try {
			SecretKeySpec keySpec = new SecretKeySpec(keyBytes.getBytes(), 0, 32, "AES");
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			IvParameterSpec ivSpec = new IvParameterSpec(ivKeyBytes.getBytes());
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
			byte[] encryptedBytes = cipher.doFinal(encrypt.getBytes());
			str=java.util.Base64.getEncoder().encodeToString(encryptedBytes);
		} catch (Exception e) {
			throw new ProviderException(UNAUTHORIZED_REQUEST);
		}
		return str;
	}

	public static String decrypt(String encrypt) {
		String decrypt = "";
		try {
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			SecretKeySpec key = new SecretKeySpec(keyBytes.getBytes(StandardCharsets.UTF_8), "AES");
			IvParameterSpec ivSpec = new IvParameterSpec(ivKeyBytes.getBytes(StandardCharsets.UTF_8));
			cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
			byte[] decryptedBytes = cipher.doFinal(Base64.decodeBase64(encrypt));
			decrypt = new String(decryptedBytes);
		} catch (Exception e) {
			throw new ProviderException(UNAUTHORIZED_REQUEST);
		}
		return decrypt;
	}
	
	public static byte[] decryptByte(byte[] encrypt) {
		byte[] decryptedBytes=null;
		try {
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			SecretKeySpec key = new SecretKeySpec(keyBytes.getBytes(StandardCharsets.UTF_8), "AES");
			IvParameterSpec ivSpec = new IvParameterSpec(ivKeyBytes.getBytes(StandardCharsets.UTF_8));
			cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
			decryptedBytes = cipher.doFinal(encrypt);
		} catch (Exception e) {
			throw new ProviderException(UNAUTHORIZED_REQUEST);
		}
		return decryptedBytes;
	}
}
