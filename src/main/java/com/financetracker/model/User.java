package com.financetracker.model;

import java.time.LocalDateTime;

/**
 * Entity model representing a registered application user.
 */
public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String salt;
    private String createdAt;

    public User() {
    }

    public User(int id, String username, String passwordHash, String salt, String createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.createdAt = createdAt;
    }

    public User(String username, String passwordHash, String salt) {
        this(0, username, passwordHash, salt, LocalDateTime.now().toString());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return username;
    }
}
