package com.av19.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.av19.models.Contact;
import com.av19.utils.SnackbarUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

import java.util.Locale;

public class ListContacts extends BaseLocaleActivity implements NavigationView.OnNavigationItemSelectedListener, EditContactDialogFragment.EditContactDialogListener {

    private ContactList contactList;
    private ContactsAdapter contactsAdapter;
    private RecyclerView recyclerView;
    private FloatingActionButton button_add;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private String currentUser;
    private static final String PREFS_NAME = "settings";
    private static final String KEY_THEME = "theme";
    private static final String KEY_LANG = "lang";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.list_contacts);

        if (getIntent().getBooleanExtra("login_success", false)) {
            SnackbarUtils.showSuccess(
                    findViewById(android.R.id.content),
                    this,
                    getString(R.string.snackbar_success_login)
            );
        }

        currentUser = getSharedPreferences("session", MODE_PRIVATE)
                .getString("auth_token", null);

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

    @Override
    protected void onResume() {
        super.onResume();

        refreshMessagesUI();
    }

    private void refreshMessagesUI() {
        contactList.reloadContacts(this);
        contactsAdapter.notifyDataSetChanged();
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
        TextView user_name = headerView.findViewById(R.id.user_name);
        user_name.setText(currentUser);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_language) {
            showLanguageDialog();
        } else if (id == R.id.nav_theme) {
            showThemeDialog();
        } else if (id == R.id.nav_about) {
            showAboutDialog();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String langCode = prefs.getString(KEY_LANG, "es"); // Valor por defecto "es" (Español)
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        config.setLocale(locale);
        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }

    private void showLanguageDialog() {
        final String[] languages = {
                "Euskera",   // Ej: "Euskera"
                "English",    // Ej: "English"
                "Castellano"       // Ej: "Español"
        };

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentLang = prefs.getString(KEY_LANG, "es");

        int checkedItem;
        if (currentLang.equals("eu")) {
            checkedItem = 0;
        } else if (currentLang.equals("en")) {
            checkedItem = 1;
        } else {
            checkedItem = 2;
        }

        new MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
                .setTitle(R.string.drawer_item_language)
                .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                    String langCode;
                    switch (which) {
                        case 0:
                            langCode = "eu";
                            break;
                        case 1:
                            langCode = "en";
                            break;
                        case 2:
                            langCode = "es";
                            break;
                        default:
                            langCode = "es";
                    }

                    setLocale(langCode);
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }


    private void setLocale(String langCode) {
        // Guarda el idioma seleccionado
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        prefs.edit().putString(KEY_LANG, langCode).apply();

        // Reinicia la Activity para que attachBaseContext se invoque y aplique el nuevo idioma
        recreate();
    }


    private void showThemeDialog() {
        final String[] themes = {
                getString(R.string.default_theme),
                getString(R.string.dark_theme),
                getString(R.string.light_theme)
        };

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int currentTheme = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        int checkedItem;
        if (currentTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            checkedItem = 1;
        } else if (currentTheme == AppCompatDelegate.MODE_NIGHT_NO) {
            checkedItem = 2;
        } else {
            checkedItem = 0;
        }

        new MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
                .setTitle(R.string.drawer_item_theme)
                .setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
                    int newMode;
                    switch (which) {
                        case 1:
                            newMode = AppCompatDelegate.MODE_NIGHT_YES;
                            break;
                        case 2:
                            newMode = AppCompatDelegate.MODE_NIGHT_NO;
                            break;
                        default:
                            newMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                    }

                    // Aplicar y guardar
                    AppCompatDelegate.setDefaultNightMode(newMode);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt(KEY_THEME, newMode);
                    editor.apply();

                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showAboutDialog() {
        String versionName = "";
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.RoundedDialog);
        builder.setTitle(R.string.about_title)
                .setMessage(getString(R.string.app_name) + " " + versionName + "\n\n" +
                        getString(R.string.about_message) + "\n\n" +
                        getString(R.string.about_copyright))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .setIcon(R.mipmap.ic_logo)
                .show();
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
                        byte[] newContactPhoto = data.getByteArrayExtra("new_contact_photo");
                        if (newContactName != null) {
                            int index = ContactList.getInstance(this).addContact(newContactName, newContactPhoto,this);
                            contactsAdapter.notifyItemInserted(index);
                            SnackbarUtils.showSuccess(
                                    findViewById(android.R.id.content),
                                    this,
                                    getString(R.string.snackbar_contact_added)
                            );
                        }
                    }
                }
            }
    );

    @Override
    public void onContactEdited(int contactId, String newName, byte[] contactPhoto) {
        ContactList.getInstance(this).updateContact(contactId, newName, contactPhoto);
        for (Contact c : contactList.getContacts()) {
            if (c.getId() == contactId) {
                c.setName(newName);
                c.setPhoto(contactPhoto);
                break;
            }
        }
        contactsAdapter.notifyDataSetChanged();
    }
}