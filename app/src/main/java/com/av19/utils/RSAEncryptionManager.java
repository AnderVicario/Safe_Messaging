package com.av19.utils;
import javax.crypto.Cipher;
import java.security.*;
import java.util.Base64;
import java.util.HashMap;

public class RSAEncryptionManager {
    private static RSAEncryptionManager instance;
    private KeyPair keyPair;  // Claves del usuario
    private HashMap<String, PublicKey> contactKeys; // Claves públicas de contactos

    private RSAEncryptionManager() {
        try {
            // Generar claves RSA
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            this.keyPair = keyGen.generateKeyPair();
            this.contactKeys = new HashMap<>();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static RSAEncryptionManager getInstance() {
        if (instance == null) {
            instance = new RSAEncryptionManager();
        }
        return instance;
    }

    // Obtener clave pública en Base64
    public String getPublicKey() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    // Obtener clave privada en Base64
    private String getPrivateKey() {
        return Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
    }

    // Agregar clave pública de un contacto
    public void addContactKey(String contactName, PublicKey publicKey) {
        contactKeys.put(contactName, publicKey);
    }

    // Obtener clave pública de un contacto
    public PublicKey getContactKey(String contactName) {
        return contactKeys.get(contactName);
    }

    // Encriptar mensaje con clave pública del destinatario
    public String encryptMessage(String message, String contactName) {
        try {
            PublicKey contactKey = contactKeys.get(contactName);
            if (contactKey == null) {
                throw new Exception("Clave pública del contacto no encontrada");
            }
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, contactKey);
            byte[] encryptedBytes = cipher.doFinal(message.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Desencriptar mensaje con clave privada del usuario
    public String decryptMessage(String encryptedMessage) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedMessage));
            return new String(decryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Simular envío de clave pública a la API
    public void sendPublicKeyToAPI() {
        System.out.println("Enviando clave pública a la API: " + getPublicKey());
        // Aquí iría la lógica para enviar la clave pública a la API
    }

    // Simular solicitud de clave pública de un contacto desde la API
    public void requestContactKeyFromAPI(String contactName, String contactPublicKeyBase64) {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(contactPublicKeyBase64);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(new java.security.spec.X509EncodedKeySpec(publicKeyBytes));
            addContactKey(contactName, publicKey);
            System.out.println("Clave pública de " + contactName + " almacenada.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

