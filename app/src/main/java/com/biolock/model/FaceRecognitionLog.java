package com.biolock.model;

import java.util.Date;

public class FaceRecognitionLog {
    private Long logId;
    private Long userId;
    private Date attemptTimestamp;
    private boolean success;
    private float similarity;
    private String deviceInfo;
    private String ipAddress;
    private ActionType actionType;

    public enum ActionType {
        ENROLLMENT,
        LOGIN
    }

    public FaceRecognitionLog() {
        this.attemptTimestamp = new Date();
    }

    // Getters and Setters
    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Date getAttemptTimestamp() {
        return attemptTimestamp;
    }

    public void setAttemptTimestamp(Date attemptTimestamp) {
        this.attemptTimestamp = attemptTimestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public float getSimilarity() {
        return similarity;
    }

    public void setSimilarity(float similarity) {
        this.similarity = similarity;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }
}