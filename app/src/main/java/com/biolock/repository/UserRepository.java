package com.biolock.repository;

import android.util.Log;
import com.biolock.database.DatabaseHelper;
import com.biolock.database.dao.UserDao;
import com.biolock.model.FaceEmbedding;
import com.biolock.model.SecuritySettings;
import com.biolock.model.User;

import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.List;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private final DatabaseHelper dbHelper;
    private final UserDao userDao;
    private final SecuritySettingsRepository securitySettingsRepository;
    private final FaceEmbeddingRepository faceEmbeddingRepository;

    public UserRepository() {
        this.dbHelper = DatabaseHelper.getInstance();
        this.userDao = new UserDao();
        this.securitySettingsRepository = new SecuritySettingsRepository();
        this.faceEmbeddingRepository = new FaceEmbeddingRepository();
    }

    private void ensureInitialized() throws SQLException {
        if (!dbHelper.isInitialized()) {
            Log.d(TAG, "Initializing database for user operations");
            dbHelper.initialize();
        }
    }

    public Result<User> login(String email, String password) {
        try {
            ensureInitialized();
            Log.d(TAG, "Attempting login for email: " + email);

            User user = userDao.validateCredentials(email, password);
            if (user != null) {
                Log.d(TAG, "Login successful for user: " + user.getUserId());
                return Result.success(user);
            } else {
                Log.w(TAG, "Invalid credentials for email: " + email);
                return Result.error(new Exception("Invalid credentials"));
            }
        } catch (SQLException e) {
            Log.e(TAG, "Database error during login", e);
            return Result.error(e);
        }
    }

    public Result<Long> register(User user, String password) {
        try {
            ensureInitialized();
            Log.d(TAG, "Attempting to register user with email: " + user.getEmail());

            // Check for existing user
            User existingUser = userDao.findByEmail(user.getEmail());
            if (existingUser != null) {
                Log.w(TAG, "Email already registered: " + user.getEmail());
                return Result.error(new Exception("Email already registered"));
            }

            // Insert new user
            long userId = userDao.insert(user, password);
            Log.d(TAG, "Successfully registered user with ID: " + userId);

            // Initialize security settings
            Result<SecuritySettings> settingsResult = securitySettingsRepository.getSettings(userId);
            if (!settingsResult.isSuccess()) {
                Log.e(TAG, "Failed to initialize security settings for user: " + userId);
                return Result.error(new Exception("Failed to initialize security settings"));
            }

            return Result.success(userId);
        } catch (SQLException e) {
            Log.e(TAG, "Database error during registration", e);
            return Result.error(e);
        }
    }

    public Result<User> getUserById(long userId) {
        try {
            ensureInitialized();
            Log.d(TAG, "Retrieving user with ID: " + userId);

            User user = userDao.findById(userId);
            if (user != null) {
                return Result.success(user);
            } else {
                Log.w(TAG, "User not found with ID: " + userId);
                return Result.error(new Exception("User not found"));
            }
        } catch (SQLException e) {
            Log.e(TAG, "Database error retrieving user", e);
            return Result.error(e);
        }
    }

    public Result<List<User>> getAllUsers() {
        try {
            ensureInitialized();
            Log.d(TAG, "Retrieving all users");

            List<User> users = userDao.findAll();
            return Result.success(users);
        } catch (SQLException e) {
            Log.e(TAG, "Database error retrieving all users", e);
            return Result.error(e);
        }
    }

    public Result<List<User>> getUsersByRole(String role) {
        try {
            ensureInitialized();
            Log.d(TAG, "Retrieving users with role: " + role);

            List<User> users = userDao.findByRole(role);
            return Result.success(users);
        } catch (SQLException e) {
            Log.e(TAG, "Database error retrieving users by role", e);
            return Result.error(e);
        }
    }

    public Result<Void> updateUser(User user) {
        try {
            ensureInitialized();
            Log.d(TAG, "Updating user with ID: " + user.getUserId());

            userDao.update(user);
            return Result.success(null);
        } catch (SQLException e) {
            Log.e(TAG, "Database error updating user", e);
            return Result.error(e);
        }
    }

    public Result<Void> deleteUser(long userId) {
        try {
            ensureInitialized();
            Log.d(TAG, "Deleting user with ID: " + userId);

            userDao.delete(userId);
            return Result.success(null);
        } catch (SQLException e) {
            Log.e(TAG, "Database error deleting user", e);
            return Result.error(e);
        }
    }

    public Result<User> findByEmail(String email) {
        try {
            ensureInitialized();
            Log.d(TAG, "Looking up user by email: " + email);

            User user = userDao.findByEmail(email);
            if (user != null) {
                return Result.success(user);
            } else {
                Log.w(TAG, "No user found with email: " + email);
                return Result.error(new Exception("User not found"));
            }
        } catch (SQLException e) {
            Log.e(TAG, "Database error finding user by email", e);
            return Result.error(e);
        }
    }
}