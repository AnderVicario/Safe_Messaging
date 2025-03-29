package com.av19.utils;

import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;

import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AESEncryptionManager {
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS7Padding";

    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }

    public static void storeAESKey(String contactUsername, SecretKey aesKey) throws Exception {
        String alias = contactUsername + "_AESKey";

        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);

        KeyProtection protectionParams = new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC) // Coincide con AES/CBC/PKCS5Padding
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .build();

        KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(aesKey);

        keyStore.setEntry(alias, secretKeyEntry, protectionParams);
    }

    public static SecretKey getAESKey(String contactUsername) throws Exception {
        String alias = contactUsername + "_AESKey";

        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);

        KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(alias, null);
        return entry.getSecretKey();
    }

    public static String encryptText(String plainText, SecretKey secretKey) throws Exception {
        if (secretKey == null) {
            throw new IllegalArgumentException("Secret key cannot be null");
        }
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] iv = cipher.getIV();
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());

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

        return new String(decryptedBytes);
    }
}
