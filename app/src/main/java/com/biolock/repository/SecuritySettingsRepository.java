package com.biolock.repository;

import android.util.Log;
import com.biolock.database.DatabaseHelper;
import com.biolock.database.dao.SecuritySettingsDao;
import com.biolock.model.SecuritySettings;
import java.sql.SQLException;
import java.sql.Timestamp;

public class SecuritySettingsRepository {
    private static final String TAG = "SecuritySettingsRepo";
    private final DatabaseHelper dbHelper;
    private final SecuritySettingsDao securitySettingsDao;

    // Constants for validation
    public static final int MAX_ALLOWED_ATTEMPTS = 5;
    public static final int MAX_LOCKOUT_DURATION = 30;
    public static final int LOCKOUT_DURATION_INTERVAL = 5;
    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    public static final int DEFAULT_LOCKOUT_DURATION = 15;

    public SecuritySettingsRepository() {
        this.dbHelper = DatabaseHelper.getInstance();
        this.securitySettingsDao = new SecuritySettingsDao();
    }

    private void ensureInitialized() throws SQLException {
        if (!dbHelper.isInitialized()) {
            Log.d(TAG, "Initializing database for security settings operations");
            dbHelper.initialize();
        }
    }

    public Result<SecuritySettings> getSettings(long userId) {
        try {
            ensureInitialized();
            Log.d(TAG, "Retrieving security settings for user: " + userId);

            SecuritySettings settings = securitySettingsDao.findByUserId(userId);
            if (settings == null) {
                Log.d(TAG, "No settings found, creating defaults for user: " + userId);
                settings = createDefaultSettings(userId);
                securitySettingsDao.insertOrUpdate(settings);
            }
            return Result.success(settings);
        } catch (SQLException e) {
            Log.e(TAG, "Error retrieving security settings", e);
            return Result.error(e);
        }
    }

    public Result<SecuritySettings> updateSettings(SecuritySettings settings) {
        try {
            ensureInitialized();
            Log.d(TAG, "Updating security settings for user: " + settings.getUserId());

            if (!validateSettings(settings)) {
                String error = "Invalid settings: maxAttempts must be between 1 and " +
                        MAX_ALLOWED_ATTEMPTS + ", lockoutDuration must be between " +
                        LOCKOUT_DURATION_INTERVAL + " and " + MAX_LOCKOUT_DURATION;
                Log.e(TAG, error);
                return Result.error(new IllegalArgumentException(error));
            }

            securitySettingsDao.insertOrUpdate(settings);
            Log.d(TAG, "Successfully updated security settings");
            return Result.success(settings);
        } catch (SQLException e) {
            Log.e(TAG, "Error updating security settings", e);
            return Result.error(e);
        }
    }

    public Result<Boolean> isUserLocked(long userId) {
        try {
            ensureInitialized();
            SecuritySettings settings = securitySettingsDao.findByUserId(userId);
            if (settings == null || settings.getLastFailedAttempt() == null) {
                return Result.success(false);
            }

            long lockoutMillis = settings.getLockoutDurationMins() * 60 * 1000L;
            long lastFailedMillis = settings.getLastFailedAttempt().getTime();
            long currentMillis = System.currentTimeMillis();

            boolean isLocked = settings.getFailedAttemptsCount() >= settings.getMaxFailedAttempts() &&
                    (currentMillis - lastFailedMillis) < lockoutMillis;

            return Result.success(isLocked);
        } catch (SQLException e) {
            Log.e(TAG, "Error checking user lock status", e);
            return Result.error(e);
        }
    }

    public Result<SecuritySettings> incrementFailedAttempts(long userId) {
        try {
            ensureInitialized();
            securitySettingsDao.updateFailedAttempt(userId);
            return getSettings(userId);
        } catch (SQLException e) {
            Log.e(TAG, "Error incrementing failed attempts", e);
            return Result.error(e);
        }
    }

    public Result<SecuritySettings> resetFailedAttempts(long userId) {
        try {
            ensureInitialized();
            securitySettingsDao.resetFailedAttempts(userId);
            return getSettings(userId);
        } catch (SQLException e) {
            Log.e(TAG, "Error resetting failed attempts", e);
            return Result.error(e);
        }
    }

    private SecuritySettings createDefaultSettings(long userId) {
        SecuritySettings settings = new SecuritySettings();
        settings.setUserId(userId);
        settings.setMaxFailedAttempts(DEFAULT_MAX_ATTEMPTS);
        settings.setLockoutDurationMins(DEFAULT_LOCKOUT_DURATION);
        settings.setFailedAttemptsCount(0);
        settings.setLastFailedAttempt(null);
        return settings;
    }

    private boolean validateSettings(SecuritySettings settings) {
        return settings != null &&
                settings.getUserId() > 0 &&
                settings.getMaxFailedAttempts() > 0 &&
                settings.getMaxFailedAttempts() <= MAX_ALLOWED_ATTEMPTS &&
                settings.getLockoutDurationMins() >= LOCKOUT_DURATION_INTERVAL &&
                settings.getLockoutDurationMins() <= MAX_LOCKOUT_DURATION;
    }
}