package com.example.carrentaluser.models;

public class UserModel {
    private String email;
    private String address;
    private String age;
    private boolean active;
    private String fcm_token;

    // Default constructor required for Firestore
    public UserModel() {
    }

    public UserModel(String email, String address, String age, boolean active, String fcm_token) {
        this.email = email;
        this.address = address;
        this.age = age;
        this.active = active;
        this.fcm_token = fcm_token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getFcm_token() {
        return fcm_token;
    }

    public void setFcm_token(String fcm_token) {
        this.fcm_token = fcm_token;
    }
}
