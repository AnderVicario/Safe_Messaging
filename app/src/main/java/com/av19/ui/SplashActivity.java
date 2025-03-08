package com.av19.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseLocaleActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Cargar el tema guardado desde SharedPreferences
        SharedPreferences settingsPrefs = getSharedPreferences("settings", MODE_PRIVATE);
        int themeMode = settingsPrefs.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);

        // Cargar al sesión guardada desde SharedPreferences
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String token = prefs.getString("auth_token", null);

        Intent intent;
        if (token != null) {
            intent = new Intent(this, ListContacts.class);
        } else {
            intent = new Intent(this, LoginMenu.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}