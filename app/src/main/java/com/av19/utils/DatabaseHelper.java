package com.av19.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.ContentValues;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "contacts.db";
    private static final int DATABASE_VERSION = 1;
    private static volatile DatabaseHelper instance;
    private static SQLiteDatabase database;
    private Context context;
    // Alias distinto para la clave de cifrado de la contraseña
    private static final String DB_PASSWORD_ALIAS = "DBPasswordKey";
    // SharedPreferences para almacenar la contraseña cifrada
    private static final String PREFS_NAME = "db_prefs";
    private static final String PREF_PASSWORD = "db_password";
    private static final String PREF_PASSWORD_IV = "db_password_iv";

    private DatabaseHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        SQLiteDatabase.loadLibs(context.getApplicationContext());
        this.context = context.getApplicationContext();
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Guarda la contraseña de la base de datos cifrándola con una clave simétrica
     * almacenada en el Keystore bajo el alias DB_PASSWORD_ALIAS.
     */
    public static void setPassword(Context context, String password) {
        try {
            // Inicializar el KeyStore y cargarlo
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            SecretKey secretKey;

            // Si no existe la clave para la contraseña, se genera
            if (!keyStore.containsAlias(DB_PASSWORD_ALIAS)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
                keyGenerator.init(
                        new android.security.keystore.KeyGenParameterSpec.Builder(
                                DB_PASSWORD_ALIAS,
                                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT | android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                                .setKeySize(256)
                                .build()
                );
                secretKey = keyGenerator.generateKey();
            } else {
                KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(DB_PASSWORD_ALIAS, null);
                secretKey = entry.getSecretKey();
            }

            // Cifrar la contraseña con AES/GCM
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = cipher.getIV();
            byte[] ciphertext = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));

            // Almacenar el resultado cifrado y el IV en SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(PREF_PASSWORD, Base64.getEncoder().encodeToString(ciphertext))
                    .putString(PREF_PASSWORD_IV, Base64.getEncoder().encodeToString(iv))
                    .apply();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Recupera la contraseña almacenada cifrada y la utiliza para abrir la base de datos.
     */
    public synchronized SQLiteDatabase getEncryptedWritableDatabase() {
        if (database == null || !database.isOpen()) {
            try {
                // Recuperar la clave simétrica del Keystore
                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(DB_PASSWORD_ALIAS, null);
                SecretKey secretKey = entry.getSecretKey();

                // Recuperar la contraseña cifrada y el IV desde SharedPreferences
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String encryptedPassword = prefs.getString(PREF_PASSWORD, null);
                String ivString = prefs.getString(PREF_PASSWORD_IV, null);
                if (encryptedPassword == null || ivString == null) {
                    throw new IllegalStateException("Password not found in secure storage");
                }
                byte[] ciphertext = Base64.getDecoder().decode(encryptedPassword);
                byte[] iv = Base64.getDecoder().decode(ivString);

                // Descifrar la contraseña
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec spec = new GCMParameterSpec(128, iv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
                byte[] passwordBytes = cipher.doFinal(ciphertext);
                String password = new String(passwordBytes, StandardCharsets.UTF_8);

                database = super.getWritableDatabase(password);
            } catch (Exception e) {
                throw new RuntimeException("Failed to open database", e);
            }
        }
        return database;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE contacts (id INTEGER PRIMARY KEY, name TEXT, public_key TEXT, photo BLOB)");
        db.execSQL("CREATE TABLE messages (" +
                "id INTEGER PRIMARY KEY, " +
                "contact_id INTEGER, " +
                "is_sender BOOLEAN, " +
                "message TEXT, " +
                "sent_at TEXT, " +
                "FOREIGN KEY(contact_id) REFERENCES contacts(id)" +
                ")");
        insertSampleData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS messages");
        db.execSQL("DROP TABLE IF EXISTS contacts");
        onCreate(db);
    }

    public void insertSampleData(SQLiteDatabase db) {
        // Insertar contacto de prueba
        ContentValues contactValues = new ContentValues();
        contactValues.put("name", "Juan Pérez");
        contactValues.put("public_key", "clave_publica_dummy_12345");
        long contactId = db.insert("contacts", null, contactValues);

        // Insertar mensaje de prueba asociado al contacto
        ContentValues messageValues = new ContentValues();
        messageValues.put("contact_id", contactId);
        messageValues.put("is_sender", 1);
        messageValues.put("message", "¡Hola! Este es un mensaje de prueba");
        messageValues.put("sent_at", "2025-02-18T20:16:51.143Z");
        db.insert("messages", null, messageValues);
        // Otros inserts...
    }
}
