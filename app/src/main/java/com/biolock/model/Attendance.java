package com.biolock.model;

import java.time.LocalTime;
import java.time.LocalDate;

public class Attendance {
    private int attendanceId;
    private long userId;
    private int sessionId;
    private LocalTime timestamp;
    private String status;

    // Fields from Session and Class
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String moduleCode;
    private String moduleName;
    private String room;
    private String section;

    // Basic attendance fields
    public int getAttendanceId() { return attendanceId; }
    public void setAttendanceId(int attendanceId) { this.attendanceId = attendanceId; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }
    public LocalTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalTime timestamp) { this.timestamp = timestamp; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Additional fields from joins
    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getModuleCode() { return moduleCode; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }
    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
}