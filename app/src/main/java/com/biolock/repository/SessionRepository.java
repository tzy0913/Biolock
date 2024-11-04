package com.biolock.repository;

import android.util.Log;
import com.biolock.database.DatabaseHelper;
import com.biolock.database.dao.SessionDao;
import com.biolock.model.Session;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class SessionRepository {
    private static final String TAG = "SessionRepository";
    private final SessionDao sessionDao;
    private final DatabaseHelper dbHelper;

    public SessionRepository() {
        this.dbHelper = DatabaseHelper.getInstance();
        this.sessionDao = new SessionDao();
    }

    private void ensureInitialized() throws SQLException {
        if (!dbHelper.isInitialized()) {
            Log.d(TAG, "Initializing database for session operations");
            dbHelper.initialize();
        }
    }

    public Result<Session> getSessionById(int sessionId) {
        try {
            ensureInitialized();
            Session session = sessionDao.getById(sessionId);
            if (session != null) {
                return Result.success(session);
            } else {
                return Result.error(new Exception("Session not found"));
            }
        } catch (SQLException e) {
            Log.e(TAG, "Database error retrieving session", e);
            return Result.error(e);
        }
    }

    public Result<List<Session>> getSessionsByDateRange(LocalDate startDate, LocalDate endDate) {
        try {
            ensureInitialized();
            Log.d(TAG, "Getting sessions from " + startDate + " to " + endDate);

            List<Session> sessions = sessionDao.getByDateRange(startDate, endDate);
            return Result.success(sessions);
        } catch (SQLException e) {
            Log.e(TAG, "Database error getting sessions by date range", e);
            return Result.error(e);
        }
    }

    public Result<List<Session>> getSessionsByDate(LocalDate date) {
        return getSessionsByDateRange(date, date);
    }

    public Result<Integer> createSession(Session session) {
        try {
            ensureInitialized();
            int sessionId = sessionDao.insert(session);
            return Result.success(sessionId);
        } catch (SQLException e) {
            Log.e(TAG, "Database error creating session", e);
            return Result.error(e);
        }
    }
}