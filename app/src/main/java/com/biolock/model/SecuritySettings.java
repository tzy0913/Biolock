package com.biolock.model;

import java.sql.Timestamp;

public class SecuritySettings {
    private long userId;
    private int maxFailedAttempts;
    private int lockoutDurationMins;
    private Timestamp lastFailedAttempt;
    private int failedAttemptsCount;

    // Getters and setters
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public int getMaxFailedAttempts() { return maxFailedAttempts; }
    public void setMaxFailedAttempts(int maxFailedAttempts) { this.maxFailedAttempts = maxFailedAttempts; }

    public int getLockoutDurationMins() { return lockoutDurationMins; }
    public void setLockoutDurationMins(int lockoutDurationMins) { this.lockoutDurationMins = lockoutDurationMins; }

    public Timestamp getLastFailedAttempt() { return lastFailedAttempt; }
    public void setLastFailedAttempt(Timestamp lastFailedAttempt) { this.lastFailedAttempt = lastFailedAttempt; }

    public int getFailedAttemptsCount() { return failedAttemptsCount; }
    public void setFailedAttemptsCount(int failedAttemptsCount) { this.failedAttemptsCount = failedAttemptsCount; }
}