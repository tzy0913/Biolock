package com.biolock.repository;

import android.util.Log;
import com.biolock.database.dao.SessionDao;
import com.biolock.model.Session;

public class SessionRepository {
    private static final String TAG = "SessionRepository";
    private final SessionDao sessionDao;

    public SessionRepository() {
        this.sessionDao = new SessionDao();
    }

    public Result<String> startSession(Long sessionId) {
        try {
            Log.d(TAG, "Starting session: " + sessionId);
            return sessionDao.startSession(sessionId);
        } catch (Exception e) {
            Log.e(TAG, "Error starting session", e);
            return Result.error("Failed to start session: " + e.getMessage());
        }
    }

    public Result<Void> endSession(Long sessionId) {
        try {
            Log.d(TAG, "Ending session early: " + sessionId);
            return sessionDao.endSessionEarly(sessionId);
        } catch (Exception e) {
            Log.e(TAG, "Error ending session", e);
            return Result.error("Failed to end session: " + e.getMessage());
        }
    }

    public Result<Boolean> validateCode(Long sessionId, String code) {
        try {
            return sessionDao.validateSessionCode(sessionId, code);
        } catch (Exception e) {
            Log.e(TAG, "Error validating session code", e);
            return Result.error("Failed to validate code: " + e.getMessage());
        }
    }
}