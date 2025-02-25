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
import com.av19.utils.ApiService;
import com.av19.utils.DatabaseHelper;
import com.av19.utils.ECCEncryptionManager;
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

    // Variables de UI
    private TextView profileName;
    private ImageView profilePicture;
    private RecyclerView messagesRecyclerView;
    private EditText messageEditText;
    private FrameLayout btnSend;

    // Datos del contacto y usuario
    private String contactId, contactName, contactPublicKey;
    private String currentUser;

    // Formateador de fechas en formato UTC
    private final SimpleDateFormat sdfUtc =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sdfUtc.setTimeZone(TimeZone.getTimeZone("UTC"));
        initializeUI();
        retrieveIntentData();
        setupToolbar();
        setupListeners();

        // Cargar los mensajes de la conversación
        loadConversation(contactId);
        fetchEncryptedMessages();
    }

    /**
     * Inicializa la interfaz de usuario y configura las propiedades de la ventana.
     */
    private void initializeUI() {
        // Habilitar EdgeToEdge y asignar el layout
        setContentView(R.layout.conversation);
        // Configurar el color de la barra de estado
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.surface));

        // Inicializar vistas
        profileName = findViewById(R.id.tv_contact_name);
        profilePicture = findViewById(R.id.iv_contact_img);
        messagesRecyclerView = findViewById(R.id.rview_messages);
        messageEditText = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.frl_send);
    }

    /**
     * Recupera los datos enviados desde el activity anterior (por ejemplo, ListContacts).
     */
    private void retrieveIntentData() {
        Intent intent = getIntent();
        contactId = intent.getStringExtra("contact_id");
        contactName = intent.getStringExtra("contact_name");
        contactPublicKey = intent.getStringExtra("contact_public_key");

        // Mostrar la información del contacto
        profileName.setText(contactName);
        profilePicture.setImageResource(R.drawable.ic_logo_background);

        // Obtener el token del usuario actual desde SharedPreferences
        currentUser = getSharedPreferences("session", MODE_PRIVATE)
                .getString("auth_token", null);
    }

    /**
     * Configura el toolbar y sus opciones.
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Configura los listeners de los botones.
     */
    private void setupListeners() {
        btnSend.setOnClickListener(v -> sendEncryptedMessage());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.conversation_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Cerrar el activity actual
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Envía un mensaje sin encriptar (se utiliza para guardar en la base de datos local).
     */
    private void sendMessage(String messageText) {
        if (messageText.isEmpty()) {
            return; // No se envía mensaje vacío.
        }

        // Limpiar el EditText para escribir un nuevo mensaje
        messageEditText.setText("");

        // Obtener la fecha y hora actual en formato UTC
        String sentAt = sdfUtc.format(new Date());

        // Insertar el mensaje en la base de datos local
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();
        String sql = "INSERT INTO messages (contact_id, message, is_sender, sent_at) VALUES (?, ?, ?, ?)";
        db.execSQL(sql, new Object[]{contactId, messageText, 0, sentAt});
        db.close();

        // Actualizar la conversación para mostrar el nuevo mensaje
        loadConversation(contactId);
    }

    /**
     * Envía un mensaje encriptado utilizando la API y luego lo guarda en la base de datos local.
     */
    private void sendEncryptedMessage() {
        String messageText = messageEditText.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }
        // Limpiar el campo de entrada
        messageEditText.setText("");

        // Encriptar el mensaje usando la clave pública del contacto
        String encryptedMessage = null;
        try {
            ECCEncryptionManager eccEncryptionManager = ECCEncryptionManager.getInstance(Boolean.FALSE, currentUser);
            encryptedMessage = eccEncryptionManager.encrypt(messageText, contactPublicKey);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (encryptedMessage == null) {
            Log.e("Conversation", "Error al encriptar el mensaje");
            return;
        }

        // Crear objeto para enviar a la API
        MessageCreate messageCreate = new MessageCreate(currentUser, contactName, encryptedMessage);
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.sendMessage(messageCreate).enqueue(new retrofit2.Callback<ApiResponse>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse> call, retrofit2.Response<ApiResponse> response) {
                if (response.isSuccessful()) {
                    // Guardar el mensaje localmente tras un envío exitoso
                     sendMessage(messageText);
                } else {
                    Log.e("Conversation", "Error en la respuesta de la API: " + response.errorBody());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse> call, Throwable t) {
                Log.e("Conversation", "Fallo al enviar el mensaje", t);
            }
        });
    }

    /**
     * Carga la conversación (mensajes) de la base de datos local para el contacto dado.
     *
     * @param contactId Identificador del contacto
     */
    private void loadConversation(String contactId) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT message, is_sender, sent_at FROM messages " +
                        "WHERE contact_id = ? ORDER BY sent_at ASC",
                new String[]{contactId}
        );

        List<Message> messages = new ArrayList<>();
        while (cursor.moveToNext()) {
            try {
                String text = cursor.getString(cursor.getColumnIndexOrThrow("message"));
                boolean isSender = cursor.getInt(cursor.getColumnIndexOrThrow("is_sender")) == 1;
                String sentAtString = cursor.getString(cursor.getColumnIndexOrThrow("sent_at"));
                Date sentAt = sdfUtc.parse(sentAtString);
                messages.add(new Message(text, isSender, sentAt));
            } catch (ParseException e) {
                Log.e("Conversation", "Error parsing date", e);
            }
        }
        cursor.close();
        db.close();

        // Configurar el RecyclerView con el adaptador
        MessagesAdapter adapter = new MessagesAdapter(messages);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(adapter);
        messagesRecyclerView.scrollToPosition(messages.size() - 1);
    }

    /**
     * Obtiene los mensajes encriptados desde la API, los desencripta y actualiza el RecyclerView.
     */
    private void fetchEncryptedMessages() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.getMessages(currentUser).enqueue(new retrofit2.Callback<List<MessageResponse>>() {
            @Override
            public void onResponse(retrofit2.Call<List<MessageResponse>> call, retrofit2.Response<List<MessageResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MessageResponse> messagesResponse = response.body();
                    List<Message> newMessages = new ArrayList<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

                    // Obtener la base de datos
                    DatabaseHelper dbHelper = DatabaseHelper.getInstance(Conversation.this);
                    SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();

                    // Consulta para obtener los mensajes ya guardados
                    Cursor cursor = db.rawQuery(
                            "SELECT sent_at FROM messages WHERE contact_id = ?",
                            new String[]{contactId}
                    );

                    // Guardar las fechas de los mensajes existentes
                    List<String> existingTimestamps = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        existingTimestamps.add(cursor.getString(cursor.getColumnIndexOrThrow("sent_at")));
                    }
                    cursor.close();

                    // Insertar solo los mensajes que no existen en la base de datos
                    for (MessageResponse mr : messagesResponse) {
                        String sentAtString = mr.getTimestamp();

                        // Si el mensaje ya existe, lo ignoramos
                        if (existingTimestamps.contains(sentAtString)) {
                            continue;
                        }

                        // Desencriptar el mensaje
                        String decryptedMessage = null;
                        try{
                            ECCEncryptionManager eccEncryptionManager = ECCEncryptionManager.getInstance(Boolean.FALSE, currentUser);
                            decryptedMessage = eccEncryptionManager.decrypt(mr.getEncrypted_message());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (decryptedMessage == null) {
                            continue; // Si hay un error en la desencriptación, no lo guardamos
                        }

                        // Convertir la fecha
                        Date sentAt = null;
                        try {
                            sentAt = sdf.parse(sentAtString);
                        } catch (ParseException e) {
                            Log.e("Conversation", "Error al parsear la fecha", e);
                        }

                        // Determinar si el mensaje fue enviado por el usuario actual
                        boolean isSender = !mr.getSender().equals(currentUser);

                        // Insertar el mensaje en la base de datos
                        db.execSQL("INSERT INTO messages (contact_id, message, is_sender, sent_at) VALUES (?, ?, ?, ?)",
                                new Object[]{contactId, decryptedMessage, isSender ? 1 : 0, sentAtString});

                        // Agregar a la lista de nuevos mensajes
                        newMessages.add(new Message(decryptedMessage, isSender, sentAt));
                    }

                    db.close();

                    // Si hay mensajes nuevos, actualizar la UI
                    if (!newMessages.isEmpty()) {
                        runOnUiThread(() -> {
                            // Recargar la lista de mensajes desde la base de datos
                            loadConversation(contactId);
                        });
                    }
                } else {
                    Log.e("Conversation", "Error al obtener mensajes: " + response.errorBody());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<MessageResponse>> call, Throwable t) {
                Log.e("Conversation", "Fallo al obtener los mensajes", t);
            }
        });
    }

}
