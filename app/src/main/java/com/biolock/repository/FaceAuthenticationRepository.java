/**
 * Repository handling facial authentication operations.
 * Manages face enrollment, authentication, and security assessments.
 * Integrates with face recognition services and maintains authentication logs.
 */
package com.biolock.repository;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import com.biolock.database.dao.*;
import com.biolock.model.*;
import com.biolock.utils.*;
import java.io.IOException;
import java.sql.*;
import java.util.Calendar;
import java.util.List;

public class FaceAuthenticationRepository {
    private static final String TAG = "FaceAuthenticationRepo";

    // DAOs for database operations
    private final FaceEmbeddingDao faceEmbeddingDao;
    private final FaceRecognitionLogDao logDao;
    private final SecuritySettingsDao securitySettingsDao;

    // Face recognition utilities
    private final FaceRecognition faceRecognition;

    /**
     * Authentication purposes to differentiate between login and attendance
     */
    public enum AuthPurpose {
        LOGIN,
        ATTENDANCE
    }

    // ============================
    // Constructor
    // ============================

    /**
     * Initializes the repository with necessary dependencies
     * @param context Application context for face recognition initialization
     * @throws RuntimeException if face recognition initialization fails
     */
    public FaceAuthenticationRepository(Context context) {
        try {
            this.faceEmbeddingDao = new FaceEmbeddingDao();
            this.logDao = new FaceRecognitionLogDao();
            this.securitySettingsDao = new SecuritySettingsDao();
            this.faceRecognition = new FaceRecognition(context);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize face recognition", e);
        }
    }

    // ============================
    // Face Embedding Operations
    // ============================

    /**
     * Retrieves stored face embedding for a user
     * @param userId ID of the user
     * @return Result containing face embedding or error
     */
    public Result<FaceEmbedding> getFaceEmbedding(long userId) {
        try {
            byte[] embeddingData = faceEmbeddingDao.getEmbedding(userId);
            if (embeddingData == null) {
                return Result.error("No face embedding found");
            }

            FaceEmbedding embedding = new FaceEmbedding();
            embedding.setUserId(userId);
            embedding.setEmbeddingData(embeddingData);
            return Result.success(embedding);
        } catch (SQLException e) {
            Log.e(TAG, "Error getting face embedding", e);
            return Result.error("Failed to get face embedding: " + e.getMessage());
        }
    }

    /**
     * Checks if a user has enrolled their face
     * @param userId ID of the user to check
     * @return Result containing enrollment status
     */
    public Result<Boolean> hasFaceEnrolled(long userId) {
        try {
            return Result.success(faceEmbeddingDao.hasEnrollment(userId));
        } catch (SQLException e) {
            Log.e(TAG, "Error checking face enrollment", e);
            return Result.error("Failed to check face enrollment: " + e.getMessage());
        }
    }

    // ============================
    // Enrollment Operations
    // ============================

    /**
     * Enrolls or updates a user's face
     * @param userId ID of the user
     * @param faceBitmap Bitmap image of the face
     * @param confidenceScore Confidence score of the face detection
     * @return Result indicating success or failure
     */
    public Result<Boolean> enrollFace(Long userId, Bitmap faceBitmap, double confidenceScore) {
        try {
            float[] embedding = faceRecognition.generateEmbedding(faceBitmap);
            if (embedding == null) {
                return Result.error("Failed to generate face embedding");
            }

            byte[] embeddingData = faceRecognition.embeddingToBytes(embedding);

            if (faceEmbeddingDao.hasEnrollment(userId)) {
                faceEmbeddingDao.update(userId, embeddingData, confidenceScore);
            } else {
                faceEmbeddingDao.save(userId, embeddingData, confidenceScore);
            }

            // Log successful enrollment
            logFaceRecognitionAttempt(userId, true, 1.0f,
                    FaceRecognitionLog.ActionType.ENROLLMENT);

            return Result.success(true);
        } catch (SQLException e) {
            Log.e(TAG, "Error during face enrollment", e);
            return Result.error("Face enrollment failed: " + e.getMessage());
        }
    }

    /**
     * Deletes a user's face enrollment
     * @param userId ID of the user
     * @return Result indicating success or failure
     */
    public Result<Boolean> deleteFace(long userId) {
        try {
            faceEmbeddingDao.delete(userId);
            return Result.success(true);
        } catch (SQLException e) {
            Log.e(TAG, "Error deleting face embedding", e);
            return Result.error("Failed to delete face enrollment: " + e.getMessage());
        }
    }

    // ============================
    // Authentication Operations
    // ============================

    /**
     * Authenticates a user using facial recognition
     * @param userId ID of the user to authenticate
     * @param faceBitmap Captured face image
     * @return Result indicating authentication success or failure
     */
    public Result<Boolean> authenticate(Long userId, Bitmap faceBitmap, AuthPurpose purpose) {
        try {
            // Get stored embedding
            byte[] storedEmbeddingBytes = faceEmbeddingDao.getEmbedding(userId);
            if (storedEmbeddingBytes == null) {
                return Result.error("No face enrollment found");
            }

            // Generate new embedding from captured face
            float[] newEmbedding = faceRecognition.generateEmbedding(faceBitmap);
            if (newEmbedding == null) {
                return Result.error("Failed to process face image");
            }

            float[] storedEmbedding = faceRecognition.bytesToEmbedding(storedEmbeddingBytes);
            boolean authenticated = faceRecognition.matchFace(storedEmbedding, newEmbedding);
            float similarity = faceRecognition.getLastSimilarityScore();

            // Map AuthPurpose to ActionType for logging
            FaceRecognitionLog.ActionType actionType = (purpose == AuthPurpose.LOGIN)
                    ? FaceRecognitionLog.ActionType.LOGIN
                    : FaceRecognitionLog.ActionType.ATTENDANCE;

            // Log the authentication attempt
            logFaceRecognitionAttempt(userId, authenticated, similarity, actionType);

            return Result.success(authenticated);
        } catch (SQLException e) {
            Log.e(TAG, "Error during face authentication", e);
            return Result.error("Authentication failed: " + e.getMessage());
        }
    }

    // ============================
    // Security Assessment
    // ============================

    /**
     * Runs a security assessment for a user
     * @param userId ID of the user
     * @return Result containing security metrics
     */
    public Result<SecurityAssessment.SecurityMetrics> runSecurityAssessment(Long userId) {
        try {
            // Get the logs for the last 30 days
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -30);
            java.sql.Date startDate = new java.sql.Date(cal.getTimeInMillis());
            java.sql.Date endDate = new java.sql.Date(System.currentTimeMillis());

            List<FaceRecognitionLog> logs = logDao.getLogsByDateRange(userId, startDate, endDate);
            boolean hasEnrollment = faceEmbeddingDao.hasEnrollment(userId);

            SecurityAssessment.SecurityMetrics metrics =
                    SecurityAssessment.analyzeSecurityMetrics(logs, hasEnrollment);

            return Result.success(metrics);
        } catch (Exception e) {
            Log.e(TAG, "Error running security assessment", e);
            return Result.error("Failed to run security assessment: " + e.getMessage());
        }
    }

    // ============================
    // Helper Methods
    // ============================

    /**
     * Logs a face recognition attempt
     */
    private void logFaceRecognitionAttempt(Long userId, boolean success, float similarity,
                                           FaceRecognitionLog.ActionType actionType)
            throws SQLException {
        FaceRecognitionLog log = new FaceRecognitionLog();
        log.setUserId(userId);
        log.setSuccess(success);
        log.setSimilarity(similarity);
        log.setActionType(actionType);
        log.setDeviceInfo(android.os.Build.MODEL);
        log.setIpAddress("127.0.0.1");
        logDao.logAttempt(log);
    }
}