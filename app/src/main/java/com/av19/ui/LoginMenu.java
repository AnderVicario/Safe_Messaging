package com.av19.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.av19.R;
import com.av19.models.api.ApiResponse;
import com.av19.models.api.ProfilePictureResponse;
import com.av19.models.api.UserLogin;
import com.av19.utils.ApiService;
import com.av19.utils.BackgroundWebSocketService;
import com.av19.utils.DatabaseHelper;
import com.av19.utils.RetrofitClient;
import com.av19.utils.SnackbarUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginMenu extends BaseLocaleActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Comprobar si ya hay una sesión activa
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String username = prefs.getString("auth_token", null);

        if (username != null) {
            // Si ya hay una sesión activa, ir directamente a la lista de contactos
            navigateToContactsList();
            return;
        }

        // Configuración de la UI
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

        // Referencias a componentes de la UI
        EditText passwordInput = findViewById(R.id.et_register_password_1);
        EditText usernameInput = findViewById(R.id.et_register_username);
        Button loginButton = findViewById(R.id.btn_send);
        TextView textRegister = findViewById(R.id.tv_toggle_reg_login);

        // Configurar listeners para restablecer colores cuando el usuario escribe
        setupTextChangeListeners(usernameInput, passwordInput);

        // Configurar listeners de clic
        loginButton.setOnClickListener(v -> performLocalLogin());
        textRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterMenu.class)));
    }

    private void setupTextChangeListeners(EditText usernameInput, EditText passwordInput) {
        TextWatcher textWatcher = new TextWatcher() {
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
        };

        usernameInput.addTextChangedListener(textWatcher);
        passwordInput.addTextChangedListener(textWatcher);
    }

    private void performLocalLogin() {
        EditText usernameInput = findViewById(R.id.et_register_username);
        EditText passwordInput = findViewById(R.id.et_register_password_1);

        String username = usernameInput.getText().toString();
        String password = passwordInput.getText().toString();

        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        // Si el usuario o la contraseña están vacíos, mostrar error
        if (username.isEmpty() || password.isEmpty()) {
            SnackbarUtils.showWarning(
                    findViewById(android.R.id.content), this, getString(R.string.snackbar_warning_empty_fields)
            );
            return;
        }

        // Llamada a la API
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        UserLogin userLoginData = new UserLogin(username, password);
        Call<ApiResponse> call = apiService.loginUser(userLoginData);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
                    prefs.edit().putString("auth_token", username).apply();
                    recoverPicture(username);

                    DatabaseHelper.getInstance(LoginMenu.this, username).setPassword(password);

                    SnackbarUtils.showSuccess(
                            findViewById(android.R.id.content), LoginMenu.this, getString(R.string.snackbar_success_login)
                    );
                } else {
                    SnackbarUtils.showError(
                            findViewById(android.R.id.content), LoginMenu.this, getString(R.string.snackbar_error_login)
                    );
                    usernameInput.setTextColor(ContextCompat.getColor(LoginMenu.this, R.color.error));
                    passwordInput.setTextColor(ContextCompat.getColor(LoginMenu.this, R.color.error));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                SnackbarUtils.showError(
                        findViewById(android.R.id.content), LoginMenu.this, getString(R.string.snackbar_server_error_login)
                );
            }
        });
    }

    private void navigateToContactsList() {
        startService(new Intent(LoginMenu.this, BackgroundWebSocketService.class));
        Intent intent = new Intent(LoginMenu.this, ListContacts.class);
        intent.putExtra("login_success", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void recoverPicture(String token) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<ProfilePictureResponse> call = apiService.getProfilePicture(token);
        call.enqueue(new Callback<ProfilePictureResponse>() {
            @Override
            public void onResponse(Call<ProfilePictureResponse> call, Response<ProfilePictureResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String profilePictureBase64 = response.body().getProfile_picture();
                    if (profilePictureBase64 != null) {
                        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
                        prefs.edit().putString("profile_picture", profilePictureBase64).apply();
                    }
                } else {
                    Log.e("SplashActivity", "No hay una foto para el usuario: " + token);
                }
                navigateToContactsList();
            }

            @Override
            public void onFailure(Call<ProfilePictureResponse> call, Throwable t) {
                Log.e("SplashActivity", "Error fetching updated photo for contact: " + token, t);
            }
        });
    }
}