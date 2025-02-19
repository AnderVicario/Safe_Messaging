package com.av19.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.av19.R;
import com.av19.adapters.ContactsAdapter;
import com.av19.models.ContactList;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ListContacts extends AppCompatActivity {

    ContactList contactList;
    ContactsAdapter contactsAdapter;
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
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.surface));

        setUpRecyclerView();
    }

    private void setUpRecyclerView() {
        contactList = ContactList.getInstance(this);
        contactsAdapter = new ContactsAdapter(contactList, this);
        button_add = findViewById(R.id.button_add);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setAdapter(contactsAdapter);
    }

    public void goToAddContactForm(View view) {
        Intent intent = new Intent(this, AddContactForm.class);
        addContactLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> addContactLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        String newContactName = data.getStringExtra("new_contact_name");
                        String newPublicKey = data.getStringExtra("new_public_key");
                        if (newContactName != null && newPublicKey != null) {
                            int index = ContactList.getInstance(this).addContact(newContactName, newPublicKey);
                            contactsAdapter.notifyItemInserted(index);
                        }
                    }
                }
            }
    );
}