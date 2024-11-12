/**
 * Repository class handling attendance-related business logic.
 * Provides methods for retrieving and managing attendance records across different time periods.
 * Supports both instructor and student views.
 */
package com.biolock.repository;

// Android Core
import android.util.Log;

// Biolock Database
import com.biolock.database.dao.AttendanceDao;
import com.biolock.database.dao.ClassDao;

// Biolock Models
import com.biolock.model.Attendance;
import com.biolock.model.CourseClass;

// Java Date & Time
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class AttendanceRepository {
    private static final String TAG = "AttendanceRepository";
    private final AttendanceDao attendanceDao;
    private final ClassDao classDao;

    // ============================
    // Constructor
    // ============================

    public AttendanceRepository() {
        this.attendanceDao = new AttendanceDao();
        this.classDao = new ClassDao();
    }

    // ============================
    // Session-based Operations
    // ============================

    /**
     * Retrieves attendance records for a specific session
     * @param sessionId ID of the session
     * @return Result containing list of attendance records or error
     */
    public Result<List<Attendance>> getCurrentSessionAttendance(Long sessionId) {
        try {
            List<Attendance> attendances = attendanceDao.getSessionAttendance(sessionId);
            return Result.success(attendances);
        } catch (Exception e) {
            Log.e(TAG, "Error getting session attendance", e);
            return Result.error("Failed to get session attendance: " + e.getMessage());
        }
    }

    // ============================
    // Time-based View Operations
    // ============================

    /**
     * Retrieves attendance/class records for the current day
     * @param id User ID (student or instructor)
     * @param isInstructor true if user is instructor, false if student
     * @return Result containing list of relevant records
     */
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

    /**
     * Retrieves attendance/class records for a specified week
     * @param id User ID
     * @param isInstructor User role flag
     * @param startDate Week start date
     * @param endDate Week end date
     * @return Result containing list of relevant records
     */
    public Result<List<?>> getWeekAttendance(Long id, boolean isInstructor,
                                             LocalDate startDate, LocalDate endDate) {
        try {
            Date sqlStartDate = toSqlDate(startDate);
            Date sqlEndDate = toSqlDate(endDate);

            Log.d(TAG, String.format("Getting attendance for week - ID: %d, IsInstructor: %b",
                    id, isInstructor));
            Log.d(TAG, String.format("Date range: %s to %s", sqlStartDate, sqlEndDate));

            if (isInstructor) {
                List<CourseClass> classes = classDao.findClassesByInstructor(id, sqlStartDate, sqlEndDate);
                Log.d(TAG, String.format("Found %d classes for instructor", classes.size()));
                return Result.success(classes);
            } else {
                List<Attendance> attendances = classDao.findClassesByStudent(id, sqlStartDate, sqlEndDate);
                Log.d(TAG, String.format("Found %d attendances for student", attendances.size()));
                return Result.success(attendances);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting week attendance", e);
            return Result.error("Failed to get week attendance: " + e.getMessage());
        }
    }

    /**
     * Retrieves attendance/class records for the current month
     * @param id User ID
     * @param isInstructor User role flag
     * @return Result containing list of relevant records
     */
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

    /**
     * Retrieves attendance/class records for a specific date
     * @param id User ID
     * @param isInstructor User role flag
     * @param selectedDate Date to retrieve records for
     * @return Result containing list of relevant records
     */
    public Result<List<?>> getDateAttendance(Long id, boolean isInstructor, LocalDate selectedDate) {
        try {
            Date sqlDate = toSqlDate(selectedDate);

            if (isInstructor) {
                List<CourseClass> classes = classDao.findClassesByInstructor(id, sqlDate, sqlDate);
                return Result.success(classes);
            } else {
                List<Attendance> attendances = classDao.findClassesByStudentForDate(id, sqlDate);
                return Result.success(attendances);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting date attendance", e);
            return Result.error("Failed to get date attendance: " + e.getMessage());
        }
    }

    // ============================
    // Attendance Management
    // ============================

    /**
     * Records attendance for a user in a session
     * @param userId ID of the user marking attendance
     * @param sessionId ID of the session
     * @return Result indicating success or failure
     */
    public Result<Void> markAttendance(Long userId, Long sessionId) {
        try {
            attendanceDao.markAttendance(userId, sessionId);
            return Result.success(null);
        } catch (Exception e) {
            Log.e(TAG, "Error marking attendance", e);
            return Result.error("Failed to mark attendance: " + e.getMessage());
        }
    }

    // ============================
    // Helper Methods
    // ============================

    /**
     * Converts LocalDate to SQL Date
     * @param date LocalDate to convert
     * @return Equivalent SQL Date
     */
    private Date toSqlDate(LocalDate date) {
        return new Date(date.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli());
    }
}