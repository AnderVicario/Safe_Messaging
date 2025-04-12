package com.av19.ui;

import android.Manifest;
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
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
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
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.av19.R;
import com.av19.adapters.ContactsAdapter;
import com.av19.models.Contact;
import com.av19.models.ContactList;
import com.av19.models.Message;
import com.av19.models.api.ApiResponse;
import com.av19.models.api.RecieveMessageResponse;
import com.av19.models.api.UpdateProfilePicture;
import com.av19.utils.ApiService;
import com.av19.utils.BackgroundWebSocketService;
import com.av19.utils.DatabaseHelper;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ListContacts extends BaseLocaleActivity implements NavigationView.OnNavigationItemSelectedListener, EditContactDialogFragment.EditContactDialogListener {

    private ContactList contactList;
    private ContactsAdapter contactsAdapter;
    private RecyclerView recyclerView;
    private FloatingActionButton button_add;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private String currentUser;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private Uri cameraImageUri;
    private static final String PREFS_NAME = "settings";
    private static final String KEY_THEME = "theme";
    private static final String KEY_LANG = "lang";
    private static final String TAG = "ListContacts";
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Permiso para la cámara
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        openCamera();
                    }
                }
            });

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

        // Gestionar la pulsación del botón "atrás" para cerrar el drawer si está abierto
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

        contactList.setOnContactPhotoUpdatedListener(new ContactList.OnContactPhotoUpdatedListener() {
            @Override
            public void onContactPhotoUpdated(int contactId) {
                runOnUiThread(() -> {
                    int position = -1;
                    for (int i = 0; i < contactList.getContacts().size(); i++) {
                        if (contactList.getContacts().get(i).getId() == contactId) {
                            position = i;
                            break;
                        }
                    }
                    if (position != -1) {
                        contactsAdapter.notifyItemChanged(position);
                    }
                });
            }
        });
        checkNotificationPermission();
        refreshMessagesUI(true);

        // Conectar el WebSocket desde el manager
        Log.d("ListContacts", "Registrar websocket");
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("NEW_MESSAGES_ADDED"));
    }

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("NEW_MESSAGES_ADDED".equals(intent.getAction())) {
                ArrayList<Integer> updatedContacts = intent.getIntegerArrayListExtra("updated_contacts");
                if (updatedContacts != null) {
                    refreshMessagesUI(false);
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        Log.d("ListContacts", "Desregistrar websocket");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        super.onDestroy();
    }

    private void refreshMessagesUI(Boolean reloadPictures) {
        contactList.reloadContacts(this, reloadPictures);
        contactList.sortContacts();
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

        SharedPreferences prefs = getSharedPreferences("session", Context.MODE_PRIVATE);
        String encodedImage = prefs.getString("profile_picture", null);
        if (encodedImage != null) {
            Bitmap savedBitmap = decodeBitmapFromBase64(encodedImage);
            if(savedBitmap != null) {
                user_profile_image.setImageBitmap(savedBitmap);
            }
        } else {
            user_profile_image.setImageResource(R.drawable.ic_launcher_background);
        }

        // Inicializar los launchers para cámara y galería
        initializeImageLaunchers(user_profile_image);

        user_profile_image.setOnClickListener(v -> {
            showImageSourceDialog();
        });

        TextView user_name = headerView.findViewById(R.id.user_name);
        user_name.setText(currentUser);

        TextView user_status = headerView.findViewById(R.id.user_status);
        user_status.setText(WebSocketClient.getInstance().isConnected() ? getString(R.string.drawer_status_online) : getString(R.string.drawer_status_offline));
    }

    private void initializeImageLaunchers(ImageView user_profile_image) {
        // Inicializar launcher para seleccionar imagen de la galería
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null){
                Uri imageUri = result.getData().getData();
                try {
                    Bitmap originalBitmap = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    processAndUploadImage(originalBitmap, user_profile_image);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(ListContacts.this, "Failed to process image", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Inicializar launcher para tomar foto con la cámara
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                try {
                    Bitmap originalBitmap = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), cameraImageUri);
                    processAndUploadImage(originalBitmap, user_profile_image);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(ListContacts.this, "Failed to process image", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showImageSourceDialog() {
        String[] options = {getString(R.string.take_photo), getString(R.string.choose_from_gallery)};

        new MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
                .setTitle(getString(R.string.select_photo))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Cámara
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            openCamera();
                        } else {
                            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
                        }
                    } else {
                        // Galería
                        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        pickImageLauncher.launch(intent);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(android.provider.MediaStore.Images.Media.TITLE, "New Picture");
        values.put(android.provider.MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        cameraImageUri = getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cameraImageUri);
        cameraLauncher.launch(cameraIntent);
    }

    private void processAndUploadImage(Bitmap originalBitmap, ImageView user_profile_image) {
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();
        int squareSize = Math.min(width, height);
        int x = (width - squareSize) / 2;
        int y = (height - squareSize) / 2;

        Bitmap croppedBitmap = Bitmap.createBitmap(
                originalBitmap,
                x,
                y,
                squareSize,
                squareSize
        );

        int targetSize = 500;
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                croppedBitmap,
                targetSize,
                targetSize,
                true
        );

        user_profile_image.setImageBitmap(scaledBitmap);

        // Guardar la imagen en SharedPreferences
        SharedPreferences prefs = getSharedPreferences("session", Context.MODE_PRIVATE);
        String newEncodedImage = encodeBitmapToBase64(scaledBitmap);
        prefs.edit().putString("profile_picture", newEncodedImage).apply();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
        byte[] userPhoto = stream.toByteArray();
        String photoBase64 = Base64.getEncoder().encodeToString(userPhoto);

        // Liberar recursos de Bitmaps
        if (originalBitmap != croppedBitmap) {
            originalBitmap.recycle();
        }
        if (croppedBitmap != scaledBitmap) {
            croppedBitmap.recycle();
        }

        // Crear la solicitud para actualizar la foto de perfil
        UpdateProfilePicture updatePicRequest = new UpdateProfilePicture(currentUser, DatabaseHelper.getInstance(this, currentUser).getDecryptedPassword(), photoBase64);
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<ApiResponse> call = apiService.updateProfilePicture(updatePicRequest);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if(response.isSuccessful()){
                    SnackbarUtils.showSuccess(
                            findViewById(android.R.id.content), ListContacts.this, getString(R.string.snackbar_success_update_profile_picture)
                    );
                } else {
                    SnackbarUtils.showError(
                            findViewById(android.R.id.content), ListContacts.this, getString(R.string.snackbar_error_update_profile_picture)
                    );
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                SnackbarUtils.showError(
                        findViewById(android.R.id.content), ListContacts.this, getString(R.string.snackbar_error_update_profile_picture)
                );
            }
        });
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
            editor.remove("profile_picture");
            editor.apply();

            stopService(new Intent(this, BackgroundWebSocketService.class));
            DatabaseHelper.removeInstance(currentUser);
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

    // Convertir un Bitmap a una cadena Base64
    private static String encodeBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.getEncoder().encodeToString(byteArray);
    }

    // Convertir una cadena Base64 a Bitmap
    private static Bitmap decodeBitmapFromBase64(String base64String) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64String);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}