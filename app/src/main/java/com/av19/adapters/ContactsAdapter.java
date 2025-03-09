package com.av19.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.av19.R;
import com.av19.models.Contact;
import com.av19.models.ContactList;
import com.av19.ui.Conversation;
import com.av19.ui.EditContactDialogFragment;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {

    private final ContactList contactList;
    private final Context context;

    public ContactsAdapter(ContactList contactList, Context context) {
        this.contactList = contactList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_contacts_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contact contact = contactList.getContacts().get(position);
        holder.bind(contact);
    }

    @Override
    public int getItemCount() {
        return contactList.getContacts().size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tv_name;
        private final TextView tv_last_message;
        private final TextView tv_last_message_time;
        private final ImageView iv_icon;
        private final CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_name = itemView.findViewById(R.id.tv_name);
            tv_last_message = itemView.findViewById(R.id.tv_last_message);
            tv_last_message_time = itemView.findViewById(R.id.tv_last_message_time);
            iv_icon = itemView.findViewById(R.id.iv_icon);
            cardView = itemView.findViewById(R.id.card_view);

            // Click corto: abre la conversación
            cardView.setOnClickListener(view -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Contact contact = contactList.getContacts().get(position);
                    Intent intent = new Intent(context, Conversation.class);
                    intent.putExtra("contact_id", String.valueOf(contact.getId()));
                    intent.putExtra("contact_name", contact.getName());
                    intent.putExtra("contact_photo", contact.getPhoto());
                    context.startActivity(intent);
                }
            });

            // Long click: abre el DialogFragment de edición
            cardView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Contact contact = contactList.getContacts().get(position);
                    EditContactDialogFragment dialogFragment = EditContactDialogFragment.newInstance(
                            contact.getId(),
                            contact.getName(),
                            contact.getPhoto()
                    );
                    // Se muestra el diálogo usando el FragmentManager del Activity contenedor
                    dialogFragment.show(((AppCompatActivity) context).getSupportFragmentManager(), "EditContactDialog");
                }
                return true;
            });
        }

        public void bind(Contact contact) {
            tv_name.setText(contact.getName());
            tv_last_message.setText(contact.getLastMessagePreview());
            tv_last_message_time.setText(contact.getFormattedLastMessageTime());
            if (contact.getPhoto() != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(contact.getPhoto(), 0, contact.getPhoto().length);
                iv_icon.setImageBitmap(bitmap);
            } else {
                iv_icon.setImageResource(R.drawable.ic_launcher_background);
            }
        }
    }
}
