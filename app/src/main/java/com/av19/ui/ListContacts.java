package com.av19.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.av19.R;
import com.av19.adapters.ContactsAdapter;
import com.av19.models.Contact;
import com.av19.models.ContactList;
import com.av19.models.api.MessageResponse;
import com.av19.utils.AESEncryptionManager;
import com.av19.utils.ApiService;
import com.av19.utils.BackgroundWebSocketService;
import com.av19.utils.DatabaseHelper;
import com.av19.utils.RSAEncryptionManager;
import com.av19.utils.RetrofitClient;
import com.av19.utils.SnackbarUtils;
import com.av19.utils.WebSocketClient;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ListContacts extends BaseLocaleActivity implements NavigationView.OnNavigationItemSelectedListener, EditContactDialogFragment.EditContactDialogListener {

    private ContactList contactList;
    private ContactsAdapter contactsAdapter;
    private RecyclerView recyclerView;
    private FloatingActionButton button_add;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private String currentUser;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private static final String PREFS_NAME = "settings";
    private static final String KEY_THEME = "theme";
    private static final String KEY_LANG = "lang";
    private static final String TAG = "ListContacts";

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

        if (getIntent().getBooleanExtra("register_success", false)) {
            SnackbarUtils.showSuccess(
                    findViewById(android.R.id.content),
                    this,
                    getString(R.string.snackbar_success_register)
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

        // Conectar el WebSocket desde el manager
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("NEW_MESSAGE"));
        fetchMessages();
        Log.d("ListContacts", "onCreate");
    }

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Actualiza la UI o refresca la lista de mensajes
            Log.d("ListContacts", "Mensaje recibido");
            fetchMessages();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("NEW_MESSAGE"));
        /*fetchMessages();*/
    }

    @Override
    protected void onPause() {
        Log.d("ListContacts", "onPause");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        super.onPause();
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
        ImageView user_profile_image = headerView.findViewById(R.id.user_profile_image);
        user_profile_image.setImageResource(R.drawable.ic_launcher_background);
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null){
                Uri imageUri = result.getData().getData();
                try {
                    // Load the original bitmap
                    Bitmap originalBitmap = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

                    // Get the dimensions
                    int width = originalBitmap.getWidth();
                    int height = originalBitmap.getHeight();

                    // Determine the square size (use the smaller dimension)
                    int squareSize = Math.min(width, height);

                    // Calculate cropping coordinates to get center of image
                    int x = (width - squareSize) / 2;
                    int y = (height - squareSize) / 2;

                    // Create a square cropped bitmap (1:1 aspect ratio)
                    Bitmap croppedBitmap = Bitmap.createBitmap(
                            originalBitmap,
                            x,
                            y,
                            squareSize,
                            squareSize
                    );

                    // Scale down the image if it's too large
                    int targetSize = 500; // You can adjust this target size as needed
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                            croppedBitmap,
                            targetSize,
                            targetSize,
                            true
                    );

                    // Set the processed image to the ImageView
                    user_profile_image.setImageBitmap(scaledBitmap);

                    // Convert to byte array for storage
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
                    byte[] userPhoto = stream.toByteArray();
                    // Agregar para guardar la foto en la base de datos y actualizar el usuario en la API. Tambien se deberia cargar antes, y cargar la del resto de usuarios.

                    // Recycle the bitmaps to free memory
                    if (originalBitmap != croppedBitmap) {
                        originalBitmap.recycle();
                    }
                    if (croppedBitmap != scaledBitmap) {
                        croppedBitmap.recycle();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(ListContacts.this, "Failed to process image", Toast.LENGTH_SHORT).show();
                }
            }
        });
        user_profile_image.setOnLongClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
            return true;
        });

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
        } else if (id == R.id.nav_close_account) {
            SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("auth_token");
            editor.apply();

            stopService(new Intent(this, BackgroundWebSocketService.class));
            DatabaseHelper.removeInstance(currentUser);
            RSAEncryptionManager.removeInstance(currentUser);
            ContactList.removeInstance(currentUser);

            Intent intent = new Intent(this, SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
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
        contactList = ContactList.getInstance(this, currentUser);
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
                            int index = ContactList.getInstance(this, currentUser).addContact(newContactName, newPublicKey, newContactPhoto,this);
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
        ContactList.getInstance(this, currentUser).updateContact(contactId, newName, contactPhoto);
        for (Contact c : contactList.getContacts()) {
            if (c.getId() == contactId) {
                c.setName(newName);
                c.setPhoto(contactPhoto);
                break;
            }
        }
        contactsAdapter.notifyDataSetChanged();
    }

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

    private void processMessages(List<MessageResponse> messagesResponse) {
        List<String> localTimestamps = getLocalMessageTimestamps();

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

            boolean messageIsSender;
            messageIsSender = sender.equals(currentUser);
            storeMessageInDatabase(sender, messageIsSender, decryptedMessage, sentAtStr);
        }
        refreshMessagesUI();
    }

    private List<String> getLocalMessageTimestamps() {
        List<String> timestamps = new ArrayList<>();
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this, currentUser);
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

    private void storeMessageInDatabase(String contact, boolean isSender, String message, String timestamp) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this, currentUser);
        SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();

        try {
            try (Cursor cursor = db.rawQuery(
                    "SELECT id FROM contacts WHERE name = ?",
                    new String[]{contact}
            )) {
                if (cursor.moveToFirst()) {
                    int contactId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
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
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Excepción al almacenar mensaje", e);
        } finally {
            db.close();
        }
    }
}