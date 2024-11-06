package com.biolock.repository;

import android.util.Log;
import com.biolock.database.dao.AttendanceDao;
import com.biolock.database.dao.ClassDao;
import com.biolock.model.Attendance;
import com.biolock.model.CourseClass;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

public class AttendanceRepository {
    private static final String TAG = "AttendanceRepository";
    private final AttendanceDao attendanceDao;
    private final ClassDao classDao;

    public AttendanceRepository() {
        this.attendanceDao = new AttendanceDao();
        this.classDao = new ClassDao();
    }

    // Helper method to convert LocalDate to SQL Date
    private Date toSqlDate(LocalDate date) {
        return new Date(date.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli());
    }

    public Result<List<Attendance>> getCurrentSessionAttendance(Long sessionId) {
        try {
            List<Attendance> attendances = attendanceDao.getSessionAttendance(sessionId);
            return Result.success(attendances);
        } catch (Exception e) {
            Log.e(TAG, "Error getting session attendance", e);
            return Result.error("Failed to get session attendance: " + e.getMessage());
        }
    }

    // For Today tab - Using date range with same start/end date
    public Result<List<?>> getTodayAttendance(Long id, boolean isInstructor) {
        try {
            LocalDate today = LocalDate.now();
            Date sqlToday = toSqlDate(today);

            if (isInstructor) {
                List<CourseClass> classes = classDao.findClassesByInstructor(id, sqlToday, sqlToday);
                return Result.success(classes);
            } else {
                List<Attendance> attendances = classDao.findClassesByStudent(id, sqlToday, sqlToday);
                return Result.success(attendances);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting today's attendance", e);
            return Result.error("Failed to get today's attendance: " + e.getMessage());
        }
    }

    // For Week tab
    public Result<List<?>> getWeekAttendance(Long id, boolean isInstructor) {
        try {
            LocalDate now = LocalDate.now();
            LocalDate monday = now.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            LocalDate sunday = monday.plusDays(6);

            Date startDate = toSqlDate(monday);
            Date endDate = toSqlDate(sunday);

            if (isInstructor) {
                List<CourseClass> classes = classDao.findClassesByInstructor(id, startDate, endDate);
                return Result.success(classes);
            } else {
                List<Attendance> attendances = classDao.findClassesByStudent(id, startDate, endDate);
                return Result.success(attendances);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting week attendance", e);
            return Result.error("Failed to get week attendance: " + e.getMessage());
        }
    }

    // For Month tab
    public Result<List<?>> getMonthAttendance(Long id, boolean isInstructor) {
        try {
            LocalDate now = LocalDate.now();
            LocalDate firstDay = now.withDayOfMonth(1);
            LocalDate lastDay = now.withDayOfMonth(now.lengthOfMonth());

            Date startDate = toSqlDate(firstDay);
            Date endDate = toSqlDate(lastDay);

            if (isInstructor) {
                List<CourseClass> classes = classDao.findClassesByInstructor(id, startDate, endDate);
                return Result.success(classes);
            } else {
                List<Attendance> attendances = classDao.findClassesByStudent(id, startDate, endDate);
                return Result.success(attendances);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting month attendance", e);
            return Result.error("Failed to get month attendance: " + e.getMessage());
        }
    }

    // For specific date (used in month view when selecting a date)
    public Result<List<?>> getDateAttendance(Long id, boolean isInstructor, LocalDate selectedDate) {
        try {
            Date sqlDate = toSqlDate(selectedDate);

            if (isInstructor) {
                // For instructor, pass same date as start and end
                List<CourseClass> classes = classDao.findClassesByInstructor(id, sqlDate, sqlDate);
                return Result.success(classes);
            } else {
                // For student, use the single date method
                List<Attendance> attendances = classDao.findClassesByStudentForDate(id, sqlDate);
                return Result.success(attendances);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting date attendance", e);
            return Result.error("Failed to get date attendance: " + e.getMessage());
        }
    }

    // For marking attendance
    public Result<Void> markAttendance(Long userId, Long sessionId) {
        try {
            attendanceDao.markAttendance(userId, sessionId);
            return Result.success(null);
        } catch (Exception e) {
            Log.e(TAG, "Error marking attendance", e);
            return Result.error("Failed to mark attendance: " + e.getMessage());
        }
    }
}