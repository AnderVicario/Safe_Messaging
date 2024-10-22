package com.av19;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        EditText passwordInput = findViewById(R.id.login_password_input);
        EditText usernameInput = findViewById(R.id.login_username_input);
        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

                if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                    usernameInput.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.primary_text_dark));
                    passwordInput.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.primary_text_dark));
                } else {
                    usernameInput.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.primary_text_light));
                    passwordInput.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.primary_text_light));
                }

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
            Intent intent = new Intent(this, MainActivity2.class);
            startActivity(intent);
        }
        else{
            usernameInput.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
            passwordInput.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
        }
    }
}