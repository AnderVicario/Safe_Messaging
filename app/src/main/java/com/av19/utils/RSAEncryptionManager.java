package com.av19.utils;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class RSAEncryptionManager {
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static RSAEncryptionManager instance;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    public String publicKeyString;

    public RSAEncryptionManager(Boolean createKeyPair, String myUsername) throws Exception {
        if (createKeyPair) {
            generateRSAKeyPair(myUsername);
        }
        else {
            loadPrivateKey(myUsername);
            loadPublicKey(myUsername);
        }
    }

    public static RSAEncryptionManager getInstance(Boolean createKeyPair, String myUsername) throws Exception {
        if (instance == null){
            instance = new RSAEncryptionManager(createKeyPair, myUsername);
        }
        return instance;
    }

    private void generateRSAKeyPair(String myUsername) throws Exception {
        String alias = myUsername + "_RSAKey";

        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);

        if (keyStore.containsAlias(alias)) {
            return; // Si ya existe, no se genera de nuevo
        }

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER);
        keyPairGenerator.initialize(new KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(2048)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .build());

        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();
        publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    private void loadPublicKey(String myUsername) throws Exception {
        String alias = myUsername + "_RSAKey";

        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);

        KeyStore.Entry entry = keyStore.getEntry(alias, null);
        if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
            return;
        }

        publicKey = ((KeyStore.PrivateKeyEntry) entry).getCertificate().getPublicKey();
        publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    private void loadPrivateKey(String myUsername) throws Exception {
        String alias = myUsername + "_RSAKey";

        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);

        KeyStore.Entry entry = keyStore.getEntry(alias, null);
        if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
            return;
        }

        privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
    }

    public String encryptAESKeyWithRSA(SecretKey aesKey, String publicKeyString) throws Exception {
        byte[] publicBytes = Base64.getDecoder().decode(publicKeyString);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedKey = cipher.doFinal(aesKey.getEncoded());
        return Base64.getEncoder().encodeToString(encryptedKey);
    }

    public SecretKey decryptAESKeyWithRSA(String encryptedAESKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decodedKey = cipher.doFinal(Base64.getDecoder().decode(encryptedAESKey));
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }
}
