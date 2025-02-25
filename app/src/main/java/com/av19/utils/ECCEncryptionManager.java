package com.av19.utils;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

public class ECCEncryptionManager {
    private static ECCEncryptionManager instance;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private String publicKeyString;

    private ECCEncryptionManager(Boolean createKeyPair, String username) throws Exception{
        if (createKeyPair) {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC,
                    "AndroidKeyStore"
            );

            keyPairGenerator.initialize(
                    new KeyGenParameterSpec.Builder(
                            username + "Keys",
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
                    )
                            .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1")) // Curva estándar
                            .setDigests(KeyProperties.DIGEST_SHA256)
                            .build()
            );
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
            publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        }
        else {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyStore.Entry entry = keyStore.getEntry(username + "Keys", null);

            if (entry instanceof KeyStore.PrivateKeyEntry) {
                KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) entry;

                privateKey = privateKeyEntry.getPrivateKey();
                publicKey = privateKeyEntry.getCertificate().getPublicKey();
                publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());

            } else {
                throw new Exception("No se encontraron claves para: " + username);
            }
        }
    }

    public static ECCEncryptionManager getInstance(Boolean createKeyPair, String username) throws Exception {
        if (instance == null){
            instance = new ECCEncryptionManager(createKeyPair, username);
        }
        return instance;
    }

    public String getPublicKeyString() {
        return publicKeyString;
    }

    public String encrypt(String message, String publicKeyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyStr);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC", "AndroidOpenSSL");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        Cipher cipher = Cipher.getInstance("ECIESwithAES-CBC/NONE/NoPadding", "AndroidOpenSSL");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public String decrypt(String encryption) throws Exception {
        byte[] encryptedBytes = Base64.getDecoder().decode(encryption);
        Cipher cipher = Cipher.getInstance("ECIESwithAES-CBC/NONE/NoPadding", "AndroidOpenSSL");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
