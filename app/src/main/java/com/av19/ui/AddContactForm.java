package com.av19.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.av19.R;
import com.av19.models.api.PublicKeyResponse;
import com.av19.utils.ApiService;
import com.av19.utils.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddContactForm extends AppCompatActivity {

    private ApiService apiService;

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

        ImageView iv_contact_icon = findViewById(R.id.iv_contact_icon);
        iv_contact_icon.setImageResource(R.drawable.ic_launcher_background);
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
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

        // Verificar si el usuario existe en la API
        apiService.getPublicKey(username).enqueue(new Callback<PublicKeyResponse>() {
            @Override
            public void onResponse(Call<PublicKeyResponse> call, Response<PublicKeyResponse> response) {

                if (response.isSuccessful() && response.body() != null) {
                    // Usuario existe, obtener su clave pública
                    String publicKey = response.body().getPublic_key();
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
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}
