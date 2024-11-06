package com.biolock.repository;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.biolock.database.dao.FaceEmbeddingDao;
import com.biolock.database.dao.FaceRecognitionLogDao;
import com.biolock.database.dao.SecuritySettingsDao;
import com.biolock.model.FaceEmbedding;
import com.biolock.model.FaceRecognitionLog;
import com.biolock.model.SecuritySettings;
import com.biolock.utils.FaceAuthenticator;
import com.biolock.utils.FaceRecognition;
import com.biolock.utils.SecurityAssessment;

import java.io.IOException;
import java.sql.*;
import java.util.Calendar;
import java.util.List;

public class FaceAuthenticationRepository {
    private static final String TAG = "FaceAuthenticationRepo";
    private final FaceEmbeddingDao faceEmbeddingDao;
    private final FaceRecognitionLogDao logDao;
    private final SecuritySettingsDao securitySettingsDao;
    private final FaceRecognition faceRecognition;
    private final FaceAuthenticator faceAuthenticator;

    public FaceAuthenticationRepository(Context context) {
        try {
            this.faceEmbeddingDao = new FaceEmbeddingDao();
            this.logDao = new FaceRecognitionLogDao();
            this.securitySettingsDao = new SecuritySettingsDao();
            this.faceRecognition = new FaceRecognition(context);
            this.faceAuthenticator = new FaceAuthenticator(this.faceRecognition);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize face recognition", e);
        }
    }

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

    public Result<Boolean> hasFaceEnrolled(long userId) {
        try {
            return Result.success(faceEmbeddingDao.hasEnrollment(userId));
        } catch (SQLException e) {
            Log.e(TAG, "Error checking face enrollment", e);
            return Result.error("Failed to check face enrollment: " + e.getMessage());
        }
    }

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
            FaceRecognitionLog log = new FaceRecognitionLog();
            log.setUserId(userId);
            log.setSuccess(true);
            log.setSimilarity(1.0f);
            log.setActionType(FaceRecognitionLog.ActionType.ENROLLMENT);
            log.setDeviceInfo(android.os.Build.MODEL);
            log.setIpAddress("127.0.0.1");
            logDao.logAttempt(log);

            return Result.success(true);
        } catch (SQLException e) {
            Log.e(TAG, "Error during face enrollment", e);
            return Result.error("Face enrollment failed: " + e.getMessage());
        }
    }

    public Result<Boolean> deleteFace(long userId) {
        try {
            faceEmbeddingDao.delete(userId);

            return Result.success(true);
        } catch (SQLException e) {
            Log.e(TAG, "Error deleting face embedding", e);
            return Result.error("Failed to delete face enrollment: " + e.getMessage());
        }
    }

    public Result<Boolean> authenticate(Long userId, Bitmap faceBitmap) {
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
            boolean authenticated = faceAuthenticator.matchFace(storedEmbedding, newEmbedding);
            float similarity = faceAuthenticator.getLastSimilarityScore();

            // Log the authentication attempt
            FaceRecognitionLog log = new FaceRecognitionLog();
            log.setUserId(userId);
            log.setSuccess(authenticated);
            log.setSimilarity(similarity);
            log.setActionType(FaceRecognitionLog.ActionType.LOGIN);
            log.setDeviceInfo(android.os.Build.MODEL);
            log.setIpAddress("127.0.0.1");
            logDao.logAttempt(log);

            return Result.success(authenticated);
        } catch (SQLException e) {
            Log.e(TAG, "Error during face authentication", e);
            return Result.error("Authentication failed: " + e.getMessage());
        }
    }

    public Result<SecurityAssessment.SecurityMetrics> runSecurityAssessment(Long userId) {
        try {
            // Get the logs
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -30);
            java.sql.Date startDate = new java.sql.Date(cal.getTimeInMillis());
            java.sql.Date endDate = new java.sql.Date(System.currentTimeMillis());

            List<FaceRecognitionLog> logs = logDao.getLogsByDateRange(userId, startDate, endDate);
            boolean hasEnrollment = faceEmbeddingDao.hasEnrollment(userId);

            // Use the utility class to analyze the data
            SecurityAssessment.SecurityMetrics metrics =
                    SecurityAssessment.analyzeSecurityMetrics(logs, hasEnrollment);

            return Result.success(metrics);
        } catch (Exception e) {
            Log.e(TAG, "Error running security assessment", e);
            return Result.error("Failed to run security assessment: " + e.getMessage());
        }
    }
}