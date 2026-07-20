package com.learn.stockpricealert.after;

public class User implements Subscriber {

    private final DeliveryChannel deliveryChannel;
    private String name;
    private String phoneNumber;
    private String email;

    public User(DeliveryChannel deliveryChannel, String name, String phoneNumber, String email) {
        this.deliveryChannel = deliveryChannel;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }


    @Override
    public void notify(String message) {
        this.deliveryChannel.deliver();
        System.out.println("Delivery channel notified: " + message);
    }



    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
