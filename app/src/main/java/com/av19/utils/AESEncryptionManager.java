package com.av19.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESEncryptionManager {
    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS7Padding";
    private static final String PREFS_NAME = "AESKeys";

    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }

    public static void storeAESKey(Context context, String contactUsername, SecretKey aesKey) {
        String alias = contactUsername + "_AESKey";
        String encodedKey = Base64.getEncoder().encodeToString(aesKey.getEncoded());

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(alias, encodedKey).apply();
    }

    public static SecretKey getAESKey(Context context, String contactUsername) {
        String alias = contactUsername + "_AESKey";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String encodedKey = prefs.getString(alias, null);

        if (encodedKey == null) {
            throw new IllegalArgumentException("Doesn't exist an AES key for user: " + contactUsername);
        }

        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }

    public static String encryptText(String plainText, SecretKey secretKey) throws Exception {
        if (secretKey == null) {
            throw new IllegalArgumentException("Secret key cannot be null");
        }
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] iv = cipher.getIV();
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // Combinar IV y mensaje cifrado
        byte[] combined = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public static String decryptText(String encryptedText, SecretKey secretKey) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedText);

        // Extraer IV
        byte[] iv = new byte[16];
        System.arraycopy(combined, 0, iv, 0, iv.length);

        // Extraer mensaje cifrado
        byte[] encryptedBytes = new byte[combined.length - iv.length];
        System.arraycopy(combined, iv.length, encryptedBytes, 0, encryptedBytes.length);

        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
