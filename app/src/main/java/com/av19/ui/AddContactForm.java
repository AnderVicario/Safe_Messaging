package com.av19.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.av19.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AddContactForm extends BaseLocaleActivity {

    private String currentUser;
    private byte[] contactPhoto = null;
    private ImageView iv_contact_icon;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.add_contact_form);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.surface));

        iv_contact_icon = findViewById(R.id.iv_contact_icon);
        iv_contact_icon.setImageResource(R.drawable.ic_launcher_background);
        iv_contact_icon.setOnClickListener(v -> {
            // Abrir selector de imágenes utilizando el ActivityResultLauncher
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        // Inicializar el launcher para seleccionar imagen
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null){
                Uri imageUri = result.getData().getData();
                try {
                    Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    iv_contact_icon.setImageBitmap(bitmap);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    contactPhoto = stream.toByteArray();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        currentUser = getSharedPreferences("session", MODE_PRIVATE)
                .getString("auth_token", null);
    }

    public void goToListContacts(View view) {
        finish();
    }

    public void addContact(View view) {
        TextView et_name = findViewById(R.id.et_name);
        String username = et_name.getText().toString().trim();

        if (username.isEmpty()) {
            Toast.makeText(this, "Ingrese un nombre de usuario", Toast.LENGTH_SHORT).show();
            return;
        }

        // Simular verificación exitosa y retornar el resultado
        returnResult(username);
    }

    private void returnResult(String username) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("new_contact_name", username);
        resultIntent.putExtra("new_contact_photo", contactPhoto);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}