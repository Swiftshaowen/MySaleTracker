package com.ape.saletracker;

import android.util.Base64;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class RSAHelper {

	private static final String ALGO = "RSA";
	private static final String TRANSFORMATION = ALGO + "/ECB/PKCS1Padding";
	private static final int BASE64_FLAGS = Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP;
	
	public static PublicKey getPublicKey(String key) throws Exception {
		byte[] keyBytes;
		keyBytes = Base64.decode(key, BASE64_FLAGS);

		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance(ALGO);
		PublicKey publicKey = keyFactory.generatePublic(keySpec);
		return publicKey;
	}

	public static PrivateKey getPrivateKey(String key) throws Exception {
		byte[] keyBytes;
		keyBytes = Base64.decode(key, BASE64_FLAGS);

		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance(ALGO);
		PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
		return privateKey;
	}

	public static String getKeyString(Key key) throws Exception {
		byte[] keyBytes = key.getEncoded();
		String s = Base64.encodeToString(keyBytes, BASE64_FLAGS);
		return s;
	}

	// 加密
	public static String encrypt(String key, String cleartext) throws Exception {
		PublicKey pubKey = getPublicKey(key);
		Cipher cipher = Cipher.getInstance(TRANSFORMATION);
		
		cipher.init(Cipher.ENCRYPT_MODE, pubKey);
		byte[] result = cipher.doFinal(cleartext.getBytes("utf-8"));

		return Base64.encodeToString(result, BASE64_FLAGS);
	}

	// 解密
	public static String decrypt(String key, String encrypted) throws Exception {
		PrivateKey prvKey = getPrivateKey(key);
		Cipher cipher = Cipher.getInstance(TRANSFORMATION);

		cipher.init(Cipher.DECRYPT_MODE, prvKey);
		byte[] result = cipher.doFinal(Base64.decode(encrypted, BASE64_FLAGS));

		return new String(result);
	}

	public static String[] genKeyPair() throws Exception {

		KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
		// 密钥位数
		keyPairGen.initialize(1024);
		// 密钥对
		KeyPair keyPair = keyPairGen.generateKeyPair();

		// 公钥
		PublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

		// 私钥
		PrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
		
		String[] result = new String[2];
		result[0] = getKeyString(publicKey);
		result[1] = getKeyString(privateKey);
		
		return result;
	}
}