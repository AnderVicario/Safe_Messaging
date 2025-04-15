package com.av19.ui;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.av19.R;
import com.av19.adapters.MessagesAdapter;
import com.av19.models.Message;
import com.av19.models.api.MessageCreate;
import com.av19.models.api.SendMessageResponse;
import com.av19.utils.ApiService;
import com.av19.utils.DatabaseHelper;
import com.av19.utils.MessageSchedulerReceiver;
import com.av19.utils.RetrofitClient;
import com.av19.utils.SnackbarUtils;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class Conversation extends BaseLocaleActivity {

    // Constantes
    private static final String TAG = "Conversation";
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS";

    // Componentes de interfaz de usuario
    private TextView profileName;
    private ImageView profilePicture;
    private RecyclerView messagesRecyclerView;
    private EditText messageEditText;
    private FrameLayout btnSend;

    private FrameLayout btnLocation;

    // Variables
    private String contactId, contactName;
    private String currentUser;

    // Formateador de fecha
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

        LocalBroadcastManager.getInstance(this).registerReceiver(newMessageReceiver, new IntentFilter("NEW_MESSAGES_ADDED"));
        Log.d("Conversation", "onCreate");
    }

    @Override
    protected void onResume() {
        Log.d("Conversation", "onResume");
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(newMessageReceiver, new IntentFilter("NEW_MESSAGES_ADDED"));
    }

    @Override
    protected void onPause() {
        Log.d("Conversation", "onPause");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(newMessageReceiver);
        super.onPause();
    }

    private BroadcastReceiver newMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("NEW_MESSAGES_ADDED".equals(intent.getAction())) {
                ArrayList<Integer> updatedContacts = intent.getIntegerArrayListExtra("updated_contacts");
                if (updatedContacts != null && updatedContacts.contains(Integer.parseInt(contactId))) {
                    refreshMessagesUI();
                }
            }
        }
    };

    // Inicializar la interfaz de usuario y las propiedades de la ventana
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
        btnLocation = findViewById(R.id.frl_location);
    }

    // Recuperar los extras del intent
    private void initIntentData() {
        Intent intent = getIntent();
        contactId = intent.getStringExtra("contact_id");
        Log.d("Conversation", contactId);
        contactName = intent.getStringExtra("contact_name");
        Log.d("Conversation", contactName);
        byte[] photo = getIntent().getByteArrayExtra("contact_photo");

        profileName.setText(contactName);
        if (photo != null) {
            Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(photo, 0, photo.length);
            profilePicture.setImageBitmap(bitmap);
        } else {
            profilePicture.setImageResource(R.drawable.ic_launcher_background);
        }

        currentUser = getSharedPreferences("session", MODE_PRIVATE)
                .getString("auth_token", null);
    }

    // Configurar la toolbar
    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    // Configurar el listener del botón de enviar
    private void initListeners() {
        btnSend.setOnClickListener(v -> {
            String messageText = messageEditText.getText().toString().trim();
            if (!messageText.isEmpty()) {
                messageEditText.setText("");
                sendAndStoreMessage(messageText);
            }
        });
        btnSend.setOnLongClickListener(v -> {
            String messageText = messageEditText.getText().toString().trim();
            if (!messageText.isEmpty()) {
                messageEditText.setText("");
                showDateTimePicker(messageText);
                return true;
            }
            return false;
        });
        btnLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddLocationMenu.class);
            addLocationLauncher.launch(intent);
        });
    }

    private final ActivityResultLauncher<Intent> addLocationLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        String location = data.getStringExtra("location");
                        messageEditText.setText(location);
                    }
                }
            }
    );

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
        } else if (id == R.id.action_delete) {
            deleteChat();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // -----------------------------------------
    // --- Importación, Exportación y Vaciar ---
    // -----------------------------------------

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
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(this, currentUser);
            SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();

            try {
                db.beginTransaction();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonMessage = jsonArray.getJSONObject(i);
                    int id = jsonMessage.getInt("id");
                    int msgContactId = jsonMessage.getInt("contact_id");
                    boolean isSender = jsonMessage.getBoolean("is_sender");
                    String message = jsonMessage.getString("message");
                    String timestamp = jsonMessage.getString("timestamp");

                    // Verificar si el mensaje ya existe
                    Cursor checkCursor = db.rawQuery(
                            "SELECT id FROM messages WHERE id = ?",
                            new String[]{String.valueOf(id)}
                    );

                    boolean messageExists = checkCursor.moveToFirst();
                    checkCursor.close();

                    if (!messageExists) {
                        // Almacenar el mensaje en la base de datos si no existe
                        ContentValues values = new ContentValues();
                        values.put("contact_id", contactId);
                        values.put("is_sender", isSender ? 1 : 0);
                        values.put("message", message);
                        values.put("sent_at", timestamp);

                        db.insert("messages", null, values);
                    }
                }

                db.setTransactionSuccessful();
                Log.d(TAG, "Conversación importada correctamente");
            } finally {
                db.endTransaction();
                db.close();
            }

            refreshMessagesUI();
            notifyContactListUpdate();
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error al importar la conversación", e);
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

    private void deleteChat() {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this, currentUser);
        SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();
        db.delete("messages", "contact_id = ?", new String[]{contactId});
        db.close();
        refreshMessagesUI();
        notifyContactListUpdate();
    }

    // -------------------------
    // --- Base de Datos -------
    // -------------------------

    private List<Message> getMessagesFromDatabase() {
        List<Message> messages = new ArrayList<>();
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this, currentUser);
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

    private void storeMessageInDatabase(int contactId, boolean isSender, String message, String timestamp) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this, currentUser);
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

    private void storeMessageInDatabase(int contactId, boolean isSender, String message) {
        String timestamp = sdfUtc.format(new Date());
        storeMessageInDatabase(contactId, isSender, message, timestamp);
    }

    private Date convertStringToDate(String dateString) {
        try {
            return sdfUtc.parse(dateString);
        } catch (ParseException e) {
            Log.e(TAG, "Error analizando fecha: " + dateString, e);
            return new Date(); // Fecha predeterminada en caso de error
        }
    }

    // -------------------------
    // --- Envío de Mensajes ---
    // -------------------------

    private void sendAndStoreMessage(String messageText) {
        String encryptedMessage = messageText;
        // ENCRIPTAR AQUI TODO

        if (encryptedMessage == null) {
            Log.e(TAG, "La encriptación falló");
            return;
        }

        // Almacenar localmente primero
        storeMessageInDatabase(Integer.parseInt(contactId), true, messageText);
        refreshMessagesUI();

        // Actualizar ListContacts
        notifyContactListUpdate();

        // Luego enviar a la API
        MessageCreate messageCreate = new MessageCreate(currentUser, contactName, encryptedMessage);
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.sendMessage(messageCreate).enqueue(new retrofit2.Callback<SendMessageResponse>() {
            @Override
            public void onResponse(retrofit2.Call<SendMessageResponse> call, retrofit2.Response<SendMessageResponse> response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Error en la API: " + response.errorBody());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<SendMessageResponse> call, Throwable t) {
                Log.e(TAG, "Fallo al enviar mensaje", t);
            }
        });
    }

    private void notifyContactListUpdate() {
        // Crear lista con el id del contacto
        ArrayList<Integer> updatedContacts = new ArrayList<>();
        updatedContacts.add(Integer.parseInt(contactId));

        // Enviar el broadcast con la estructura esperada
        Intent updateIntent = new Intent("NEW_MESSAGES_ADDED");
        updateIntent.putIntegerArrayListExtra("updated_contacts", updatedContacts);
        LocalBroadcastManager.getInstance(this).sendBroadcast(updateIntent);
    }

    private void scheduleMessage(String message, long triggerAtMillis) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                Toast.makeText(this, "Por favor permite alarmas exactas", Toast.LENGTH_LONG).show();
                return;
            }
        }

        Intent intent = new Intent(this, MessageSchedulerReceiver.class);
        intent.putExtra("message", message);
        intent.putExtra("receiver", contactName);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
    }

    private void showDateTimePicker(String message) {
        Calendar currentTime = Calendar.getInstance();

        // Builder para DatePicker
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecciona una fecha")
                .setSelection(currentTime.getTimeInMillis())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.setTimeInMillis(selection);

            // Mostrar TimePicker
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(currentTime.get(Calendar.HOUR_OF_DAY))
                    .setMinute(currentTime.get(Calendar.MINUTE))
                    .setTitleText("Selecciona la hora")
                    .build();

            timePicker.addOnPositiveButtonClickListener(view -> {
                selectedDate.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                selectedDate.set(Calendar.MINUTE, timePicker.getMinute());

                long triggerTime = selectedDate.getTimeInMillis();
                scheduleMessage(message, triggerTime);

                SnackbarUtils.showSuccess(
                        Objects.requireNonNull(this.getCurrentFocus()),
                        this,
                        getString(R.string.message_scheduled) +
                                new SimpleDateFormat(getString(R.string.date_format), Locale.getDefault()).format(triggerTime)
                );
            });
            timePicker.show(getSupportFragmentManager(), "MATERIAL_TIME_PICKER");

        });
        datePicker.show(getSupportFragmentManager(), "MATERIAL_DATE_PICKER");
    }

    // -------------------------------------------------
    // --- Actualizaciones de la Interfaz de Usuario ---
    // -------------------------------------------------

    public void refreshMessagesUI() {
        List<Message> messages = getMessagesFromDatabase();
        MessagesAdapter adapter = new MessagesAdapter(messages);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(adapter);
        if (!messages.isEmpty()) {
            messagesRecyclerView.scrollToPosition(messages.size() - 1);
        }
    }
}