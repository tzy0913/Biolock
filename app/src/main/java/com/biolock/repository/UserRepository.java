package com.biolock.repository;

import com.biolock.database.dao.UserDao;
import com.biolock.model.User;
import java.sql.SQLException;

public class UserRepository {
    private final UserDao userDao;

    public UserRepository() {
        this.userDao = new UserDao();
    }

    public Result<User> login(String email, String password) {
        try {
            User user = userDao.validateCredentials(email, password);
            if (user != null) {
                return Result.success(user);
            } else {
                return Result.error(new Exception("Invalid credentials"));
            }
        } catch (SQLException e) {
            return Result.error(e);
        }
    }

    public Result<Long> register(User user, String password) {
        try {
            if (userDao.findByEmail(user.getEmail()) != null) {
                return Result.error(new Exception("Email already registered"));
            }

            long userId = userDao.insert(user, password);

            // Initialize security settings
            SecuritySettingsRepository securityRepo = new SecuritySettingsRepository();
            securityRepo.initializeSettings(userId);

            return Result.success(userId);
        } catch (SQLException e) {
            return Result.error(e);
        }
    }

    public Result<User> getUserById(long userId) {
        try {
            User user = userDao.findById(userId);
            if (user != null) {
                return Result.success(user);
            } else {
                return Result.error(new Exception("User not found"));
            }
        } catch (SQLException e) {
            return Result.error(e);
        }
    }
}