/**
 * Model class representing a user in the system.
 * Supports two types of users: students and instructors.
 * Contains basic user information and authentication details.
 */
package com.biolock.model;

// Java Utilities
import java.util.Date;

public class User {
    // ============================
    // Role Constants
    // ============================
    /** Role identifier for student users */
    public static final String ROLE_STUDENT = "student";
    /** Role identifier for instructor users */
    public static final String ROLE_INSTRUCTOR = "instructor";

    // ============================
    // Core User Information
    // ============================
    private Long userId;             // Unique identifier for the user
    private String name;             // Full name of the user
    private String email;            // Email address (used for login)

    // ============================
    // Authentication Details
    // ============================
    private String role;             // User's role (student/instructor)
    private String password;         // Encrypted password

    // ============================
    // Metadata
    // ============================
    private Date createdAt;          // Account creation timestamp

    // ============================
    // Constructors
    // ============================

    /**
     * Default constructor
     */
    public User() {}

    /**
     * Constructor for creating a new user
     * @param name User's full name
     * @param email User's email address
     * @param role User's role (should use ROLE_* constants)
     * @param password User's password (will be encrypted)
     */
    public User(String name, String email, String role, String password) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.password = password;
        this.createdAt = new Date();
    }

    // ============================
    // Core Information Getters/Setters
    // ============================

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

    // ============================
    // Authentication Getters/Setters
    // ============================

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

    // ============================
    // Metadata Getters/Setters
    // ============================

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    // ============================
    // Helper Methods
    // ============================

    /**
     * Checks if the user is an instructor
     * @return true if user has instructor role, false otherwise
     */
    public boolean isInstructor() {
        return ROLE_INSTRUCTOR.equals(this.role);
    }
}