package com.av19.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.av19.R;
import com.av19.adapters.MessagesAdapter;
import com.av19.models.Message;
import com.av19.utils.DatabaseHelper;

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
    private TextView profileName;
    private RecyclerView messagesRecyclerView;
    private String contactId, contactName, contactPublicKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.conversation);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.surface));

        profileName = findViewById(R.id.tv_contact_name);
        messagesRecyclerView = findViewById(R.id.rview_messages);

        // Get data passed from ListContacts
        Intent intent = getIntent();
        contactId = intent.getStringExtra("contact_id");
        contactName = intent.getStringExtra("contact_name");
        contactPublicKey = intent.getStringExtra("contact_public_key");

        // Display the contact's name in the toolbar
        profileName.setText(contactName);

        // Load the conversation messages for the contact using contactId
        loadConversation(contactId);
    }

    private void loadConversation(String contactId) {
        // 1. Obtener instancia de la base de datos
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();

        // 2. Consultar mensajes ordenados por fecha
        Cursor cursor = db.rawQuery(
                "SELECT message, is_sender, sent_at FROM messages " +
                        "WHERE contact_id = ? ORDER BY sent_at ASC",
                new String[]{contactId}
        );

        // 3. Procesar resultados
        List<Message> messages = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        while (cursor.moveToNext()) {
            try {
                String text = cursor.getString(cursor.getColumnIndexOrThrow("message"));
                boolean isSender = cursor.getInt(cursor.getColumnIndexOrThrow("is_sender")) == 1;
                String sentAtString = cursor.getString(cursor.getColumnIndexOrThrow("sent_at"));

                Date sentAt = sdf.parse(sentAtString);

                messages.add(new Message(text, isSender, sentAt));
            } catch (ParseException e) {
                Log.e("Conversation", "Error parsing date", e);
            }
        }

        cursor.close();
        db.close();

        // 4. Configurar RecyclerView con adaptador
        MessagesAdapter adapter = new MessagesAdapter(messages);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(adapter);
        messagesRecyclerView.scrollToPosition(messages.size() - 1); // Ir al último mensaje
    }
}
