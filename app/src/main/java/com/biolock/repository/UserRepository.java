/**
 * Repository for managing user accounts and related security operations.
 * Handles user authentication, registration, and security settings management.
 * Integrates user data with security and logging functionality.
 */
package com.biolock.repository;

// Android Core
import android.util.Log;

// Biolock Components
import com.biolock.database.dao.*;
import com.biolock.model.*;

// Java Utilities
import java.sql.*;
import java.util.List;

public class UserRepository {
    private static final String TAG = "UserRepository";

    // Data Access Objects
    private final UserDao userDao;
    private final SecuritySettingsDao securitySettingsDao;
    private final FaceRecognitionLogDao logDao;

    // ============================
    // Constructor
    // ============================

    /**
     * Initializes the repository with necessary DAOs
     */
    public UserRepository() {
        this.userDao = new UserDao();
        this.securitySettingsDao = new SecuritySettingsDao();
        this.logDao = new FaceRecognitionLogDao();
    }

    // ============================
    // Authentication Operations
    // ============================

    /**
     * Authenticates a user with email and password
     * Logs both successful and failed attempts
     *
     * @param email User's email address
     * @param password User's password
     * @return Result containing authenticated user or error
     */
    public Result<User> login(String email, String password) {
        try {
            User user = userDao.findByEmail(email);
            if (user == null) {
                return Result.error("Invalid email or password");
            }

            boolean authenticated = userDao.verifyPassword(email, password);
            // Create log entry
            logLoginAttempt(user.getUserId(), authenticated);

            if (!authenticated) {
                return Result.error("Invalid email or password");
            }

            return Result.success(user);
        } catch (SQLException e) {
            Log.e(TAG, "Login error", e);
            return Result.error("Login failed: " + e.getMessage());
        }
    }

    /**
     * Registers a new user account with default security settings
     *
     * @param name User's full name
     * @param email User's email address
     * @param password User's password
     * @return Result containing created user or error
     */
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
            user.setRole(User.ROLE_STUDENT);

            Long userId = userDao.save(user);
            user.setUserId(userId);

            // Initialize default security settings
            initializeSecuritySettings(userId);

            return Result.success(user);
        } catch (SQLException e) {
            Log.e(TAG, "Registration error", e);
            return Result.error("Registration failed: " + e.getMessage());
        }
    }

    // ============================
    // Security Operations
    // ============================

    /**
     * Checks if a user's account is temporarily locked due to failed attempts
     *
     * @param userId ID of the user to check
     * @return Result containing lock status
     */
    public Result<Boolean> isUserLocked(Long userId) {
        try {
            SecuritySettings settings = securitySettingsDao.getSettings(userId);
            if (settings == null) {
                return Result.success(false);
            }

            // Get recent failed attempts from logs
            List<FaceRecognitionLog> recentLogs = logDao.getRecentLogs(userId, settings.getMaxFailedAttempts());
            int failedAttempts = 0;
            long latestFailedTime = 0;

            for (FaceRecognitionLog log : recentLogs) {
                // Only count failed LOGIN attempts, ignore ATTENDANCE attempts
                if (!log.isSuccess() && log.getActionType() == FaceRecognitionLog.ActionType.LOGIN) {
                    if (log.getSimilarity() < 1.0f) { // Face login attempt
                        failedAttempts++;
                        latestFailedTime = Math.max(latestFailedTime, log.getAttemptTimestamp().getTime());
                    }
                } else if (log.isSuccess() && log.getSimilarity() == 1.0f) { // Manual login success
                    return Result.success(false); // Successful manual login resets lockout
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

    /**
     * Retrieves security settings for a user
     */
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

    /**
     * Updates security settings for a user
     */
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

    // ============================
    // User Information Operations
    // ============================

    /**
     * Retrieves user information by ID
     */
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

    // ============================
    // Helper Methods
    // ============================

    /**
     * Initializes default security settings for a new user
     */
    private void initializeSecuritySettings(Long userId) throws SQLException {
        SecuritySettings settings = new SecuritySettings();
        settings.setUserId(userId);
        settings.setMaxFailedAttempts(3);
        settings.setLockoutDurationMins(15);
        securitySettingsDao.save(settings);
    }

    /**
     * Logs a login attempt
     */
    private void logLoginAttempt(Long userId, boolean success) throws SQLException {
        FaceRecognitionLog log = new FaceRecognitionLog();
        log.setUserId(userId);
        log.setSuccess(success);
        log.setSimilarity(1.0f); // Indicates manual login
        log.setActionType(FaceRecognitionLog.ActionType.LOGIN); // Keep as LOGIN for manual attempts
        log.setDeviceInfo(android.os.Build.MODEL);
        log.setIpAddress("127.0.0.1");
        logDao.logAttempt(log);
    }
}