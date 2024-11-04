package com.biolock.utils;

import android.util.Log;
import com.biolock.model.FaceRecognitionLog;
import com.biolock.repository.FaceEmbeddingRepository;
import com.biolock.repository.FaceRecognitionLogRepository;
import com.biolock.repository.Result;

import java.util.*;

public class SecurityAssessment {
    private static final String TAG = "SecurityAssessment";

    public static class SecurityMetrics {
        public boolean hasFaceEnrollment;
        public int totalLoginAttempts;
        public float successRate;
        public float avgSimilarity;
        public float highQualityRate;
        public int uniqueDeviceCount;
        public int uniqueIPCount;
        public int suspiciousTimingCount;
        public String mostCommonLoginTime;
        public String securityStatus;
        public List<String> securityWarnings;

        public SecurityMetrics() {
            this.securityWarnings = new ArrayList<>();
        }
    }

    public static Result<SecurityMetrics> analyzeSecurityStatus(
            long userId,
            FaceEmbeddingRepository faceEmbeddingRepository,
            FaceRecognitionLogRepository faceRecognitionLogRepository) {
        try {
            SecurityMetrics metrics = new SecurityMetrics();

            // Check face enrollment
            Result<Boolean> hasFaceResult = faceEmbeddingRepository.hasFaceEmbedding(userId);
            if (!hasFaceResult.isSuccess()) {
                return Result.error(new Exception("Failed to check face enrollment"));
            }
            metrics.hasFaceEnrollment = hasFaceResult.getData();

            if (!metrics.hasFaceEnrollment) {
                metrics.securityStatus = "Face enrollment required for biometric authentication";
                return Result.success(metrics);
            }

            // Get logs
            Result<List<FaceRecognitionLog>> faceRecognitionLogResult = faceRecognitionLogRepository.getUserLogs(userId);
            if (!faceRecognitionLogResult.isSuccess()) {
                return Result.error(new Exception("Failed to retrieve logs"));
            }

            List<FaceRecognitionLog> faceRecognitionLog = faceRecognitionLogResult.getData();
            calculateBasicMetrics(faceRecognitionLog, metrics);
            analyzeLoginPatterns(faceRecognitionLog, metrics);
            determineSecurityStatus(metrics);

            return Result.success(metrics);
        } catch (Exception e) {
            Log.e(TAG, "Error analyzing security status", e);
            return Result.error(e);
        }
    }

    private static void calculateBasicMetrics(List<FaceRecognitionLog> faceRecognitionLog, SecurityMetrics metrics) {
        int failedAttempts = 0;
        int highSimilarityCount = 0;
        float totalSimilarity = 0;

        for (FaceRecognitionLog log : faceRecognitionLog) {
            if (log.getActionType() == FaceRecognitionLog.ActionType.LOGIN) {
                metrics.totalLoginAttempts++;
                if (!log.isSuccess()) {
                    failedAttempts++;
                } else {
                    totalSimilarity += log.getSimilarity();
                    if (log.getSimilarity() >= 0.85f) {
                        highSimilarityCount++;
                    }
                }
            }
        }

        // Calculate rates
        if (metrics.totalLoginAttempts > 0) {
            metrics.successRate = ((float)(metrics.totalLoginAttempts - failedAttempts) / metrics.totalLoginAttempts) * 100;

            int successfulAttempts = metrics.totalLoginAttempts - failedAttempts;
            if (successfulAttempts > 0) {
                metrics.avgSimilarity = totalSimilarity / successfulAttempts;
                metrics.highQualityRate = ((float)highSimilarityCount / successfulAttempts) * 100;
            }
        }
    }

    private static void analyzeLoginPatterns(List<FaceRecognitionLog> faceRecognitionLog, SecurityMetrics metrics) {
        Set<String> uniqueDevices = new HashSet<>();
        Set<String> uniqueIPs = new HashSet<>();
        List<Integer> loginHours = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        long currentTime = System.currentTimeMillis();
        long thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000;

        for (FaceRecognitionLog log : faceRecognitionLog) {
            if (log.getActionType() == FaceRecognitionLog.ActionType.LOGIN &&
                    currentTime - log.getAttemptTimestamp().getTime() <= thirtyDaysMillis) {

                uniqueDevices.add(log.getDeviceInfo());
                uniqueIPs.add(log.getIpAddress());

                cal.setTime(log.getAttemptTimestamp());
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                loginHours.add(hour);

                if (hour >= 1 && hour <= 5) {
                    metrics.suspiciousTimingCount++;
                }
            }
        }

        metrics.uniqueDeviceCount = uniqueDevices.size();
        metrics.uniqueIPCount = uniqueIPs.size();

        if (!loginHours.isEmpty()) {
            Map<Integer, Integer> hourFrequency = new HashMap<>();
            for (int hour : loginHours) {
                hourFrequency.put(hour, hourFrequency.getOrDefault(hour, 0) + 1);
            }
            int mostCommonHour = Collections.max(hourFrequency.entrySet(),
                    Map.Entry.comparingByValue()).getKey();
            metrics.mostCommonLoginTime = String.format(Locale.US, "%02d:00 - %02d:00",
                    mostCommonHour, (mostCommonHour + 1) % 24);
        }
    }

    private static void determineSecurityStatus(SecurityMetrics metrics) {
        int riskFactors = 0;

        if (metrics.uniqueDeviceCount > 2) {
            riskFactors++;
            metrics.securityWarnings.add("Multiple devices detected (" +
                    metrics.uniqueDeviceCount + " devices)");
        }

        if (metrics.uniqueIPCount > 2) {
            riskFactors++;
            metrics.securityWarnings.add("Multiple IP addresses detected (" +
                    metrics.uniqueIPCount + " IPs)");
        }

        if (metrics.suspiciousTimingCount > 2) {
            riskFactors++;
            metrics.securityWarnings.add(metrics.suspiciousTimingCount +
                    " login(s) during unusual hours (1 AM - 5 AM)");
        }

        if (metrics.successRate < 60) {
            riskFactors++;
            metrics.securityWarnings.add("Low success rate: " +
                    String.format(Locale.US, "%.1f%%", metrics.successRate));
        }

        if (metrics.avgSimilarity < 0.85f) {
            riskFactors++;
            metrics.securityWarnings.add("Low average similarity score: " +
                    String.format(Locale.US, "%.2f", metrics.avgSimilarity));
        }

        if (riskFactors == 0 && metrics.successRate >= 80 && metrics.avgSimilarity >= 0.85f) {
            metrics.securityStatus = "EXCELLENT";
        } else if (riskFactors <= 1 && metrics.successRate >= 60 && metrics.avgSimilarity >= 0.80f) {
            metrics.securityStatus = "GOOD";
        } else if (riskFactors <= 2 && metrics.successRate >= 40) {
            metrics.securityStatus = "FAIR";
        } else {
            metrics.securityStatus = "ATTENTION NEEDED";
        }
    }

    public static String formatSecurityReport(SecurityMetrics metrics) {
        StringBuilder report = new StringBuilder("Security Assessment:\n\n");

        // Basic metrics
        if (metrics.totalLoginAttempts == 0) {
            report.append("Face recognition configured but no login attempts recorded.\n");
        } else {
            report.append(String.format(Locale.US, "Login Success Rate: %.1f%%\n", metrics.successRate));
            if (metrics.successRate > 0) {
                report.append(String.format(Locale.US, "Average Match Score: %.2f\n", metrics.avgSimilarity));
                report.append(String.format(Locale.US, "High Quality Matches: %.1f%%\n", metrics.highQualityRate));
            }
        }

        // Pattern analysis
        if (metrics.mostCommonLoginTime != null) {
            report.append("\nLogin Patterns (Last 30 days):\n");
            report.append("• Most frequent login time: ").append(metrics.mostCommonLoginTime).append("\n");

            for (String warning : metrics.securityWarnings) {
                report.append("• ").append(warning).append("\n");
            }
        }

        // Overall status
        report.append("\nSecurity Status: ").append(metrics.securityStatus);

        return report.toString();
    }
}
