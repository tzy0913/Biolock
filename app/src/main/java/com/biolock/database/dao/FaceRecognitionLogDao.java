package com.biolock.database.dao;

import com.biolock.database.DatabaseHelper;
import com.biolock.model.FaceRecognitionLog;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FaceRecognitionLogDao {
    private final DatabaseHelper dbHelper;

    public FaceRecognitionLogDao() {
        this.dbHelper = DatabaseHelper.getInstance();
    }

    public long insert(FaceRecognitionLog log) throws SQLException {
        String sql = "INSERT INTO face_recognition_logs (user_id, success, score, device_info, ip_address) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, log.getUserId());
            pstmt.setBoolean(2, log.isSuccess());
            pstmt.setDouble(3, log.getScore());
            pstmt.setString(4, log.getDeviceInfo());
            pstmt.setString(5, log.getIpAddress());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating face recognition log failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating face recognition log failed, no ID obtained.");
                }
            }
        }
    }

    public List<FaceRecognitionLog> findByUserId(long userId) throws SQLException {
        String sql = "SELECT * FROM face_recognition_logs WHERE user_id = ? ORDER BY attempt_timestamp DESC";
        List<FaceRecognitionLog> logs = new ArrayList<>();

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToFaceRecognitionLog(rs));
                }
            }
        }
        return logs;
    }

    private FaceRecognitionLog mapResultSetToFaceRecognitionLog(ResultSet rs) throws SQLException {
        FaceRecognitionLog log = new FaceRecognitionLog();
        log.setLogId(rs.getLong("log_id"));
        log.setUserId(rs.getLong("user_id"));
        log.setAttemptTimestamp(rs.getTimestamp("attempt_timestamp"));
        log.setSuccess(rs.getBoolean("success"));
        log.setScore(rs.getDouble("score"));
        log.setDeviceInfo(rs.getString("device_info"));
        log.setIpAddress(rs.getString("ip_address"));
        return log;
    }
}