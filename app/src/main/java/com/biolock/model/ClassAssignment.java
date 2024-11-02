package com.biolock.model;

import java.sql.Timestamp;

public class ClassAssignment {
    private long assignmentId;
    private long classId;
    private long userId;
    private String role;
    private Timestamp createdAt;

    // Getters and setters
    public long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(long assignmentId) { this.assignmentId = assignmentId; }

    public long getClassId() { return classId; }
    public void setClassId(long classId) { this.classId = classId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}