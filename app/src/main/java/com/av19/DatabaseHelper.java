package com.av19;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import android.content.ContentValues;
import android.content.Context;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "contacts.db";
    private static final int DATABASE_VERSION = 1;
    private static final String PASSWORD = "TuClaveSegura";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        SQLiteDatabase.loadLibs(context);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE contacts (id INTEGER PRIMARY KEY, name TEXT)");
        db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY, contact_id INTEGER, message TEXT, sent_at TEXT, FOREIGN KEY(contact_id) REFERENCES contacts(id))");

        // Pasar el objeto db a insertSampleData
        insertSampleData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS messages");
        db.execSQL("DROP TABLE IF EXISTS contacts");
        onCreate(db);
    }

    public SQLiteDatabase getEncryptedWritableDatabase() {
        return getWritableDatabase(PASSWORD);
    }

    public void insertSampleData(SQLiteDatabase db) {
        // Insertar contactos
        String[] contactNames = {
                "John Doe",
                "Ivy White",
                "Jack Black",
                "Karen Green",
                "Emily Davis",
                "Michael Brown"
        };

        for (String name : contactNames) {
            ContentValues contactValues = new ContentValues();
            contactValues.put("name", name);
            db.insert("contacts", null, contactValues);
        }

        // Insertar mensajes de ejemplo para cada contacto
        String[] messages = {
                "Hello!",
                "Have a great day!",
                "Can't wait to see you!",
                "Long time no see!",
                "Let's catch up soon.",
                "How's it going?"
        };

        String[] dates = {
                "2024-10-21 12:30:00",
                "2024-10-02 15:45:00",
                "2024-10-13 10:00:00",
                "2024-10-04 09:15:00",
                "2024-10-05 14:20:00",
                "2024-10-27 20:30:00"
        };

        for (int i = 0; i < contactNames.length; i++) {
            ContentValues messageValues = new ContentValues();
            messageValues.put("contact_id", i + 1); // Los IDs comienzan desde 1
            messageValues.put("message", messages[i]);
            messageValues.put("sent_at", dates[i]);
            db.insert("messages", null, messageValues);
        }
    }
}


