package com.biolock.model;

public class AttendanceStats {
    private int totalSessions;
    private int present;
    private int late;
    private int absent;
    private float presentRate;
    private float lateRate;
    private float absentRate;

    public AttendanceStats() {
        // Default constructor for empty stats
    }

    // Getters and Setters
    public int getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(int totalSessions) {
        this.totalSessions = totalSessions;
    }

    public int getPresent() {
        return present;
    }

    public void setPresent(int present) {
        this.present = present;
    }

    public int getLate() {
        return late;
    }

    public void setLate(int late) {
        this.late = late;
    }

    public int getAbsent() {
        return absent;
    }

    public void setAbsent(int absent) {
        this.absent = absent;
    }

    public float getPresentRate() {
        return presentRate;
    }

    public void setPresentRate(float presentRate) {
        this.presentRate = presentRate;
    }

    public float getLateRate() {
        return lateRate;
    }

    public void setLateRate(float lateRate) {
        this.lateRate = lateRate;
    }

    public float getAbsentRate() {
        return absentRate;
    }

    public void setAbsentRate(float absentRate) {
        this.absentRate = absentRate;
    }
}
