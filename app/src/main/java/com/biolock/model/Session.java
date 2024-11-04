package com.biolock.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Session {
    private int sessionId;
    private int classId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    // Getters and setters
    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }

    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
}