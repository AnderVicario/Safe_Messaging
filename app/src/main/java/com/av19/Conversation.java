package com.av19;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

public class Conversation extends AppCompatActivity {
    private TextView profileName;
    private RecyclerView messagesRecyclerView;
    private String contactId, contactName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversation);

        profileName = findViewById(R.id.profile_name);
        messagesRecyclerView = findViewById(R.id.messages);

        // Get data passed from ListContacts
        Intent intent = getIntent();
        contactId = intent.getStringExtra("contact_id");
        contactName = intent.getStringExtra("contact_name");

        // Display the contact's name in the toolbar
        profileName.setText(contactName);

        // Load the conversation messages for the contact using contactId
        loadConversation(contactId);
    }

    private void loadConversation(String contactId) {
        // Código para cargar los mensajes de la conversación de la base de datos
    }
}
