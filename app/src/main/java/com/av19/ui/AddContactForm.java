package com.av19.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.av19.R;
import com.av19.models.api.ProfilePictureResponse;
import com.av19.models.api.PublicKeyResponse;
import com.av19.utils.ApiService;
import com.av19.utils.RetrofitClient;
import com.av19.utils.SnackbarUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddContactForm extends BaseLocaleActivity {

    private ApiService apiService;
    private String currentUser;
    private byte[] contactPhoto = null;
    private ImageView iv_contact_icon;
    private Handler handler = new Handler();
    private Runnable fetchPhotoRunnable;

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

        iv_contact_icon = findViewById(R.id.iv_contact_icon);
        iv_contact_icon.setImageResource(R.drawable.ic_launcher_background);

        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        currentUser = getSharedPreferences("session", MODE_PRIVATE)
                .getString("auth_token", null);

        TextView et_name = findViewById(R.id.et_name);
        et_name.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(fetchPhotoRunnable);
            }
            @Override public void afterTextChanged(Editable s) {
                String username = s.toString().trim();
                if (username.isEmpty()) return;
                fetchPhotoRunnable = () -> fetchProfilePicture(username);
                handler.postDelayed(fetchPhotoRunnable, 2000);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (contactPhoto != null) {
            outState.putByteArray("contactPhoto", contactPhoto);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        contactPhoto = savedInstanceState.getByteArray("contactPhoto");
        if (contactPhoto != null) {
            Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(contactPhoto, 0, contactPhoto.length);
            iv_contact_icon.setImageBitmap(bitmap);
        }
    }

    private void fetchProfilePicture(String username) {
        apiService.getProfilePicture(username).enqueue(new Callback<ProfilePictureResponse>() {
            @Override
            public void onResponse(Call<ProfilePictureResponse> call, Response<ProfilePictureResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String base64 = response.body().getProfile_picture();
                    if (base64 != null && !base64.isEmpty()) {
                        try {
                            byte[] photoBytes = java.util.Base64.getDecoder().decode(base64);
                            contactPhoto = photoBytes;
                            Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);
                            iv_contact_icon.setImageBitmap(bitmap);
                        } catch (IllegalArgumentException e) {
                            Log.e("AddContactForm", "Error decodificando imagen", e);
                        }
                    }
                    else {
                        iv_contact_icon.setImageResource(R.drawable.ic_launcher_background);
                    }
                }
            }
            @Override
            public void onFailure(Call<ProfilePictureResponse> call, Throwable t) {
                Log.e("AddContactForm", "Fallo al obtener imagen", t);
            }
        });
    }

    public void goToListContacts(View view) {
        finish();
    }

    public void addContact(View view) {
        TextView et_name = findViewById(R.id.et_name);
        String username = et_name.getText().toString().trim();

        if (username.isEmpty()) {
            SnackbarUtils.showWarning(
                    findViewById(android.R.id.content),
                    this,
                    getString(R.string.snackbar_warning_empty_user)
            );
        }

        apiService.getPublicKey(username).enqueue(new Callback<PublicKeyResponse>() {
            @Override
            public void onResponse(Call<PublicKeyResponse> call, Response<PublicKeyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String publicKey = response.body().getPublic_key();
                    if (!username.equals(currentUser)){
                        returnResult(username, publicKey);
                    }
                    else {
                        SnackbarUtils.showWarning(
                                findViewById(android.R.id.content),
                                AddContactForm.this,
                                getString(R.string.snackbar_error_username_same)
                        );
                    }
                } else {
                    SnackbarUtils.showError(
                            findViewById(android.R.id.content),
                            AddContactForm.this,
                            getString(R.string.snackbar_error_username_dontexist)
                    );
                }
            }

            @Override
            public void onFailure(Call<PublicKeyResponse> call, Throwable t) {
                Log.e("AddContactForm", "Fallo al recibir el usuario", t);
            }
        });
    }

    private void returnResult(String username, String publicKey) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("new_contact_name", username);
        resultIntent.putExtra("new_public_key", publicKey);
        resultIntent.putExtra("new_contact_photo", contactPhoto);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}
