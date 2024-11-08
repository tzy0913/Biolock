/**
 * Model class representing a course class.
 * Contains information about the course, session details, and class status.
 * Includes static tracking of the current active class.
 */
package com.biolock.model;

import java.sql.Date;
import java.sql.Time;

public class CourseClass {
    // ============================
    // Static Class Management
    // ============================
    private static CourseClass currentClass;

    /**
     * Gets the currently active class
     * @return Currently active CourseClass instance
     */
    public static CourseClass getCurrentClass() {
        return currentClass;
    }

    /**
     * Sets the currently active class
     * @param courseClass CourseClass to set as current
     */
    public static void setCurrentClass(CourseClass courseClass) {
        currentClass = courseClass;
    }

    // ============================
    // Course Information
    // ============================
    private Long classId;
    private String moduleCode;
    private String moduleName;
    private String section;
    private String type;  // lecture/tutorial
    private String room;
    private Long instructorId;

    // ============================
    // Session Information
    // ============================
    private Long sessionId;
    private Date sessionDate;
    private Time startTime;
    private Time endTime;
    private String status;
    private String validationCode;

    // ============================
    // Constructor
    // ============================
    public CourseClass() {}

    // ============================
    // Course Information Getters/Setters
    // ============================

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public Long getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Long instructorId) {
        this.instructorId = instructorId;
    }

    // ============================
    // Session Information Getters/Setters
    // ============================

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getValidationCode() {
        return validationCode;
    }

    public void setValidationCode(String validationCode) {
        this.validationCode = validationCode;
    }
}