package com.av19;

import java.util.ArrayList;
import java.util.List;

public class ContactList {
    private List<Contact> contacts = new ArrayList<>();

    public ContactList() {
        contacts.add(new Contact("John Doe", "Hello!"));
        contacts.add(new Contact("Jane Doe", "Hi there!"));
        contacts.add(new Contact("Bob Smith", "How are you?"));
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }
}
