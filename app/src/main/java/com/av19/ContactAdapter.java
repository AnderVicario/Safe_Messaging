package com.av19;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ContactAdapter extends BaseAdapter {

    Activity myActivity;
    ContactList contactList;

    public ContactAdapter(Activity myActivity, ContactList contactList) {
        this.myActivity = myActivity;
        this.contactList = contactList;
    }

    @Override
    public int getCount() {
        return contactList.getContacts().size();
    }

    @Override
    public Contact getItem(int i) {
        return contactList.getContacts().get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup parent) {

        View onePersonLine;

        LayoutInflater inflater = (LayoutInflater) myActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        onePersonLine = inflater.inflate(R.layout.person_one_line, parent, false);

        TextView tv_name = onePersonLine.findViewById(R.id.tv_name);
        TextView tv_last_message = onePersonLine.findViewById(R.id.tv_last_message);
        ImageView iv_icon = onePersonLine.findViewById(R.id.iv_icon);

        Contact contact = this.getItem(i);

        tv_name.setText(contact.getName());
        tv_last_message.setText(contact.getLastMessage());
        iv_icon.setImageResource(R.drawable.icon);

        return onePersonLine;
    }
}
