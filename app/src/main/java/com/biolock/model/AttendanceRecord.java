package com.biolock.model;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AttendanceRecord {
    private Attendance attendance;
    private Session session;
    private CourseClass classInfo;

    public AttendanceRecord() {}

    // Getters and Setters
    public Attendance getAttendance() {
        return attendance;
    }

    public void setAttendance(Attendance attendance) {
        this.attendance = attendance;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public CourseClass getClassInfo() {
        return classInfo;
    }

    public void setClassInfo(CourseClass classInfo) {
        this.classInfo = classInfo;
    }

    // Convenience methods for UI
    public String getFormattedDateTime() {
        if (session != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return dateFormat.format(session.getDate()) + " " +
                    timeFormat.format(session.getStartTime()) + " - " +
                    timeFormat.format(session.getEndTime());
        }
        return "";
    }

    public String getClassDetails() {
        if (classInfo != null) {
            return classInfo.getModuleCode() + " - " +
                    classInfo.getModuleName() + " (" +
                    classInfo.getSection() + ")";
        }
        return "";
    }

    public String getStatusForDisplay() {
        if (attendance != null) {
            String status = attendance.getStatus();
            return status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
        }
        return "Not Marked";
    }
}
