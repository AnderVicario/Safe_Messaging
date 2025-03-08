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

public class RegisterMenu extends BaseLocaleActivity {

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
        setContentView(R.layout.register_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.surface));

        EditText passwordInput1 = findViewById(R.id.et_register_password_1);
        EditText passwordInput2 = findViewById(R.id.et_register_password_2);
        EditText usernameInput = findViewById(R.id.et_register_username);

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

        Button registerButton = findViewById(R.id.btn_send);
        TextView textLogin = findViewById(R.id.tv_toggle_reg_login);

        registerButton.setOnClickListener(v -> performRegister());
        textLogin.setOnClickListener(v -> startActivity(new Intent(this, ListContacts.class)));
    }

    private void performRegister() {
        EditText usernameInput = findViewById(R.id.et_register_username);
        EditText passwordInput1 = findViewById(R.id.et_register_password_1);
        EditText passwordInput2 = findViewById(R.id.et_register_password_2);

        String username = usernameInput.getText().toString();
        String password1 = passwordInput1.getText().toString();
        String password2 = passwordInput2.getText().toString();

        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        // Comprobar si las contraseñas coinciden
        if (!password1.equals(password2)) {
            SnackbarUtils.showWarning(
                    findViewById(android.R.id.content), this, getString(R.string.snackbar_warning_password_mismatch)
            );
            passwordInput1.setTextColor(ContextCompat.getColor(RegisterMenu.this, R.color.error));
            passwordInput2.setTextColor(ContextCompat.getColor(RegisterMenu.this, R.color.error));
            return;
        }

        // Simulación de verificación de usuario existente
        if (username.isEmpty() || password1.isEmpty()) {
            SnackbarUtils.showWarning(
                    findViewById(android.R.id.content), this, getString(R.string.snackbar_warning_empty_fields)
            );
            return;
        }

        if (!isValidCredentials(username)) {
            SnackbarUtils.showError(
                    findViewById(android.R.id.content), this, getString(R.string.snackbar_error_username_exists)
            );
            usernameInput.setTextColor(ContextCompat.getColor(RegisterMenu.this, R.color.error));
            return;
        }

        // Simulación de registro exitoso
        SharedPreferences userPrefs = getSharedPreferences("registered_users", MODE_PRIVATE);
        userPrefs.edit().putString(username, password1).apply();

        SnackbarUtils.showSuccess(
                findViewById(android.R.id.content), this, getString(R.string.snackbar_success_register)
        );

        Intent intent = new Intent(RegisterMenu.this, LoginMenu.class);
        intent.putExtra("register_success", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private boolean isValidCredentials(String username) {
        SharedPreferences userPrefs = getSharedPreferences("registered_users", MODE_PRIVATE);
        String storedPassword = userPrefs.getString(username, null);

        if (storedPassword != null) {
            return false;
        } else {
            return true;
        }
    }
}