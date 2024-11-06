package com.biolock.model;

import java.util.Date;

public class SecuritySettings {
    private Long userId;
    private int maxFailedAttempts;
    private int lockoutDurationMins;
    private int currentFailedAttempts;
    private Date lastFailedAttempt;
    private Date lastUpdated;

    public SecuritySettings() {
        // Default settings
        this.maxFailedAttempts = 3;
        this.lockoutDurationMins = 15;
        this.currentFailedAttempts = 0;
    }

    // Getters and Setters
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

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}