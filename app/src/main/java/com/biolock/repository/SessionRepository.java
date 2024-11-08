/**
 * Repository for managing class session operations.
 * Handles session lifecycle including starting, ending, and validation.
 * Acts as a higher-level abstraction over SessionDao.
 */
package com.biolock.repository;

import android.util.Log;
import com.biolock.database.dao.SessionDao;
import com.biolock.model.Session;

public class SessionRepository {
    private static final String TAG = "SessionRepository";
    private final SessionDao sessionDao;

    // ============================
    // Constructor
    // ============================

    /**
     * Initializes the repository with SessionDao
     */
    public SessionRepository() {
        this.sessionDao = new SessionDao();
    }

    // ============================
    // Session Lifecycle Operations
    // ============================

    /**
     * Initiates a new class session
     * Generates and returns a validation code for attendance
     *
     * @param sessionId ID of the session to start
     * @return Result containing the validation code or error message
     */
    public Result<String> startSession(Long sessionId) {
        try {
            Log.d(TAG, "Starting session: " + sessionId);
            return sessionDao.startSession(sessionId);
        } catch (Exception e) {
            Log.e(TAG, "Error starting session", e);
            return Result.error("Failed to start session: " + e.getMessage());
        }
    }

    /**
     * Ends an ongoing session before its scheduled end time
     *
     * @param sessionId ID of the session to end
     * @return Result indicating success or failure
     */
    public Result<Void> endSession(Long sessionId) {
        try {
            Log.d(TAG, "Ending session early: " + sessionId);
            return sessionDao.endSessionEarly(sessionId);
        } catch (Exception e) {
            Log.e(TAG, "Error ending session", e);
            return Result.error("Failed to end session: " + e.getMessage());
        }
    }

    // ============================
    // Session Validation Operations
    // ============================

    /**
     * Validates a session code for attendance marking
     *
     * @param sessionId ID of the session
     * @param code Validation code to verify
     * @return Result containing boolean indicating if code is valid
     */
    public Result<Boolean> validateCode(Long sessionId, String code) {
        try {
            return sessionDao.validateSessionCode(sessionId, code);
        } catch (Exception e) {
            Log.e(TAG, "Error validating session code", e);
            return Result.error("Failed to validate code: " + e.getMessage());
        }
    }
}