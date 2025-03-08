package com.av19.models;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.av19.utils.DatabaseHelper;

import net.sqlcipher.database.SQLiteDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ContactList {
    private static ContactList myContactList;
    private static List<Contact> contacts = new ArrayList<>();
    private DatabaseHelper dbHelper;

    private ContactList(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
        // Carga inicial de contactos
        loadContacts(context);
    }

    // Método encargado de cargar y actualizar la lista de contactos
    private void loadContacts(Context context) {
        contacts.clear();
        SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();

        Cursor contactCursor = db.rawQuery("SELECT * FROM contacts", null);
        int contactIdIndex = contactCursor.getColumnIndex("id");
        int contactNameIndex = contactCursor.getColumnIndex("name");
        int contactPhotoIndex = contactCursor.getColumnIndex("photo");

        if (contactIdIndex == -1 || contactNameIndex == -1) {
            Log.e("ContactList", "Column index not found for 'id' or 'name'");
        } else {
            while (contactCursor.moveToNext()) {
                int contactId = contactCursor.getInt(contactIdIndex);
                String contactName = contactCursor.getString(contactNameIndex);
                byte[] contactPhoto = contactPhotoIndex != -1 ? contactCursor.getBlob(contactPhotoIndex) : null;

                // Cargar mensajes para el contacto actual
                List<Message> messages = loadMessagesForContact(db, contactId);

                // Crear el contacto y añadirlo a la lista
                contacts.add(new Contact(contactId, contactName, contactPhoto, messages, context));
            }
        }
        sortContacts();
        contactCursor.close();
        db.close();
    }

    // Método para cargar los mensajes asociados a un contacto
    private List<Message> loadMessagesForContact(SQLiteDatabase db, int contactId) {
        List<Message> messages = new ArrayList<>();
        Cursor messageCursor = db.rawQuery(
                "SELECT * FROM messages WHERE contact_id = ?",
                new String[]{String.valueOf(contactId)}
        );

        int idIndex = messageCursor.getColumnIndex("id");
        int contactIdIndex = messageCursor.getColumnIndex("contact_id");
        int messageTextIndex = messageCursor.getColumnIndex("message");
        int sentAtIndex = messageCursor.getColumnIndex("sent_at");
        int isSenderIndex = messageCursor.getColumnIndex("is_sender");

        while (messageCursor.moveToNext()) {
            // Validar que se hayan obtenido correctamente los índices
            if (idIndex != -1 && contactIdIndex != -1 && messageTextIndex != -1
                    && sentAtIndex != -1 && isSenderIndex != -1) {
                int messageId = messageCursor.getInt(idIndex);
                int messageContactId = messageCursor.getInt(contactIdIndex);
                String messageText = messageCursor.getString(messageTextIndex);
                boolean isSender = messageCursor.getInt(isSenderIndex) == 1;
                String sentAtString = messageCursor.getString(sentAtIndex);

                Date sentAtDate = convertStringToDate(sentAtString);
                messages.add(new Message(messageId, messageContactId, messageText, isSender, sentAtDate));
            } else {
                Log.e("ContactList", "Column index not found in messages for 'message' or 'sent_at'");
            }
        }
        messageCursor.close();
        return messages;
    }

    // Conversión de String a Date
    private Date convertStringToDate(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return sdf.parse(dateString);
        } catch (ParseException e) {
            Log.e("ContactList", "Error parsing date: " + dateString, e);
            return null;
        }
    }

    public static ContactList getInstance(Context context) {
        if (myContactList == null) {
            myContactList = new ContactList(context);
        }
        return myContactList;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    // Agregar un nuevo contacto y actualizar la lista
    public int addContact(String name, byte[] photo, Context context) {
        SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();

        ContentValues contactValues = new ContentValues();
        contactValues.put("name", name);
        contactValues.put("photo", photo);

        long contactId = db.insert("contacts", null, contactValues);
        db.close();

        Contact newContact = new Contact((int) contactId, name, photo, new ArrayList<>(), context);
        contacts.add(newContact);
        sortContacts();

        return contacts.size() - 1;
    }

    // Recargar la lista de contactos
    public void reloadContacts(Context context) {
        loadContacts(context);
    }

    // Ordenar los contactos según la fecha del último mensaje
    private void sortContacts() {
        contacts.sort((c1, c2) -> {
            Date date1 = c1.getLastMessageDate();
            Date date2 = c2.getLastMessageDate();

            if (date1 == null && date2 == null) return 0;
            if (date1 == null) return 1;
            if (date2 == null) return -1;

            return date2.compareTo(date1);
        });
    }
}
