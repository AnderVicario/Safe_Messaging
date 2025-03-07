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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.av19.R;
import com.av19.models.api.ApiResponse;
import com.av19.models.api.MessageCreate;
import com.av19.models.api.MessageResponse;
import com.av19.models.api.PublicKeyResponse;
import com.av19.utils.AESEncryptionManager;
import com.av19.utils.ApiService;
import com.av19.utils.RSAEncryptionManager;
import com.av19.utils.RetrofitClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.crypto.SecretKey;

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
            Toast.makeText(this, "Ingrese un nombre de usuario", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Verificar si el usuario existe en la API
        apiService.getPublicKey(username).enqueue(new Callback<PublicKeyResponse>() {
            @Override
            public void onResponse(Call<PublicKeyResponse> call, Response<PublicKeyResponse> response) {

                if (response.isSuccessful() && response.body() != null) {
                    // Usuario existe, obtener su clave pública
                    String publicKey = response.body().getPublic_key();

                    // 2. Verificar si ya he recibido un mensaje inicial (la clave AES)
                    apiService.getMessages(currentUser).enqueue(new Callback<List<MessageResponse>>() {
                        @Override
                        public void onResponse(Call<List<MessageResponse>> call, Response<List<MessageResponse>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                MessageResponse initialMessage = null;
                                for (MessageResponse msg : response.body()) {
                                    if (msg.getIs_initial()) {
                                        initialMessage = msg;
                                        break;
                                    }
                                }

                                if (initialMessage != null) {
                                    String encryptedAESKey = initialMessage.getEncrypted_message();
                                    try {
                                        SecretKey aesKey = RSAEncryptionManager.getInstance(Boolean.FALSE, currentUser).decryptAESKeyWithRSA(encryptedAESKey);
                                        AESEncryptionManager.storeAESKey(username, aesKey);
                                    }
                                    catch (Exception e){
                                        e.printStackTrace();
                                    }
                                } else {
                                    // 3. Genera la clave AES
                                    SecretKey aesKey = null;
                                    try {
                                        aesKey = AESEncryptionManager.generateAESKey();
                                        String encryptedAESKey = RSAEncryptionManager.getInstance(Boolean.FALSE, currentUser).encryptAESKeyWithRSA(aesKey, publicKey);
                                        MessageCreate messageCreate = new MessageCreate(currentUser, username, encryptedAESKey);
                                        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
                                        SecretKey finalAesKey = aesKey;
                                        apiService.sendMessage(messageCreate).enqueue(new retrofit2.Callback<ApiResponse>() {
                                            @Override
                                            public void onResponse(retrofit2.Call<ApiResponse> call, retrofit2.Response<ApiResponse> response) {
                                                if (response.isSuccessful()) {
                                                    Log.e("AddContactForm", "AES enviado correctamente.");
                                                    try {
                                                        AESEncryptionManager.storeAESKey(username, finalAesKey);
                                                    }
                                                    catch (Exception e){
                                                        e.printStackTrace();
                                                    }
                                                } else {
                                                    Log.e("AddContactForm", "Error en la respuesta de la API: " + response.errorBody());
                                                }
                                            }

                                            @Override
                                            public void onFailure(retrofit2.Call<ApiResponse> call, Throwable t) {
                                                Log.e("Conversation", "Fallo al enviar el mensaje", t);
                                            }
                                        });
                                    }
                                    catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        @Override
                        public void onFailure(Call<List<MessageResponse>> call, Throwable t) {
                            // Manejar error
                        }
                    });
                    returnResult(username, publicKey);
                } else {
                    Toast.makeText(AddContactForm.this, "Error al verificar usuario", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PublicKeyResponse> call, Throwable t) {
                Toast.makeText(AddContactForm.this, "Error de conexión", Toast.LENGTH_SHORT).show();
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
