package com.av19;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ListContacts extends AppCompatActivity {

    ContactList contactList;
    ContactAdapter contactAdapter;
    RecyclerView recyclerView;
    FloatingActionButton button_add;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.list_contacts);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        setUpRecyclerView();
    }

    private void setUpRecyclerView() {
        contactList = new ContactList();
        contactAdapter = new ContactAdapter(contactList);
        button_add = findViewById(R.id.button_add);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setAdapter(contactAdapter);
    }

    public void goToAddContactForm(View view) {
        Intent intent = new Intent(this, AddContactForm.class);
        startActivity(intent);
    }
}