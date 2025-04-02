package com.av19.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final Map<String, DatabaseHelper> instances = new HashMap<>();
    private static SQLiteDatabase database;
    private final Context context;
    private final String databaseName;
    private final String dbPasswordAlias;
    private final String prefPasswordKey;
    private final String prefIvKey;

    private DatabaseHelper(Context context, String userId) {
        super(context.getApplicationContext(), "contacts_" + userId + ".db", null, DATABASE_VERSION);
        SQLiteDatabase.loadLibs(context.getApplicationContext());
        this.context = context.getApplicationContext();
        this.databaseName = "contacts_" + userId + ".db";
        this.dbPasswordAlias = "DBPasswordKey_" + userId;
        this.prefPasswordKey = "db_password_" + userId;
        this.prefIvKey = "db_password_iv_" + userId;
    }

    public static synchronized DatabaseHelper getInstance(Context context, String userId) {
        if (!instances.containsKey(userId)) {
            instances.put(userId, new DatabaseHelper(context, userId));
        }
        return instances.get(userId);
    }

    public static synchronized void removeInstance(String userId) {
        DatabaseHelper instance = instances.remove(userId);
        if (instance != null && database != null && database.isOpen()) {
            database.close();
        }
    }

    /**
     * Guarda la contraseña de la base de datos cifrándola con una clave simétrica
     * almacenada en el Keystore bajo el alias DB_PASSWORD_ALIAS.
     */
    public void setPassword(String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            SecretKey secretKey;

            if (!keyStore.containsAlias(dbPasswordAlias)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
                keyGenerator.init(new android.security.keystore.KeyGenParameterSpec.Builder(
                        dbPasswordAlias,
                        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT | android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                );
                secretKey = keyGenerator.generateKey();
            } else {
                KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(dbPasswordAlias, null);
                secretKey = entry.getSecretKey();
            }

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = cipher.getIV();
            byte[] ciphertext = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));

            SharedPreferences prefs = context.getSharedPreferences("db_prefs", Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(prefPasswordKey, Base64.getEncoder().encodeToString(ciphertext))
                    .putString(prefIvKey, Base64.getEncoder().encodeToString(iv))
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
                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(dbPasswordAlias, null);
                SecretKey secretKey = entry.getSecretKey();

                SharedPreferences prefs = context.getSharedPreferences("db_prefs", Context.MODE_PRIVATE);
                String encryptedPassword = prefs.getString(prefPasswordKey, null);
                String ivString = prefs.getString(prefIvKey, null);
                if (encryptedPassword == null || ivString == null) {
                    throw new IllegalStateException("Password not found in secure storage");
                }
                byte[] ciphertext = Base64.getDecoder().decode(encryptedPassword);
                byte[] iv = Base64.getDecoder().decode(ivString);

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
        db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY, contact_id INTEGER, is_sender BOOLEAN, message TEXT, sent_at TEXT, FOREIGN KEY(contact_id) REFERENCES contacts(id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS messages");
        db.execSQL("DROP TABLE IF EXISTS contacts");
        onCreate(db);
    }

    public void registerUser(String username, String publicKey, byte[] photo) {
        SQLiteDatabase db = getEncryptedWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", username);
        values.put("public_key", publicKey);
        values.put("photo", photo);

        db.insert("contacts", null, values);
    }

    public void updateUserPhoto(String username, byte[] newPhoto) {
        SQLiteDatabase db = getEncryptedWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("photo", newPhoto);

        int rowsUpdated = db.update("contacts", values, "name = ?", new String[]{username});

        if (rowsUpdated == 0) {
            throw new IllegalStateException("No se pudo actualizar la foto. Verifica que el usuario existe.");
        }
    }
}
