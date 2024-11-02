package com.biolock.model;

import java.sql.Timestamp;

public class Attendance {
    private long attendanceId;
    private long userId;
    private long sessionId;
    private Timestamp timestamp;
    private String status;

    // Getters and setters
    public long getAttendanceId() { return attendanceId; }
    public void setAttendanceId(long attendanceId) { this.attendanceId = attendanceId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public long getSessionId() { return sessionId; }
    public void setSessionId(long sessionId) { this.sessionId = sessionId; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}