package com.biolock.model;

import java.sql.Timestamp;

public class FaceRecognitionLog {
    private long logId;
    private long userId;
    private Timestamp attemptTimestamp;
    private boolean success;
    private double score;
    private String deviceInfo;
    private String ipAddress;

    // Getters and setters
    public long getLogId() { return logId; }
    public void setLogId(long logId) { this.logId = logId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public Timestamp getAttemptTimestamp() { return attemptTimestamp; }
    public void setAttemptTimestamp(Timestamp attemptTimestamp) { this.attemptTimestamp = attemptTimestamp; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}