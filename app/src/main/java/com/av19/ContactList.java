package com.av19;

import java.util.ArrayList;
import java.util.List;

public class ContactList {
    private static ContactList myContactList;
    private static List<Contact> contacts = new ArrayList<>();

    private ContactList() {
        this.addContact(new Contact("John Doe", "Hello!", "22m"));
        this.addContact(new Contact("Ivy White", "Have a great day!", "7d"));
        this.addContact(new Contact("Jack Black", "Can't wait to see you!", "33d"));
        this.addContact(new Contact("Karen Green", "Long time no see!", "64d"));
        this.addContact(new Contact("Emily Davis", "Let's catch up soon.", "22h"));
        this.addContact(new Contact("Frank Miller", "Are you free today?", "1d"));
        this.addContact(new Contact("Bob Smith", "How are you?", "54m"));
        this.addContact(new Contact("Matogo", "eres un calvo", "1h"));
        this.addContact(new Contact("Alice Johnson", "Nice to meet you!", "2h"));
        this.addContact(new Contact("Charlie Brown", "What's up?", "4h"));
        this.addContact(new Contact("David Clark", "Good morning!", "5h"));
        this.addContact(new Contact("Grace Kelly", "I'll call you later.", "3d"));
        this.addContact(new Contact("Henry Ford", "See you tomorrow!", "5d"));
        this.addContact(new Contact("Lucas Grey", "How's everything going?", "123d"));
        this.addContact(new Contact("Mary Blue", "I have good news!", "1y"));
        this.addContact(new Contact("Jane Doe", "Hi there!", "30m"));

    }

    public static ContactList getInstance() {
        if (myContactList == null) {
            myContactList = new ContactList();
        }
        return myContactList;
    }

    public int addContact(Contact contact) {
        int contactLastMessageTimeInMinutes = contact.getLastMessageTimeInMinutes();
        if (contactLastMessageTimeInMinutes == 0) {
            contacts.add(contact);
            return contacts.size() - 1;
        }
        else{
            int index = 0;
            for (Contact c : contacts) {
                if (contact.getLastMessageTimeInMinutes() < c.getLastMessageTimeInMinutes()) {
                    break; // Encuentra el punto de inserción
                }
                index++;
            }
            contacts.add(index, contact); // Inserta en la posición correcta
            return index;
        }
    }

    public List<Contact> getContacts() {
        return contacts;
    }
}
