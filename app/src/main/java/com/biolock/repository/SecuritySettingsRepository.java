package com.biolock.repository;

import com.biolock.database.dao.SecuritySettingsDao;
import com.biolock.model.SecuritySettings;
import java.sql.SQLException;

public class SecuritySettingsRepository {
    private final SecuritySettingsDao securitySettingsDao;

    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    public static final int DEFAULT_LOCKOUT_DURATION = 5; // minutes
    public static final int MAX_ALLOWED_ATTEMPTS = 5;
    public static final int MAX_LOCKOUT_DURATION = 15; // minutes
    public static final int LOCKOUT_DURATION_INTERVAL = 5; // minutes

    public SecuritySettingsRepository() {
        this.securitySettingsDao = new SecuritySettingsDao();
    }

    public Result<SecuritySettings> initializeSettings(long userId) {
        SecuritySettings settings = new SecuritySettings();
        settings.setUserId(userId);
        settings.setMaxFailedAttempts(DEFAULT_MAX_ATTEMPTS);
        settings.setLockoutDurationMins(DEFAULT_LOCKOUT_DURATION);

        try {
            securitySettingsDao.insertOrUpdate(settings);
            return Result.success(settings);
        } catch (SQLException e) {
            return Result.error(e);
        }
    }

    public Result<SecuritySettings> updateSettings(SecuritySettings settings) {
        // Validate settings
        if (settings.getMaxFailedAttempts() > MAX_ALLOWED_ATTEMPTS ||
                settings.getMaxFailedAttempts() < 1) {
            return Result.error(new IllegalArgumentException("Invalid max attempts value"));
        }

        if (settings.getLockoutDurationMins() > MAX_LOCKOUT_DURATION ||
                settings.getLockoutDurationMins() % LOCKOUT_DURATION_INTERVAL != 0) {
            return Result.error(new IllegalArgumentException("Invalid lockout duration"));
        }

        try {
            securitySettingsDao.insertOrUpdate(settings);
            return Result.success(settings);
        } catch (SQLException e) {
            return Result.error(e);
        }
    }

    public Result<SecuritySettings> getSettings(long userId) {
        try {
            SecuritySettings settings = securitySettingsDao.findByUserId(userId);
            if (settings == null) {
                // Settings don't exist, initialize with defaults
                return initializeSettings(userId);
            }
            return Result.success(settings);
        } catch (SQLException e) {
            return Result.error(e);
        }
    }
}