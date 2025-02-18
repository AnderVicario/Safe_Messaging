package com.av19.ui;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.av19.models.api.UserLogin;
import com.av19.utils.ApiService;
import com.av19.utils.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginMenu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String token = prefs.getString("auth_token", null);

        if (token != null) {
            Intent intent = new Intent(this, ListContacts.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.login_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.surface));


        EditText passwordInput = findViewById(R.id.register_password_input1);
        EditText usernameInput = findViewById(R.id.register_username_input);

        usernameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                usernameInput.setTextColor(ContextCompat.getColor(LoginMenu.this, R.color.onBackground));
                passwordInput.setTextColor(ContextCompat.getColor(LoginMenu.this, R.color.onBackground));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                usernameInput.setTextColor(ContextCompat.getColor(LoginMenu.this, R.color.onBackground));
                passwordInput.setTextColor(ContextCompat.getColor(LoginMenu.this, R.color.onBackground));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        Button loginButton = findViewById(R.id.send_button);
        TextView textRegister = findViewById(R.id.textView);

        loginButton.setOnClickListener(v -> performLogin());
        textRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterMenu.class)));
    }

    private void performLogin() {
        EditText usernameInput = findViewById(R.id.register_username_input);
        EditText passwordInput = findViewById(R.id.register_password_input1);

        String username = usernameInput.getText().toString();
        String password = passwordInput.getText().toString();

        // Llamada a la API
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        UserLogin userLoginData = new UserLogin(username, password);

        // Realizar la llamada al endpoint de login
        Call<ApiResponse> call = apiService.loginUser(userLoginData);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();

                    SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
                    prefs.edit().putString("auth_token", username).apply();

                    Toast.makeText(getApplicationContext(), "Login exitoso: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginMenu.this, ListContacts.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "Error en el login", Toast.LENGTH_SHORT).show();
                    usernameInput.setTextColor(ContextCompat.getColor(LoginMenu.this, R.color.error));
                    passwordInput.setTextColor(ContextCompat.getColor(LoginMenu.this, R.color.error));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }
}