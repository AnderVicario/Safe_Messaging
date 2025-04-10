package com.av19.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import com.av19.models.api.PublicKeyResponse;
import com.av19.utils.ApiService;
import com.av19.utils.RetrofitClient;
import com.av19.utils.SnackbarUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddContactForm extends BaseLocaleActivity {

    private ApiService apiService;
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
                    iv_contact_icon.setImageBitmap(scaledBitmap);

                    // Convert to byte array for storage
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
                    contactPhoto = stream.toByteArray();

                    // Recycle the bitmaps to free memory
                    if (originalBitmap != croppedBitmap) {
                        originalBitmap.recycle();
                    }
                    if (croppedBitmap != scaledBitmap) {
                        croppedBitmap.recycle();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(AddContactForm.this, "Failed to process image", Toast.LENGTH_SHORT).show();
                }
            }
        });

        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
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
            SnackbarUtils.showWarning(
                    findViewById(android.R.id.content),
                    this,
                    getString(R.string.snackbar_warning_empty_user)
            );
        }

        // 1. Verificar si el usuario existe en la API
        apiService.getPublicKey(username).enqueue(new Callback<PublicKeyResponse>() {
            @Override
            public void onResponse(Call<PublicKeyResponse> call, Response<PublicKeyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Usuario existe, obtener su clave pública
                    String publicKey = response.body().getPublic_key();
                    returnResult(username, publicKey);
                }
                else {
                    SnackbarUtils.showError(
                            findViewById(android.R.id.content),
                            AddContactForm.this,
                            getString(R.string.snackbar_error_username_dontexist)
                    );
                }
            }

            @Override
            public void onFailure(Call<PublicKeyResponse> call, Throwable t) {
                Log.e("AddContactForm", "Fallo al recibir el usuario", t);
            }
        });
    }

    private void returnResult(String username, String publicKey) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("new_contact_name", username);
        resultIntent.putExtra("new_public_key", publicKey);
        resultIntent.putExtra("new_contact_photo", contactPhoto);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}