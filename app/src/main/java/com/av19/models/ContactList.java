package com.av19.models;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.av19.models.api.ProfilePictureResponse;
import com.av19.utils.ApiService;
import com.av19.utils.DatabaseHelper;
import com.av19.utils.RetrofitClient;

import net.sqlcipher.database.SQLiteDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ContactList {
    private static final Map<String, ContactList> instances = new HashMap<>();
    private static List<Contact> contacts = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private String currentUser;
    private OnContactPhotoUpdatedListener photoUpdateListener;

    private ContactList(Context context, String currentUser) {
        this.currentUser = currentUser;
        this.dbHelper = DatabaseHelper.getInstance(context, currentUser);
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
        int contactPublicKeyIndex = contactCursor.getColumnIndex("public_key");
        int contactPhotoIndex = contactCursor.getColumnIndex("photo");

        if (contactIdIndex == -1 || contactNameIndex == -1) {
            Log.e("ContactList", "Column index not found for 'id' or 'name'");
        } else {
            while (contactCursor.moveToNext()) {
                int contactId = contactCursor.getInt(contactIdIndex);
                String contactName = contactCursor.getString(contactNameIndex);
                String contactPublicKey = contactCursor.getString(contactPublicKeyIndex);
                byte[] contactPhoto = contactPhotoIndex != -1 ? contactCursor.getBlob(contactPhotoIndex) : null;

                // Cargar mensajes para el contacto actual
                List<Message> messages = loadMessagesForContact(db, contactId);

                // Crear el contacto y añadirlo a la lista
                contacts.add(new Contact(contactId, contactName, contactPublicKey, contactPhoto, messages, context));
            }
        }
        sortContacts();
        contactCursor.close();
        db.close();

        // Para cada contacto, actualizar la foto usando la API, excepto el propio usuario
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        for (Contact contact : contacts) {
            if (!contact.getName().equals(currentUser)) {
                Call<ProfilePictureResponse> call = apiService.getProfilePicture(contact.getName());
                call.enqueue(new Callback<ProfilePictureResponse>() {
                    @Override
                    public void onResponse(Call<ProfilePictureResponse> call, Response<ProfilePictureResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String profilePictureBase64 = response.body().getProfile_picture();
                            if (profilePictureBase64 != null) {
                                try {
                                    byte[] updatedPhoto = java.util.Base64.getDecoder().decode(profilePictureBase64);
                                    contact.setPhoto(updatedPhoto);
                                    if (photoUpdateListener != null) {
                                        photoUpdateListener.onContactPhotoUpdated(contact);
                                    }
                                } catch (IllegalArgumentException e) {
                                    Log.e("ContactList", "Error decoding profile picture for " + contact.getName(), e);
                                }
                            }
                        } else {
                            Log.e("ContactList", "No se pudo obtener la foto de " + contact.getName());
                        }
                    }

                    @Override
                    public void onFailure(Call<ProfilePictureResponse> call, Throwable t) {
                        Log.e("ContactList", "Error fetching updated photo for contact: " + contact.getName(), t);
                    }
                });
            }
        }
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

    public static synchronized ContactList getInstance(Context context, String currentUser) {
        if (!instances.containsKey(currentUser)) {
            instances.put(currentUser, new ContactList(context, currentUser));
        }
        return instances.get(currentUser);
    }

    public static synchronized void removeInstance(String currentUser) {
        instances.remove(currentUser);
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    // Agregar un nuevo contacto y actualizar la lista
    public int addContact(String name, String publicKey, byte[] photo, Context context) {
        SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();

        ContentValues contactValues = new ContentValues();
        contactValues.put("name", name);
        contactValues.put("public_key", publicKey);
        contactValues.put("photo", photo);

        long contactId = db.insert("contacts", null, contactValues);
        db.close();

        Contact newContact = new Contact((int) contactId, name, publicKey, photo, new ArrayList<>(), context);
        contacts.add(newContact);
        sortContacts();

        return contacts.size() - 1;
    }

    public void updateContact(int contactId, String newName, byte[] newPhoto) {
        SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", newName);
        values.put("photo", newPhoto);
        int rowsAffected = db.update("contacts", values, "id = ?", new String[]{String.valueOf(contactId)});
        Log.d("ContactList", "Actualizados " + rowsAffected + " registros para el contacto " + contactId);
        db.close();
    }

    // Recargar la lista completa de contactos
    public void reloadContacts(Context context) {
        loadContacts(context);
    }

    // Ordenar los contactos según la fecha del último mensaje
    public void sortContacts() {
        contacts.sort((c1, c2) -> {
            Date date1 = c1.getLastMessageDate();
            Date date2 = c2.getLastMessageDate();

            if (date1 == null && date2 == null) return 0;
            if (date1 == null) return 1;
            if (date2 == null) return -1;

            return date2.compareTo(date1);
        });
    }

    public interface OnContactPhotoUpdatedListener {
        void onContactPhotoUpdated(Contact contact);
    }

    public void setOnContactPhotoUpdatedListener(OnContactPhotoUpdatedListener listener) {
        this.photoUpdateListener = listener;
    }
}
