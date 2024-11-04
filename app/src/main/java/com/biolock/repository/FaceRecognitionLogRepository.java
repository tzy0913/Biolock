package com.biolock.repository;

import android.util.Log;
import com.biolock.database.DatabaseHelper;
import com.biolock.database.dao.FaceRecognitionLogDao;
import com.biolock.model.FaceRecognitionLog;
import java.sql.SQLException;
import java.util.List;

public class FaceRecognitionLogRepository {
    private static final String TAG = "FaceRecognitionLogsRepo";
    private final DatabaseHelper dbHelper;
    private final FaceRecognitionLogDao faceRecognitionLogDao;

    public FaceRecognitionLogRepository() {
        this.dbHelper = DatabaseHelper.getInstance();
        this.faceRecognitionLogDao = new FaceRecognitionLogDao();
    }

    private void ensureInitialized() throws SQLException {
        if (!dbHelper.isInitialized()) {
            Log.d(TAG, "Initializing database for face recognition logs");
            dbHelper.initialize();
        }
    }

    public Result<Long> logAttempt(FaceRecognitionLog log) {
        try {
            ensureInitialized();
            Log.d(TAG, "Logging face recognition attempt for user: " + log.getUserId());

            long logId = faceRecognitionLogDao.insert(log);
            Log.d(TAG, "Successfully logged attempt with ID: " + logId);

            return Result.success(logId);
        } catch (SQLException e) {
            Log.e(TAG, "Database error while logging attempt", e);
            return Result.error(e);
        }
    }

    public Result<List<FaceRecognitionLog>> getUserLogs(long userId) {
        try {
            ensureInitialized();
            Log.d(TAG, "Retrieving face recognition logs for user: " + userId);

            List<FaceRecognitionLog> logs = faceRecognitionLogDao.findByUserId(userId);
            return Result.success(logs);
        } catch (SQLException e) {
            Log.e(TAG, "Database error while retrieving logs", e);
            return Result.error(e);
        }
    }

    public Result<Boolean> checkSuspiciousActivity(long userId, String ipAddress) {
        try {
            ensureInitialized();
            Log.d(TAG, "Checking suspicious activity for user: " + userId);

            int recentFailures = faceRecognitionLogDao.countRecentFailedAttempts(userId, ipAddress, 30);
            boolean suspicious = recentFailures >= 5;

            if (suspicious) {
                Log.w(TAG, "Detected suspicious activity for user: " + userId);
            }

            return Result.success(suspicious);
        } catch (SQLException e) {
            Log.e(TAG, "Error checking suspicious activity", e);
            return Result.error(e);
        }
    }
}