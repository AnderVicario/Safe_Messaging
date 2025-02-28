package com.av19.ui;

import android.content.Intent;
import android.os.Bundle;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Conversation extends AppCompatActivity {

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
    private final SimpleDateFormat sdfUtc =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());

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
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // -------------------------
    // --- Envío de Mensajes ---
    // -------------------------

    // Encripta, envía mediante API y almacena el mensaje localmente.
    private void sendAndStoreMessage(String messageText) {
        String encryptedMessage;
        try {
            encryptedMessage = AESEncryptionManager.encryptText(messageText,
                    AESEncryptionManager.getAESKey(contactName));
        } catch (Exception e) {
            Log.e("Conversation", "Error de encriptación", e);
            return;
        }

        if (encryptedMessage == null) {
            Log.e("Conversation", "La encriptación falló");
            return;
        }

        MessageCreate messageCreate = new MessageCreate(currentUser, contactName, encryptedMessage);
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.sendMessage(messageCreate).enqueue(new retrofit2.Callback<ApiResponse>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse> call, retrofit2.Response<ApiResponse> response) {
                if (response.isSuccessful()) {
                    fetchMessages();
                    storeMessageLocally(messageText, false);
                    refreshMessagesUI();
                } else {
                    Log.e("Conversation", "Error en la API: " + response.errorBody());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse> call, Throwable t) {
                Log.e("Conversation", "Fallo al enviar", t);
            }
        });
    }

    // Almacena un mensaje en la base de datos local.
    private void storeMessageLocally(String messageText, boolean isSender) {
        String sentAt = sdfUtc.format(new Date());
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();
        String sql = "INSERT INTO messages (contact_id, message, is_sender, sent_at) VALUES (?, ?, ?, ?)";
        db.execSQL(sql, new Object[]{contactId, messageText, isSender ? 1 : 0, sentAt});
        db.close();
    }

    // -----------------------------
    // --- Recepción de Mensajes ---
    // -----------------------------

    // Obtiene mensajes desde la API.
    private void fetchMessages() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.getMessages(currentUser).enqueue(new retrofit2.Callback<List<MessageResponse>>() {
            @Override
            public void onResponse(retrofit2.Call<List<MessageResponse>> call, retrofit2.Response<List<MessageResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    processMessages(response.body());
                } else {
                    Log.e("Conversation", "Error al obtener mensajes: " + response.errorBody());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<MessageResponse>> call, Throwable t) {
                Log.e("Conversation", "Fallo al obtener mensajes", t);
            }
        });
    }

    // Procesa y almacena nuevos mensajes.
    private void processMessages(List<MessageResponse> messagesResponse) {
        List<String> localTimestamps = getLocalMessageTimestamps();
        boolean hasNewMessages = false;
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();

        for (MessageResponse mr : messagesResponse) {
            String sentAtStr = mr.getTimestamp();
            String sender = mr.getSender();
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
                Log.e("Conversation", "Error de desencriptación", e);
                continue;
            }
            if (decryptedMessage == null) continue;

            boolean isSender = !mr.getSender().equals(currentUser);
            db.execSQL(
                    "INSERT INTO messages (contact_id, message, is_sender, sent_at) VALUES (?, ?, ?, ?)",
                    new Object[]{contactId, decryptedMessage, isSender ? 1 : 0, sentAtStr}
            );
            hasNewMessages = true;
        }
        db.close();
        if (hasNewMessages) {
            runOnUiThread(this::refreshMessagesUI);
        }
    }

    // Recupera las marcas de tiempo de los mensajes ya almacenados.
    private List<String> getLocalMessageTimestamps() {
        List<String> timestamps = new ArrayList<>();
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT sent_at FROM messages WHERE contact_id = ?", new String[]{contactId});
        while (cursor.moveToNext()) {
            timestamps.add(cursor.getString(cursor.getColumnIndexOrThrow("sent_at")));
        }
        cursor.close();
        db.close();
        return timestamps;
    }

    // -------------------------------------------------
    // --- Actualizaciones de la Interfaz de Usuario ---
    // -------------------------------------------------

    // Actualiza los mensajes mostrados en el RecyclerView.
    private void refreshMessagesUI() {
        List<Message> messages = loadLocalMessages();
        MessagesAdapter adapter = new MessagesAdapter(messages);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(adapter);
        if (!messages.isEmpty()) {
            messagesRecyclerView.scrollToPosition(messages.size() - 1);
        }
    }

    // Carga los mensajes desde la base de datos local.
    private List<Message> loadLocalMessages() {
        List<Message> messages = new ArrayList<>();
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT message, is_sender, sent_at FROM messages WHERE contact_id = ? ORDER BY sent_at ASC",
                new String[]{contactId}
        );
        while (cursor.moveToNext()) {
            try {
                String text = cursor.getString(cursor.getColumnIndexOrThrow("message"));
                boolean isSender = cursor.getInt(cursor.getColumnIndexOrThrow("is_sender")) == 1;
                Date sentAt = sdfUtc.parse(cursor.getString(cursor.getColumnIndexOrThrow("sent_at")));
                messages.add(new Message(text, isSender, sentAt));
            } catch (ParseException e) {
                Log.e("Conversation", "Error al analizar la fecha", e);
            }
        }
        cursor.close();
        db.close();
        return messages;
    }
}
