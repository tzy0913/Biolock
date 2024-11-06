package com.biolock.model;

import java.util.Date;

public class User {
    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_INSTRUCTOR = "instructor";
    private Long userId;
    private String name;
    private String email;
    private String role;
    private String password;
    private Date createdAt;

    public User() {}

    public User(String name, String email, String role, String password) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.password = password;
        this.createdAt = new Date();
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isInstructor() {
        return ROLE_INSTRUCTOR.equals(this.role);
    }
}