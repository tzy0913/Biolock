/**
 * Model class representing an attendance record.
 * Includes core attendance data, session information, and student details.
 * Used for tracking and displaying class attendance records.
 */
package com.biolock.model;

import java.sql.*;
import java.sql.Time;

public class Attendance {
    /**
     * Status options for attendance:
     * - pending: Initial state when attendance is marked
     * - present: Student attended within time
     * - late: Student attended after start time
     * - absent: Student did not attend
     */

    // ============================
    // Core Attendance Fields
    // ============================
    private Long attendanceId;
    private Long userId;
    private Long sessionId;
    private Time timestamp;
    private String status;  // pending/present/late/absent

    // ============================
    // Session Details
    // ============================
    private Date sessionDate;
    private Time startTime;
    private Time endTime;
    private String validationCode;

    // ============================
    // Course Information
    // ============================
    private String moduleCode;
    private String moduleName;
    private String section;
    private String room;

    // ============================
    // Student Information
    // ============================
    private String studentName;
    private String studentEmail;

    // ============================
    // Constructors
    // ============================

    /**
     * Default constructor
     */
    public Attendance() {}

    /**
     * Constructor for creating a new attendance record
     * @param userId ID of the student
     * @param sessionId ID of the class session
     */
    public Attendance(Long userId, Long sessionId) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.timestamp = new Time(System.currentTimeMillis());
        this.status = "pending";
    }

    // ============================
    // Core Attendance Getters/Setters
    // ============================

    public Long getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(Long attendanceId) {
        this.attendanceId = attendanceId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Time getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Time timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // ============================
    // Session Details Getters/Setters
    // ============================

    public Date getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(Date sessionDate) {
        this.sessionDate = sessionDate;
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

    public String getValidationCode() {
        return validationCode;
    }

    public void setValidationCode(String validationCode) {
        this.validationCode = validationCode;
    }

    // ============================
    // Course Information Getters/Setters
    // ============================

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    // ============================
    // Student Information Getters/Setters
    // ============================

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }
}