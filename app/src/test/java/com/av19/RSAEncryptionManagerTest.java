package com.av19;

import com.av19.utils.RSAEncryptionManager;
import org.junit.Test;
import static org.junit.Assert.*;

public class RSAEncryptionManagerTest {

    @Test
    public void testEncryptionAndDecryption() {
        // Obtener la instancia del manager
        RSAEncryptionManager manager = RSAEncryptionManager.getInstance();

        // Mensaje de prueba
        String plaintext = "Hola, este es un mensaje de prueba";

        // Convertir la clave pública a String para usar en la encriptación
        String publicKeyStr = RSAEncryptionManager.publicKeyToString(manager.getPublicKey());

        // Encriptar el mensaje utilizando la clave pública en formato String
        String encryptedMessage = manager.encryptMessage(plaintext, publicKeyStr);
        assertNotNull("El mensaje encriptado no debe ser null", encryptedMessage);

        // Desencriptar el mensaje encriptado
        String decryptedMessage = manager.decryptMessage(encryptedMessage);
        assertNotNull("El mensaje desencriptado no debe ser null", decryptedMessage);

        // Verificar que el mensaje desencriptado coincide con el mensaje original
        assertEquals("El mensaje desencriptado debe coincidir con el original", plaintext, decryptedMessage);
    }
}
