package com.learn.stockpricealert.before;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String name;
    private String email;
    private String phoneNumber;
    private List<Subscription> subscriptions;


    public User(String name, String email, String phoneNumber) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.subscriptions = new ArrayList<>();
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void addSubscription(Subscription subscription) {
        this.subscriptions.add(subscription);
    }

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }
}
