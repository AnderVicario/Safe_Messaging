package com.av19.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.av19.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class EditContactDialogFragment extends DialogFragment {

    private EditText etName;
    private ShapeableImageView ivContactIcon;
    private byte[] contactPhoto;
    private int contactId;
    private String initialName;

    // Interfaz para el callback
    public interface EditContactDialogListener {
        void onContactEdited(int contactId, String newName, byte[] contactPhoto);
    }

    private EditContactDialogListener listener;

    // Registrar el ActivityResultLauncher para seleccionar imagen
    private ActivityResultLauncher<Intent> pickImageLauncher;

    // Método de fábrica para crear la instancia con los argumentos necesarios
    public static EditContactDialogFragment newInstance(int contactId, String name, byte[] photo) {
        EditContactDialogFragment fragment = new EditContactDialogFragment();
        Bundle args = new Bundle();
        args.putInt("contact_id", contactId);
        args.putString("contact_name", name);
        args.putByteArray("contact_photo", photo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Se espera que la Activity contenedora implemente el callback
        if (context instanceof EditContactDialogListener) {
            listener = (EditContactDialogListener) context;
        } else {
            throw new RuntimeException(context.toString() + " debe implementar EditContactDialogListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Registra el launcher para la selección de imagen
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                            int width = originalBitmap.getWidth();
                            int height = originalBitmap.getHeight();
                            int squareSize = Math.min(width, height);
                            int x = (width - squareSize) / 2;
                            int y = (height - squareSize) / 2;
                            Bitmap croppedBitmap = Bitmap.createBitmap(originalBitmap, x, y, squareSize, squareSize);
                            int targetSize = 500;
                            Bitmap scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, targetSize, targetSize, true);
                            ivContactIcon.setImageBitmap(scaledBitmap);
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
                            contactPhoto = stream.toByteArray();

                            if(originalBitmap != croppedBitmap) {
                                originalBitmap.recycle();
                            }
                            if(croppedBitmap != scaledBitmap) {
                                croppedBitmap.recycle();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(getActivity(), "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Recupera los argumentos
        if(getArguments() != null) {
            contactId = getArguments().getInt("contact_id", -1);
            initialName = getArguments().getString("contact_name");
            contactPhoto = getArguments().getByteArray("contact_photo");
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.RoundedDialog);
        // Infla el layout personalizado (asegúrate de que edit_contact_form.xml exista en res/layout)
        final android.view.View view = requireActivity().getLayoutInflater().inflate(R.layout.edit_contact_form, null);
        etName = view.findViewById(R.id.et_name);
        ivContactIcon = view.findViewById(R.id.iv_contact_icon);

        etName.setText(initialName);
        if (contactPhoto != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(contactPhoto, 0, contactPhoto.length);
            ivContactIcon.setImageBitmap(bitmap);
        }
        else {
            ivContactIcon.setImageResource(R.drawable.ic_launcher_background);
        }

        // Al pulsar la imagen se abre el selector de imágenes
        ivContactIcon.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(pickIntent);
        });

        builder.setView(view)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dismiss())
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String newName = etName.getText().toString();
                    if (listener != null) {
                        listener.onContactEdited(contactId, newName, contactPhoto);
                    }
                });
        return builder.create();
    }
}
