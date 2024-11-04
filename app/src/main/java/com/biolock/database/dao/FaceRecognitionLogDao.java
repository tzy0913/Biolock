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
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "INSERT INTO face_recognition_logs (user_id, success, similarity, device_info, " +
                    "ip_address, action_type) VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setLong(1, log.getUserId());
                pstmt.setBoolean(2, log.isSuccess());
                pstmt.setFloat(3, log.getSimilarity());
                pstmt.setString(4, log.getDeviceInfo());
                pstmt.setString(5, log.getIpAddress());
                pstmt.setString(6, log.getActionType().name());

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
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    public List<FaceRecognitionLog> findByUserId(long userId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "SELECT * FROM face_recognition_logs WHERE user_id = ? ORDER BY attempt_timestamp DESC";
            List<FaceRecognitionLog> logs = new ArrayList<>();

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, userId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        logs.add(mapResultSetToFaceRecognitionLog(rs));
                    }
                }
            }
            return logs;
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    public int countRecentFailedAttempts(long userId, String ipAddress, int minutes) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "SELECT COUNT(*) FROM face_recognition_logs " +
                    "WHERE user_id = ? AND ip_address = ? AND success = 0 AND action_type = 'LOGIN' " +
                    "AND attempt_timestamp >= DATE_SUB(NOW(), INTERVAL ? MINUTE)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, userId);
                pstmt.setString(2, ipAddress);
                pstmt.setInt(3, minutes);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                    return 0;
                }
            }
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    private FaceRecognitionLog mapResultSetToFaceRecognitionLog(ResultSet rs) throws SQLException {
        FaceRecognitionLog log = new FaceRecognitionLog();
        log.setLogId(rs.getLong("log_id"));
        log.setUserId(rs.getLong("user_id"));
        log.setAttemptTimestamp(rs.getTimestamp("attempt_timestamp"));
        log.setSuccess(rs.getBoolean("success"));
        log.setSimilarity(rs.getFloat("similarity"));
        log.setDeviceInfo(rs.getString("device_info"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setActionType(FaceRecognitionLog.ActionType.valueOf(rs.getString("action_type")));
        return log;
    }
}