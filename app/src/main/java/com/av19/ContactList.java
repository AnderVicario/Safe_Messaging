package com.av19;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import net.sqlcipher.database.SQLiteDatabase;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class ContactList {
    private static ContactList myContactList;
    private static List<Contact> contacts = new ArrayList<>();
    private DatabaseHelper dbHelper;

    private ContactList(Context context) {
        dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();

        // Cargar contactos desde la base de datos
        Cursor contactCursor = db.rawQuery("SELECT * FROM contacts", null);
        int contactIdIndex = contactCursor.getColumnIndex("id");
        int contactNameIndex = contactCursor.getColumnIndex("name");

        if (contactIdIndex == -1 || contactNameIndex == -1) {
            Log.e("ContactList", "Column index not found for 'id' or 'name'");
        } else {
            while (contactCursor.moveToNext()) {
                int contactId = contactCursor.getInt(contactIdIndex);
                String contactName = contactCursor.getString(contactNameIndex);

                List<Message> messages = new ArrayList<>();
                Cursor messageCursor = db.rawQuery("SELECT message, sent_at FROM messages WHERE contact_id = ?", new String[]{String.valueOf(contactId)});
                while (messageCursor.moveToNext()) {
                    int messageTextIndex = messageCursor.getColumnIndex("message");
                    int sentAtIndex = messageCursor.getColumnIndex("sent_at");

                    // Verifica si los índices son válidos
                    if (messageTextIndex != -1 && sentAtIndex != -1) {
                        String messageText = messageCursor.getString(messageTextIndex);
                        String sentAtString = messageCursor.getString(sentAtIndex);

                        // Convierte la cadena a un objeto Date
                        Date sentAtDate = convertStringToDate(sentAtString);

                        messages.add(new Message(messageText, sentAtDate));
                    } else {
                        Log.e("ContactList", "Column index not found in messages for 'message' or 'sent_at'");
                    }
                }
                messageCursor.close();

                // Aquí se asume que el constructor de Contact ahora acepta una lista de mensajes
                Contact contact = new Contact(contactId, contactName, messages);
                contacts.add(contact);
            }
        }
        sortContacts();
        contactCursor.close();
        db.close();
    }

    // Método para convertir la cadena a Date
    private Date convertStringToDate(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return sdf.parse(dateString);
        } catch (ParseException e) {
            Log.e("ContactList", "Error parsing date: " + dateString, e);
            return null; // Devuelve null si hay un error
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

    private void sortContacts() {
        Collections.sort(contacts, new Comparator<Contact>() {
            @Override
            public int compare(Contact c1, Contact c2) {
                Date date1 = c1.getLastMessageDate(); // Método que devuelve la fecha del último mensaje
                Date date2 = c2.getLastMessageDate();
                if (date1 == null && date2 == null) return 0;
                if (date1 == null) return 1; // Colocar contactos sin mensajes al final
                if (date2 == null) return -1; // Colocar contactos sin mensajes al final
                return date2.compareTo(date1); // Ordenar descendente
            }
        });
    }

    public int addContact(String name) {
        SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();

        // Insertar el contacto en la base de datos
        ContentValues contactValues = new ContentValues();
        contactValues.put("name", name);
        long contactId = db.insert("contacts", null, contactValues);

        db.close();

        // Crear el objeto Contact y agregarlo a la lista en memoria
        Contact newContact = new Contact((int) contactId, name, null);
        contacts.add(newContact);
        sortContacts();

        return contacts.size() - 1; // Retorna el índice del nuevo contacto en la lista
    }
}
