/**
 * Model class for managing user security settings and lockout policies.
 * Handles configuration for failed login attempts, lockout durations,
 * and tracks the current security status of a user.
 */
package com.biolock.model;

// Java Utilities
import java.util.Date;

public class SecuritySettings {
    // ============================
    // Security Policy Configuration
    // ============================
    private Long userId;                     // User these settings belong to
    private int maxFailedAttempts;          // Maximum allowed failed attempts before lockout
    private int lockoutDurationMins;        // Duration of lockout in minutes

    // ============================
    // Current Security Status
    // ============================
    private int currentFailedAttempts;      // Current count of consecutive failed attempts
    private Date lastFailedAttempt;         // Timestamp of most recent failed attempt

    // ============================
    // Metadata
    // ============================
    private Date lastUpdated;               // Last time settings were modified

    // ============================
    // Constructor
    // ============================
    /**
     * Creates new security settings with default values:
     * - Max failed attempts: 3
     * - Lockout duration: 15 minutes
     * - Current failed attempts: 0
     */
    public SecuritySettings() {
        // Default security policy settings
        this.maxFailedAttempts = 3;
        this.lockoutDurationMins = 15;
        this.currentFailedAttempts = 0;
    }

    // ============================
    // Security Policy Getters/Setters
    // ============================

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public void setMaxFailedAttempts(int maxFailedAttempts) {
        this.maxFailedAttempts = maxFailedAttempts;
    }

    public int getLockoutDurationMins() {
        return lockoutDurationMins;
    }

    public void setLockoutDurationMins(int lockoutDurationMins) {
        this.lockoutDurationMins = lockoutDurationMins;
    }

    // ============================
    // Security Status Getters/Setters
    // ============================

    public int getCurrentFailedAttempts() {
        return currentFailedAttempts;
    }

    public void setCurrentFailedAttempts(int currentFailedAttempts) {
        this.currentFailedAttempts = currentFailedAttempts;
    }

    public Date getLastFailedAttempt() {
        return lastFailedAttempt;
    }

    public void setLastFailedAttempt(Date lastFailedAttempt) {
        this.lastFailedAttempt = lastFailedAttempt;
    }

    // ============================
    // Metadata Getters/Setters
    // ============================

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}