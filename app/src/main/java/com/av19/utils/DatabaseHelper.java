package com.av19.utils;

import android.content.Context;
import android.content.ContentValues;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "contacts.db";
    private static final int DATABASE_VERSION = 1;
    private static volatile DatabaseHelper instance;
    private static SQLiteDatabase database;
    private Context context;

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

    public static void setPassword(Context context, String password) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            EncryptedSharedPreferences encryptedSharedPreferences = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                    context,
                    "encrypted_db_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            encryptedSharedPreferences.edit()
                    .putString("db_password", password)
                    .apply();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized SQLiteDatabase getEncryptedWritableDatabase() {
        if (database == null || !database.isOpen()) {
            try {
                MasterKey masterKey = new MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();

                EncryptedSharedPreferences encryptedSharedPreferences = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                        context,
                        "encrypted_db_prefs",
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );

                String password = encryptedSharedPreferences.getString("db_password", null);
                if (password == null) {
                    throw new IllegalStateException("Password not found in secure storage");
                }
                database = super.getWritableDatabase(password);
            } catch (GeneralSecurityException | IOException e) {
                throw new RuntimeException("Failed to open database", e);
            }
        }
        return database;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE contacts (id INTEGER PRIMARY KEY, name TEXT, public_key TEXT)");
        db.execSQL("CREATE TABLE messages (" +
                "id INTEGER PRIMARY KEY, " +
                "contact_id INTEGER, " +
                "is_sender BOOLEAN, " +
                "message TEXT, " +
                "sent_at TEXT, " +
                "FOREIGN KEY(contact_id) REFERENCES contacts(id)" +
                ")");

        // Pasar el objeto db a insertSampleData
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
        messageValues.put("is_sender", 1); // 1 = true (es remitente)
        messageValues.put("message", "¡Hola! Este es un mensaje de pruebaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        messageValues.put("sent_at", "2025-02-18T20:16:51.143Z");
        db.insert("messages", null, messageValues);
    }
}


