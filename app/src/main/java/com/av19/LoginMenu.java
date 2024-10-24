package com.av19;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginMenu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        EditText passwordInput = findViewById(R.id.login_password_input);
        EditText usernameInput = findViewById(R.id.login_username_input);
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
    }

    public void goToSecondActivity(View view) {
        EditText usernameInput = findViewById(R.id.login_username_input);
        EditText passwordInput = findViewById(R.id.login_password_input);

        String username = usernameInput.getText().toString();
        String password = passwordInput.getText().toString();

        if (username.equals(password)){
            Intent intent = new Intent(this, ListContacts.class);
            startActivity(intent);
        }
        else{
            usernameInput.setTextColor(ContextCompat.getColor(this, R.color.error));
            passwordInput.setTextColor(ContextCompat.getColor(this, R.color.error));
        }
    }
}