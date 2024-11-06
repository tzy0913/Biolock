package com.biolock.model;

public class ClassAssignment {
    private Long assignmentId;
    private Long classId;
    private Long userId;

    public ClassAssignment() {}

    public ClassAssignment(Long classId, Long userId) {
        this.classId = classId;
        this.userId = userId;
    }

    // Getters and Setters
    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}