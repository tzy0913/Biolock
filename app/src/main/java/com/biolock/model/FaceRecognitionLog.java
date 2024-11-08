/**
 * Model class representing a face recognition attempt log.
 * Records details of face recognition attempts including success/failure,
 * similarity scores, device information, and the type of action attempted.
 */
package com.biolock.model;

import java.util.Date;

public class FaceRecognitionLog {
    // ============================
    // Action Types
    // ============================
    /**
     * Types of face recognition actions that can be performed
     */
    public enum ActionType {
        /** Initial face registration/update */
        ENROLLMENT,
        LOGIN,
        /** Authentication attempt for attendance */
        ATTENDANCE
    }

    // ============================
    // Core Log Information
    // ============================
    private Long logId;              // Unique identifier for the log entry
    private Long userId;             // User who attempted recognition
    private Date attemptTimestamp;   // When the attempt occurred
    private ActionType actionType;   // Type of recognition attempt

    // ============================
    // Recognition Results
    // ============================
    private boolean success;         // Whether the recognition was successful
    private float similarity;        // Similarity score with stored embedding

    // ============================
    // Device Context
    // ============================
    private String deviceInfo;       // Device information (model, OS, etc.)
    private String ipAddress;        // IP address of the device

    // ============================
    // Constructor
    // ============================
    /**
     * Creates a new log entry with current timestamp
     */
    public FaceRecognitionLog() {
        this.attemptTimestamp = new Date();
    }

    // ============================
    // Core Information Getters/Setters
    // ============================

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

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    // ============================
    // Recognition Results Getters/Setters
    // ============================

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

    // ============================
    // Device Context Getters/Setters
    // ============================

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
}