package com.example.stay_healthy;

public class User {
    public String email;
    public String profileImageUrl;
    public String signature;
    public String gender;
    public String birthday;

    public User() {
    }

    public User(String email, String profileImageUrl, String signature, String gender, String birthday) {
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.signature = signature;
        this.gender = gender;
        this.birthday = birthday;
    }
}
