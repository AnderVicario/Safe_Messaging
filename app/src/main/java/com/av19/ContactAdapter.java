package com.av19;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    private final ContactList contactList;
    private final Context context;

    public ContactAdapter(ContactList contactList, Context context) {
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

    // Remove static modifier from ViewHolder class
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
            Log.d("MiTag", itemView.toString());

            cardView.setOnClickListener(view -> {
                Log.d("MiTag", "Has clicado en un contacto");
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Contact contact = contactList.getContacts().get(position);

                    // Launch Conversation with contact details
                    Intent intent = new Intent(context, Conversation.class);
                    intent.putExtra("contact_id", contact.getId());
                    intent.putExtra("contact_name", contact.getName());
                    context.startActivity(intent);
                }
            });
        }

        public void bind(Contact contact) {
            tv_name.setText(contact.getName());
            tv_last_message.setText(contact.getLastMessage());
            tv_last_message_time.setText(contact.getLastMessageTime());
            iv_icon.setImageResource(R.drawable.ic_launcher_background);
        }
    }
}
