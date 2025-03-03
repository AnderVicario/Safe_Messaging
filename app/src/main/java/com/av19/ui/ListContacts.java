package com.av19.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.drawerlayout.widget.DrawerLayout;

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
import com.google.android.material.navigation.NavigationView;

public class ListContacts extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    ContactList contactList;
    ContactsAdapter contactsAdapter;
    RecyclerView recyclerView;
    FloatingActionButton button_add;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.list_contacts);

        setUpRecyclerView();

        setupNavigationDrawer();

        // Manejar pulsación del botón "atrás" para cerrar el drawer si está abierto
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void setupNavigationDrawer() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        // Configurar datos del usuario en el header del drawer
        View headerView = navigationView.getHeaderView(0);
        // Personalizar elementos del header si es necesario
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_language) {
            // Acción para perfil de usuario
        } else if (id == R.id.nav_theme) {
            // Acción para mostrar conversaciones con más mensajes
        } else if (id == R.id.nav_about) {
            // Acción para "Acerca de"
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
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
                        byte[] newContactPhoto = data.getByteArrayExtra("new_contact_photo");
                        if (newContactName != null && newPublicKey != null) {
                            int index = ContactList.getInstance(this).addContact(newContactName, newPublicKey, newContactPhoto,this);
                            contactsAdapter.notifyItemInserted(index);
                        }
                    }
                }
            }
    );
}