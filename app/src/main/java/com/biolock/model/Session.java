/**
 * Model class representing a class session.
 * Contains information about when a class occurs and its validation status.
 * Each course class can have multiple sessions on different dates.
 */
package com.biolock.model;

// Java SQL
import java.sql.Time;

// Java Utilities
import java.util.Date;

public class Session {
    // ============================
    // Session Identifiers
    // ============================
    private Long sessionId;          // Unique identifier for this session
    private Long classId;            // Reference to the associated course class

    // ============================
    // Session Timing
    // ============================
    private Date date;               // Date of the session
    private Time startTime;          // When the session begins
    private Time endTime;            // When the session ends

    // ============================
    // Session Validation
    // ============================
    private String validationCode;   // Code used to verify attendance

    // ============================
    // Constructor
    // ============================
    /**
     * Default constructor
     */
    public Session() {}

    // ============================
    // Session Identifiers Getters/Setters
    // ============================

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    // ============================
    // Session Timing Getters/Setters
    // ============================

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Time getStartTime() {
        return startTime;
    }

    public void setStartTime(Time startTime) {
        this.startTime = startTime;
    }

    public Time getEndTime() {
        return endTime;
    }

    public void setEndTime(Time endTime) {
        this.endTime = endTime;
    }

    // ============================
    // Session Validation Getters/Setters
    // ============================

    public String getValidationCode() {
        return validationCode;
    }

    public void setValidationCode(String validationCode) {
        this.validationCode = validationCode;
    }
}