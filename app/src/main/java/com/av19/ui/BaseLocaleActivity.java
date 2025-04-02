package com.av19.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class BaseLocaleActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        // Cargar idioma
        SharedPreferences prefs = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String langCode = prefs.getString("lang", "es");
        Locale newLocale = new Locale(langCode);
        Locale.setDefault(newLocale);

        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        config.setLocale(newLocale);
        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }
}