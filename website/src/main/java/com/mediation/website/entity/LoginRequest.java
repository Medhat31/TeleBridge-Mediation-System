package com.mediation.website.entity;

import java.io.Serializable;

/**
 * A Data Transfer Object (DTO) representing an incoming login request containing user credentials.
 */
public class LoginRequest implements Serializable {
    private String email;
    private String password;

    public LoginRequest() {}

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
