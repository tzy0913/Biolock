package com.biolock.repository;

import android.util.Log;

import com.biolock.database.dao.FaceRecognitionLogDao;
import com.biolock.database.dao.SecuritySettingsDao;
import com.biolock.database.dao.UserDao;
import com.biolock.model.FaceRecognitionLog;
import com.biolock.model.SecuritySettings;
import com.biolock.model.User;

import java.sql.*;
import java.util.Calendar;
import java.util.List;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private final UserDao userDao;
    private final SecuritySettingsDao securitySettingsDao;
    private final FaceRecognitionLogDao logDao;

    public UserRepository() {
        this.userDao = new UserDao();
        this.securitySettingsDao = new SecuritySettingsDao();
        this.logDao = new FaceRecognitionLogDao();
    }

    public Result<User> register(String name, String email, String password) {
        try {
            if (userDao.credentialsExist(email)) {
                return Result.error("Email already registered");
            }

            // Create and save user as student
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPassword(password);
            user.setRole(User.ROLE_STUDENT); // Explicitly set student role

            Long userId = userDao.save(user);
            user.setUserId(userId);

            // Initialize default security settings
            SecuritySettings settings = new SecuritySettings();
            settings.setUserId(userId);
            settings.setMaxFailedAttempts(3);
            settings.setLockoutDurationMins(15);
            securitySettingsDao.save(settings);

            return Result.success(user);
        } catch (SQLException e) {
            Log.e(TAG, "Registration error", e);
            return Result.error("Registration failed: " + e.getMessage());
        }
    }

    public Result<User> login(String email, String password) {
        try {
            User user = userDao.findByEmail(email);
            if (user == null) {
                return Result.error("Invalid email or password");
            }

            // Verify password using MySQL's encryption in DAO
            if (!userDao.verifyPassword(email, password)) {
                // Log failed manual login attempt
                FaceRecognitionLog log = new FaceRecognitionLog();
                log.setUserId(user.getUserId());
                log.setSuccess(false);
                log.setSimilarity(1.0f); // Indicates manual login attempt
                log.setActionType(FaceRecognitionLog.ActionType.LOGIN);
                log.setDeviceInfo(android.os.Build.MODEL);
                log.setIpAddress("127.0.0.1");
                logDao.logAttempt(log);

                return Result.error("Invalid email or password");
            }

            // Log successful manual login
            FaceRecognitionLog log = new FaceRecognitionLog();
            log.setUserId(user.getUserId());
            log.setSuccess(true);
            log.setSimilarity(1.0f); // Indicates manual login
            log.setActionType(FaceRecognitionLog.ActionType.LOGIN);
            log.setDeviceInfo(android.os.Build.MODEL);
            log.setIpAddress("127.0.0.1");
            logDao.logAttempt(log);

            return Result.success(user);
        } catch (SQLException e) {
            Log.e(TAG, "Login error", e);
            return Result.error("Login failed: " + e.getMessage());
        }
    }

    public Result<Boolean> isUserLocked(Long userId) {
        try {
            SecuritySettings settings = securitySettingsDao.getSettings(userId);
            if (settings == null) {
                return Result.success(false);
            }

            // Get recent failed attempts from logs
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, -settings.getLockoutDurationMins());
            java.sql.Date since = new java.sql.Date(cal.getTimeInMillis());

            // Count failed face auth attempts only (not manual logins)
            List<FaceRecognitionLog> recentLogs = logDao.getRecentLogs(userId, settings.getMaxFailedAttempts());
            int failedAttempts = 0;
            long latestFailedTime = 0;

            for (FaceRecognitionLog log : recentLogs) {
                if (!log.isSuccess() && log.getSimilarity() < 1.0f) { // Face login attempt
                    failedAttempts++;
                    if (log.getAttemptTimestamp().getTime() > latestFailedTime) {
                        latestFailedTime = log.getAttemptTimestamp().getTime();
                    }
                } else if (log.isSuccess() && log.getSimilarity() == 1.0f) { // Manual login success
                    // If there's a successful manual login after failed attempts, user is not locked
                    return Result.success(false);
                }
            }

            if (failedAttempts >= settings.getMaxFailedAttempts()) {
                long minutesSinceLastAttempt =
                        (System.currentTimeMillis() - latestFailedTime) / (60 * 1000);
                return Result.success(minutesSinceLastAttempt < settings.getLockoutDurationMins());
            }

            return Result.success(false);
        } catch (SQLException e) {
            Log.e(TAG, "Error checking user lock status", e);
            return Result.error("Failed to check lock status: " + e.getMessage());
        }
    }

    public Result<SecuritySettings> getSecuritySettings(Long userId) {
        try {
            SecuritySettings settings = securitySettingsDao.getSettings(userId);
            if (settings == null) {
                return Result.error("Security settings not found");
            }
            return Result.success(settings);
        } catch (SQLException e) {
            Log.e(TAG, "Error getting security settings", e);
            return Result.error("Failed to get security settings: " + e.getMessage());
        }
    }

    public Result<Boolean> updateSecuritySettings(Long userId, int maxAttempts, int lockoutDuration) {
        try {
            SecuritySettings settings = securitySettingsDao.getSettings(userId);
            if (settings == null) {
                return Result.error("Security settings not found");
            }

            settings.setMaxFailedAttempts(maxAttempts);
            settings.setLockoutDurationMins(lockoutDuration);
            securitySettingsDao.updateSettings(settings);

            return Result.success(true);
        } catch (SQLException e) {
            Log.e(TAG, "Error updating security settings", e);
            return Result.error("Failed to update security settings: " + e.getMessage());
        }
    }

    public Result<User> getUserById(Long userId) {
        try {
            User user = userDao.findById(userId);
            if (user == null) {
                return Result.error("User not found");
            }
            return Result.success(user);
        } catch (SQLException e) {
            Log.e(TAG, "Error getting user", e);
            return Result.error("Failed to get user: " + e.getMessage());
        }
    }
}