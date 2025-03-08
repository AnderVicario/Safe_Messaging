package com.av19.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.av19.R;
import com.av19.utils.DatabaseHelper;
import com.av19.utils.SnackbarUtils;

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

        if (getIntent().getBooleanExtra("register_success", false)) {
            SnackbarUtils.showSuccess(
                    findViewById(android.R.id.content),
                    this,
                    getString(R.string.snackbar_success_register)
            );
        }

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

        // Implementar verificación local
        if (isValidCredentials(username, password)) {
            // Guardar credenciales en SharedPreferences
            SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
            prefs.edit().putString("auth_token", username).apply();

            // Establecer contraseña para encriptación de base de datos
            DatabaseHelper.setPassword(LoginMenu.this, password);

            SnackbarUtils.showSuccess(
                    findViewById(android.R.id.content), this, getString(R.string.snackbar_success_login)
            );
            navigateToContactsList();
        } else {
            // Mostrar error
            SnackbarUtils.showError(
                    findViewById(android.R.id.content), this, getString(R.string.snackbar_error_login)
            );
            usernameInput.setTextColor(ContextCompat.getColor(LoginMenu.this, R.color.error));
            passwordInput.setTextColor(ContextCompat.getColor(LoginMenu.this, R.color.error));
        }
    }

    private boolean isValidCredentials(String username, String password) {
        SharedPreferences userPrefs = getSharedPreferences("registered_users", MODE_PRIVATE);
        String storedPassword = userPrefs.getString(username, null);

        if (storedPassword != null) {
            // Si el usuario está registrado, comparamos la contraseña
            return storedPassword.equals(password);
        } else {
            return false;
        }
    }

    private void navigateToContactsList() {
        Intent intent = new Intent(LoginMenu.this, ListContacts.class);
        intent.putExtra("login_success", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}