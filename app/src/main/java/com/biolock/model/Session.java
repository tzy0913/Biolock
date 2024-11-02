package com.biolock.model;

import java.sql.Date;
import java.sql.Time;

public class Session {
    private long sessionId;
    private long classId;
    private Date date;
    private Time startTime;
    private Time endTime;

    // Getters and setters
    public long getSessionId() { return sessionId; }
    public void setSessionId(long sessionId) { this.sessionId = sessionId; }

    public long getClassId() { return classId; }
    public void setClassId(long classId) { this.classId = classId; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public Time getStartTime() { return startTime; }
    public void setStartTime(Time startTime) { this.startTime = startTime; }

    public Time getEndTime() { return endTime; }
    public void setEndTime(Time endTime) { this.endTime = endTime; }
}