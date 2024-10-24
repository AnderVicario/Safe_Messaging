package com.av19;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AddContactForm extends AppCompatActivity {

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
    }

    public void goToListContacts(View view) {
        finish();
    }

    public void addContact(View view) {
        TextView et_name = findViewById(R.id.et_name);
        String name = et_name.getText().toString();

        // Añadir el contacto
        Intent resultIntent = new Intent();
        resultIntent.putExtra("new_contact_name", name);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();  // Termina la actividad y regresa el resultado
    }
}
