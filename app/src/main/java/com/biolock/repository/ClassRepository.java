package com.biolock.repository;

import android.util.Log;
import com.biolock.database.DatabaseHelper;
import com.biolock.database.dao.ClassDao;
import com.biolock.model.Class;
import java.sql.SQLException;

public class ClassRepository {
    private static final String TAG = "ClassRepository";
    private final ClassDao classDao;
    private final DatabaseHelper dbHelper;

    public ClassRepository() {
        this.dbHelper = DatabaseHelper.getInstance();
        this.classDao = new ClassDao();
    }

    private void ensureInitialized() throws SQLException {
        if (!dbHelper.isInitialized()) {
            Log.d(TAG, "Initializing database for class operations");
            dbHelper.initialize();
        }
    }

    public Result<Class> getClassById(int classId) {
        try {
            ensureInitialized();
            Class classData = classDao.getById(classId);
            if (classData != null) {
                return Result.success(classData);
            } else {
                return Result.error(new Exception("Class not found"));
            }
        } catch (SQLException e) {
            Log.e(TAG, "Database error retrieving class", e);
            return Result.error(e);
        }
    }

    public Result<Integer> createClass(Class classData) {
        try {
            ensureInitialized();
            int classId = classDao.insert(classData);
            return Result.success(classId);
        } catch (SQLException e) {
            Log.e(TAG, "Database error creating class", e);
            return Result.error(e);
        }
    }
}