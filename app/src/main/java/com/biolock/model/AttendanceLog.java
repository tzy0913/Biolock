package com.biolock.model;

import java.sql.Timestamp;

public class AttendanceLog {
    private long logId;
    private long userId;
    private Timestamp timestamp;
    private String action;
    private long attendanceId;

    // Getters and setters
    public long getLogId() { return logId; }
    public void setLogId(long logId) { this.logId = logId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public long getAttendanceId() { return attendanceId; }
    public void setAttendanceId(long attendanceId) { this.attendanceId = attendanceId; }
}