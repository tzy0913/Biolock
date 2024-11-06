package com.biolock.repository;

import android.util.Log;

import com.biolock.database.dao.FaceRecognitionLogDao;
import com.biolock.model.FaceRecognitionLog;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class FaceRecognitionLogRepository {
    private static final String TAG = "FaceRecognitionLogRepo";
    private final FaceRecognitionLogDao logDao;

    public FaceRecognitionLogRepository() {
        this.logDao = new FaceRecognitionLogDao();
    }

    public Result<Void> logAttempt(FaceRecognitionLog log) {
        try {
            logDao.logAttempt(log);
            return Result.success(null);
        } catch (SQLException e) {
            Log.e(TAG, "Error logging attempt", e);
            return Result.error("Failed to log attempt: " + e.getMessage());
        }
    }

    public Result<List<FaceRecognitionLog>> getUserLogs(Long userId) {
        try {
            // Get logs for last 30 days by default
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -30);
            java.sql.Date startDate = new java.sql.Date(cal.getTimeInMillis());
            java.sql.Date endDate = new java.sql.Date(System.currentTimeMillis());

            List<FaceRecognitionLog> logs = logDao.getLogsByDateRange(userId, startDate, endDate);
            return Result.success(logs);
        } catch (SQLException e) {
            Log.e(TAG, "Error getting user logs", e);
            return Result.error("Failed to get logs: " + e.getMessage());
        }
    }

    public Result<Boolean> hasRecentFailedAttempts(Long userId, int minutes) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, -minutes);
            java.sql.Date since = new java.sql.Date(cal.getTimeInMillis());

            int failedAttempts = logDao.getFailedAttempts(userId, since);
            return Result.success(failedAttempts > 0);
        } catch (SQLException e) {
            Log.e(TAG, "Error checking failed attempts", e);
            return Result.error("Failed to check attempts: " + e.getMessage());
        }
    }

    public Result<List<FaceRecognitionLog>> getRecentLogs(Long userId, int limit) {
        try {
            List<FaceRecognitionLog> logs = logDao.getRecentLogs(userId, limit);
            return Result.success(logs);
        } catch (SQLException e) {
            Log.e(TAG, "Error getting recent logs", e);
            return Result.error("Failed to get recent logs: " + e.getMessage());
        }
    }

    public Result<List<FaceRecognitionLog>> getLogsByDateRange(Long userId, Date start, Date end) {
        try {
            java.sql.Date sqlStart = new java.sql.Date(start.getTime());
            java.sql.Date sqlEnd = new java.sql.Date(end.getTime());

            List<FaceRecognitionLog> logs = logDao.getLogsByDateRange(userId, sqlStart, sqlEnd);
            return Result.success(logs);
        } catch (SQLException e) {
            Log.e(TAG, "Error getting logs by date range", e);
            return Result.error("Failed to get logs: " + e.getMessage());
        }
    }
}