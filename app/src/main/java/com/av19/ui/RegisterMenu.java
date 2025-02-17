package com.av19.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.av19.R;
import com.av19.models.api.ApiResponse;
import com.av19.models.api.PublicKeyResponse;
import com.av19.models.api.UserCreate;
import com.av19.models.api.UserLogin;
import com.av19.utils.ApiService;
import com.av19.utils.RSAEncryptionManager;
import com.av19.utils.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterMenu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.register_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.surface));


        EditText passwordInput1 = findViewById(R.id.register_password_input1);
        EditText passwordInput2 = findViewById(R.id.register_password_input2);
        EditText usernameInput = findViewById(R.id.register_username_input);

        usernameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                usernameInput.setTextColor(ContextCompat.getColor(RegisterMenu.this, R.color.onBackground));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        passwordInput1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                passwordInput1.setTextColor(ContextCompat.getColor(RegisterMenu.this, R.color.onBackground));
                passwordInput2.setTextColor(ContextCompat.getColor(RegisterMenu.this, R.color.onBackground));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        passwordInput2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                passwordInput1.setTextColor(ContextCompat.getColor(RegisterMenu.this, R.color.onBackground));
                passwordInput2.setTextColor(ContextCompat.getColor(RegisterMenu.this, R.color.onBackground));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        Button registerButton = findViewById(R.id.send_button);
        TextView textLogin = findViewById(R.id.textView);

        registerButton.setOnClickListener(v -> performRegister());
        textLogin.setOnClickListener(v -> startActivity(new Intent(this, ListContacts.class)));
    }

    private void performRegister() {
        EditText usernameInput = findViewById(R.id.register_username_input);
        EditText passwordInput1 = findViewById(R.id.register_password_input1);
        EditText passwordInput2 = findViewById(R.id.register_password_input2);

        String username = usernameInput.getText().toString();
        String password1 = passwordInput1.getText().toString();
        String password2 = passwordInput2.getText().toString();

        // Comprobar si las contraseñas coindicen
        if (!password1.equals(password2)) {
            Toast.makeText(getApplicationContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            passwordInput1.setTextColor(ContextCompat.getColor(RegisterMenu.this, R.color.error));
            passwordInput2.setTextColor(ContextCompat.getColor(RegisterMenu.this, R.color.error));
            return;
        }

        // 1. Llamada a la API para comprobar si existe el usuario
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        // Realizar la llamada al endpoint para obtener la clave pública de un usuario
        Call<PublicKeyResponse> call = apiService.getPublicKey(username);
        call.enqueue(new Callback<PublicKeyResponse>() {
            @Override
            public void onResponse(Call<PublicKeyResponse> call, Response<PublicKeyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(getApplicationContext(), "Ya existe un usuario con ese nombre.", Toast.LENGTH_SHORT).show();
                } else {
                    // Crear par de llaves
                    RSAEncryptionManager rsaEncryptionManager = RSAEncryptionManager.getInstance();
                    String publicKey = rsaEncryptionManager.getPublicKey();

                    // 2. Llamada a la API para registrar el usuario
                    ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
                    UserCreate userCreateData = new UserCreate(username, password1, publicKey);

                    // Realizar la llamada al endpoint de register
                    Call<ApiResponse> registerCall = apiService.registerUser(userCreateData);
                    registerCall.enqueue(new Callback<ApiResponse>() {
                        @Override
                        public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                ApiResponse apiResponse = response.body();
                                Toast.makeText(getApplicationContext(), "Register exitoso: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegisterMenu.this, ListContacts.class));
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse> call, Throwable t) {
                            Toast.makeText(getApplicationContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<PublicKeyResponse> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}