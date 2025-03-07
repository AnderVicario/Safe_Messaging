package com.av19.ui;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.av19.R;
import com.av19.adapters.MessagesAdapter;
import com.av19.models.Message;
import com.av19.models.api.ApiResponse;
import com.av19.models.api.MessageCreate;
import com.av19.models.api.MessageResponse;
import com.av19.utils.AESEncryptionManager;
import com.av19.utils.ApiService;
import com.av19.utils.DatabaseHelper;
import com.av19.utils.RetrofitClient;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Conversation extends AppCompatActivity {

    // Constantes
    private static final String TAG = "Conversation";
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS";

    // Componentes de interfaz de usuario
    private TextView profileName;
    private ImageView profilePicture;
    private RecyclerView messagesRecyclerView;
    private EditText messageEditText;
    private FrameLayout btnSend;

    // Variables de datos
    private String contactId, contactName, contactPublicKey;
    private String currentUser;

    // Formateador de fecha en UTC
    private final SimpleDateFormat sdfUtc = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sdfUtc.setTimeZone(TimeZone.getTimeZone("UTC"));
        initUI();
        initIntentData();
        initToolbar();
        initListeners();
        refreshMessagesUI();
        fetchMessages();
    }

    // Inicializa la interfaz de usuario y las propiedades de la ventana.
    private void initUI() {
        EdgeToEdge.enable(this);
        setContentView(R.layout.conversation);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.surface));

        profileName = findViewById(R.id.tv_contact_name);
        profilePicture = findViewById(R.id.iv_contact_img);
        messagesRecyclerView = findViewById(R.id.rview_messages);
        messageEditText = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.frl_send);
    }

    // Recupera los extras del intent.
    private void initIntentData() {
        Intent intent = getIntent();
        contactId = intent.getStringExtra("contact_id");
        contactName = intent.getStringExtra("contact_name");
        contactPublicKey = intent.getStringExtra("contact_public_key");

        profileName.setText(contactName);
        profilePicture.setImageResource(R.drawable.ic_logo_background);

        currentUser = getSharedPreferences("session", MODE_PRIVATE)
                .getString("auth_token", null);
    }

    // Configura la barra de herramientas.
    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    // Configura el listener del botón de enviar.
    private void initListeners() {
        btnSend.setOnClickListener(v -> {
            String messageText = messageEditText.getText().toString().trim();
            if (!messageText.isEmpty()) {
                messageEditText.setText("");
                sendAndStoreMessage(messageText);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.conversation_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_export) {
            exportChat();
            return true;
        } else if (id == R.id.action_import) {
            importChat();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ---------------------------------
    // --- Importación y Exportación ---
    // ---------------------------------

    private void exportChat() {
        if (!isExternalStorageWritable()) {
            Log.e(TAG, "No se puede escribir en el almacenamiento externo");
            return;
        }

        List<Message> messages = getMessagesFromDatabase();
        JSONArray jsonArray = new JSONArray();

        for (Message msg : messages) {
            try {
                JSONObject jsonMessage = new JSONObject();
                jsonMessage.put("id", msg.getId());
                jsonMessage.put("contact_id", msg.getContactId());
                jsonMessage.put("is_sender", msg.getIsSender());
                jsonMessage.put("message", msg.getMessage());
                jsonMessage.put("timestamp", sdfUtc.format(msg.getSentAt()));

                jsonArray.put(jsonMessage);
            } catch (JSONException e) {
                Log.e(TAG, "Error al crear JSON para exportación", e);
            }
        }

        File file = new File(getExternalFilesDir(null), contactId + "_chat_backup.json");

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(jsonArray.toString());
            Log.d(TAG, "Conversación exportada a: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error al escribir archivo de exportación", e);
        }
    }

    private void importChat() {
        if (!isExternalStorageReadable()) {
            Log.e(TAG, "No se puede leer del almacenamiento externo");
            return;
        }

        File file = new File(getExternalFilesDir(null), contactId + "_chat_backup.json");
        if (!file.exists()) {
            Log.e(TAG, "El archivo de respaldo no existe");
            return;
        }

        try {
            StringBuilder stringBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
            }

            JSONArray jsonArray = new JSONArray(stringBuilder.toString());
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
            SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();

            // Comenzar transacción para operaciones masivas
            db.beginTransaction();
            try {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonMessage = jsonArray.getJSONObject(i);
                    int id = jsonMessage.getInt("id");
                    int msgContactId = jsonMessage.getInt("contact_id");
                    boolean isSender = jsonMessage.getBoolean("is_sender");
                    String message = jsonMessage.getString("message");
                    String timestamp = jsonMessage.getString("timestamp");

                    // Verificar si el mensaje ya existe para evitar duplicados
                    Cursor checkCursor = db.rawQuery(
                            "SELECT id FROM messages WHERE id = ?",
                            new String[]{String.valueOf(id)}
                    );

                    boolean messageExists = checkCursor.moveToFirst();
                    checkCursor.close();

                    if (!messageExists) {
                        // Almacenar el mensaje en la base de datos si no existe
                        storeMessageInDatabase(Integer.parseInt(contactId), isSender, message, timestamp);
                    }
                }
                db.setTransactionSuccessful();
                Log.d(TAG, "Conversación importada correctamente");
                refreshMessagesUI(); // Actualizar UI después de importar
            } finally {
                db.endTransaction();
                db.close();
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error al importar conversación", e);
        }
    }

    private boolean isExternalStorageWritable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    // -------------------------
    // --- Base de Datos -------
    // -------------------------

    /**
     * Obtiene todos los mensajes para el contacto actual.
     * Utilizado para exportación y otras operaciones que requieren todos los datos.
     */
    private List<Message> getMessagesFromDatabase() {
        List<Message> messages = new ArrayList<>();
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();

        try {
            Cursor messageCursor = db.rawQuery(
                    "SELECT * FROM messages WHERE contact_id = ? ORDER BY sent_at ASC",
                    new String[]{contactId}
            );

            while (messageCursor.moveToNext()) {
                int idIndex = messageCursor.getColumnIndex("id");
                int contactIdIndex = messageCursor.getColumnIndex("contact_id");
                int messageIndex = messageCursor.getColumnIndex("message");
                int sentAtIndex = messageCursor.getColumnIndex("sent_at");
                int isSenderIndex = messageCursor.getColumnIndex("is_sender");

                if (idIndex != -1 && contactIdIndex != -1 && messageIndex != -1 &&
                        sentAtIndex != -1 && isSenderIndex != -1) {

                    int messageId = messageCursor.getInt(idIndex);
                    int messageContactId = messageCursor.getInt(contactIdIndex);
                    String messageText = messageCursor.getString(messageIndex);
                    boolean isSender = messageCursor.getInt(isSenderIndex) == 1;
                    String sentAtString = messageCursor.getString(sentAtIndex);

                    Date sentAtDate = convertStringToDate(sentAtString);
                    messages.add(new Message(messageId, messageContactId, messageText, isSender, sentAtDate));
                } else {
                    Log.e(TAG, "Índice de columna no encontrado en tabla messages");
                }
            }
            messageCursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Error al obtener mensajes de la base de datos", e);
        } finally {
            db.close();
        }

        return messages;
    }

    /**
     * Almacena un mensaje en la base de datos.
     * Método centralizado para guardar mensajes.
     */
    private void storeMessageInDatabase(int contactId, boolean isSender, String message, String timestamp) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();

        try {
            ContentValues values = new ContentValues();
            values.put("contact_id", contactId);
            values.put("is_sender", isSender ? 1 : 0);
            values.put("message", message);
            values.put("sent_at", timestamp);

            long newRowId = db.insert("messages", null, values);

            if (newRowId == -1) {
                Log.e(TAG, "Error al guardar mensaje en la base de datos");
            } else {
                Log.d(TAG, "Mensaje guardado con ID: " + newRowId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Excepción al guardar mensaje", e);
        } finally {
            db.close();
        }
    }

    /**
     * Sobrecarga que utiliza la fecha actual.
     * Para mensajes nuevos enviados por el usuario.
     */
    private void storeMessageInDatabase(int contactId, boolean isSender, String message) {
        String timestamp = sdfUtc.format(new Date());
        storeMessageInDatabase(contactId, isSender, message, timestamp);
    }

    /**
     * Recupera las marcas de tiempo de mensajes locales.
     * Utilizado para evitar duplicación durante la sincronización.
     */
    private List<String> getLocalMessageTimestamps() {
        List<String> timestamps = new ArrayList<>();
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();

        try {
            Cursor cursor = db.rawQuery(
                    "SELECT sent_at FROM messages",
                    null
            );

            while (cursor.moveToNext()) {
                timestamps.add(cursor.getString(cursor.getColumnIndexOrThrow("sent_at")));
            }
            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Error al obtener timestamps de mensajes", e);
        } finally {
            db.close();
        }

        return timestamps;
    }

    private Date convertStringToDate(String dateString) {
        try {
            return sdfUtc.parse(dateString);
        } catch (ParseException e) {
            Log.e(TAG, "Error analizando fecha: " + dateString, e);
            return new Date(); // Fecha predeterminada en caso de error
        }
    }

    /**
     * Obtiene el ID de contacto basado en su nombre.
     * @param contactName Nombre del contacto a buscar
     * @return ID del contacto o -1 si no se encuentra
     */
    private int getContactIdByName(String contactName) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();
        int id = -1;

        try {
            Cursor cursor = db.rawQuery(
                    "SELECT id FROM contacts WHERE name = ?",
                    new String[]{contactName}
            );

            if (cursor.moveToFirst()) {
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            }
            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Error al buscar contacto por nombre", e);
        } finally {
            db.close();
        }

        return id;
    }

    // -------------------------
    // --- Envío de Mensajes ---
    // -------------------------

    /**
     * Encripta, envía mediante API y almacena el mensaje localmente.
     */
    private void sendAndStoreMessage(String messageText) {
        String encryptedMessage;
        try {
            encryptedMessage = AESEncryptionManager.encryptText(messageText,
                    AESEncryptionManager.getAESKey(contactName));
        } catch (Exception e) {
            Log.e(TAG, "Error de encriptación", e);
            return;
        }

        if (encryptedMessage == null) {
            Log.e(TAG, "La encriptación falló");
            return;
        }

        // Almacenar localmente primero
        storeMessageInDatabase(Integer.parseInt(contactId), false, messageText);
        refreshMessagesUI();

        // Luego enviar a la API
        MessageCreate messageCreate = new MessageCreate(currentUser, contactName, encryptedMessage);
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.sendMessage(messageCreate).enqueue(new retrofit2.Callback<ApiResponse>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse> call, retrofit2.Response<ApiResponse> response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Error en la API: " + response.errorBody());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "Fallo al enviar mensaje", t);
            }
        });
    }

    // -----------------------------
    // --- Recepción de Mensajes ---
    // -----------------------------

    /**
     * Obtiene mensajes desde la API.
     */
    private void fetchMessages() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.getMessages(currentUser).enqueue(new retrofit2.Callback<List<MessageResponse>>() {
            @Override
            public void onResponse(retrofit2.Call<List<MessageResponse>> call, retrofit2.Response<List<MessageResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    processMessages(response.body());
                } else {
                    Log.e(TAG, "Error al obtener mensajes: " + response.errorBody());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<MessageResponse>> call, Throwable t) {
                Log.e(TAG, "Fallo al obtener mensajes", t);
            }
        });
    }

    /**
     * Procesa y almacena nuevos mensajes recibidos de la API.
     * Guarda todos los mensajes en la base de datos, pero solo actualiza
     * la UI si hay mensajes nuevos para la conversación actual.
     */
    private void processMessages(List<MessageResponse> messagesResponse) {
        List<String> localTimestamps = getLocalMessageTimestamps();
        boolean hasNewMessagesForCurrentContact = false;

        for (MessageResponse mr : messagesResponse) {
            String sentAtStr = mr.getTimestamp();
            String sender = mr.getSender();
            String recipient = this.currentUser;

            // Omitir mensajes que ya tenemos o mensajes iniciales
            if (localTimestamps.contains(sentAtStr) || mr.getIs_initial()) {
                continue;
            }

            String decryptedMessage;
            try {
                decryptedMessage = AESEncryptionManager.decryptText(
                        mr.getEncrypted_message(),
                        AESEncryptionManager.getAESKey(sender)
                );
            } catch (Exception e) {
                Log.e(TAG, "Error de desencriptación", e);
                continue;
            }

            if (decryptedMessage == null) continue;

            // Determinar el ID del contacto para este mensaje
            int messageContactId;
            boolean messageIsSender;

            if (sender.equals(currentUser)) {
                // Mensaje enviado por el usuario actual
                messageContactId = getContactIdByName(recipient);
                messageIsSender = false;
            } else {
                // Mensaje recibido por el usuario actual
                messageContactId = getContactIdByName(sender);
                messageIsSender = true;
            }

            // Solo si se pudo identificar el contacto
            if (messageContactId != -1) {
                storeMessageInDatabase(messageContactId, messageIsSender, decryptedMessage, sentAtStr);

                // Verificar si este mensaje pertenece a la conversación actual
                if (messageContactId == Integer.parseInt(contactId)) {
                    hasNewMessagesForCurrentContact = true;
                }
            }
        }

        // Solo actualizar la UI si hay mensajes nuevos para el contacto actual
        if (hasNewMessagesForCurrentContact) {
            runOnUiThread(this::refreshMessagesUI);
        }
    }

    // -------------------------------------------------
    // --- Actualizaciones de la Interfaz de Usuario ---
    // -------------------------------------------------

    /**
     * Actualiza el RecyclerView con los mensajes más recientes.
     */
    private void refreshMessagesUI() {
        List<Message> messages = getMessagesFromDatabase();
        MessagesAdapter adapter = new MessagesAdapter(messages);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(adapter);
        if (!messages.isEmpty()) {
            messagesRecyclerView.scrollToPosition(messages.size() - 1);
        }
    }
}