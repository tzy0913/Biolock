package com.biolock.utils;

import com.biolock.model.FaceRecognitionLog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
        public List<String> recommendations;

        public SecurityMetrics() {
            this.securityWarnings = new ArrayList<>();
            this.recommendations = new ArrayList<>();
        }

        public int getStatusColor() {
            switch (securityStatus) {
                case "EXCELLENT":
                    return android.R.color.holo_green_dark;
                case "GOOD":
                    return android.R.color.holo_green_light;
                case "FAIR":
                    return android.R.color.holo_orange_light;
                case "ATTENTION NEEDED":
                    return android.R.color.holo_red_light;
                default:
                    return android.R.color.darker_gray;
            }
        }

        public boolean needsRecommendations() {
            return "FAIR".equals(securityStatus) || "ATTENTION NEEDED".equals(securityStatus);
        }
    }

    public static SecurityMetrics analyzeSecurityMetrics(List<FaceRecognitionLog> logs, boolean hasFaceEnrollment) {
        SecurityMetrics metrics = new SecurityMetrics();
        metrics.hasFaceEnrollment = hasFaceEnrollment;

        if (!hasFaceEnrollment) {
            metrics.securityStatus = "NOT CONFIGURED";
            return metrics;
        }

        calculateBasicMetrics(logs, metrics);
        analyzeLoginPatterns(logs, metrics);
        determineSecurityStatus(metrics);

        return metrics;
    }

    private static void calculateBasicMetrics(List<FaceRecognitionLog> logs, SecurityMetrics metrics) {
        int failedAttempts = 0;
        int highSimilarityCount = 0;
        float totalSimilarity = 0;

        for (FaceRecognitionLog log : logs) {
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

    private static void analyzeLoginPatterns(List<FaceRecognitionLog> logs, SecurityMetrics metrics) {
        Set<String> uniqueDevices = new HashSet<>();
        Set<String> uniqueIPs = new HashSet<>();
        List<Integer> loginHours = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        long currentTime = System.currentTimeMillis();
        long thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000;

        for (FaceRecognitionLog log : logs) {
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
            metrics.recommendations.add("Review and verify all devices used for login");
        }

        if (metrics.uniqueIPCount > 2) {
            riskFactors++;
            metrics.securityWarnings.add("Multiple IP addresses detected (" +
                    metrics.uniqueIPCount + " IPs)");
            metrics.recommendations.add("Check login locations for suspicious activity");
        }

        if (metrics.suspiciousTimingCount > 2) {
            riskFactors++;
            metrics.securityWarnings.add(metrics.suspiciousTimingCount +
                    " login(s) during unusual hours (1 AM - 5 AM)");
            metrics.recommendations.add("Review unusual login time patterns");
        }

        if (metrics.successRate < 60) {
            riskFactors++;
            metrics.securityWarnings.add("Low success rate: " +
                    String.format(Locale.US, "%.1f%%", metrics.successRate));
            metrics.recommendations.add("Consider re-enrolling your face in better lighting");
            metrics.recommendations.add("Ensure consistent lighting during authentication");
        }

        if (metrics.avgSimilarity < 0.85f) {
            riskFactors++;
            metrics.securityWarnings.add("Low average similarity score: " +
                    String.format(Locale.US, "%.2f", metrics.avgSimilarity));
            metrics.recommendations.add("Re-enroll your face to improve recognition accuracy");
            metrics.recommendations.add("Use consistent pose and expression when authenticating");
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

    public static String getRecommendations(SecurityMetrics metrics) {
        if (!metrics.needsRecommendations()) {
            return null;
        }

        StringBuilder recommendations = new StringBuilder();
        recommendations.append("Recommended Actions:\n\n");

        for (String recommendation : metrics.recommendations) {
            recommendations.append("• ").append(recommendation).append("\n");
        }

        return recommendations.toString();
    }

    public static String formatSecurityReport(SecurityMetrics metrics) {
        StringBuilder report = new StringBuilder("Security Assessment:\n\n");

        // Check enrollment status first
        if (!metrics.hasFaceEnrollment) {
            report.append("Face recognition is not configured. Please enroll your face in the settings.\n");
            return report.toString();
        }

        // Then check login attempts
        if (metrics.totalLoginAttempts == 0) {
            report.append("No login attempts recorded yet.\n");
        } else {
            report.append(String.format(Locale.US, "Login Success Rate: %.1f%%\n", metrics.successRate));
            if (metrics.successRate > 0) {
                report.append(String.format(Locale.US, "Average Match Score: %.2f\n", metrics.avgSimilarity));
                report.append(String.format(Locale.US, "High Quality Matches: %.1f%%\n", metrics.highQualityRate));
            }
        }

        // Pattern analysis - only show if there are login attempts
        if (metrics.totalLoginAttempts > 0 && metrics.mostCommonLoginTime != null) {
            report.append("\nLogin Patterns (Last 30 days):\n");
            report.append("• Most frequent login time: ").append(metrics.mostCommonLoginTime).append("\n");

            for (String warning : metrics.securityWarnings) {
                report.append("• ").append(warning).append("\n");
            }
        }

        // Show overall status only if face is enrolled
        if (metrics.hasFaceEnrollment) {
            report.append("\nSecurity Status: ").append(metrics.securityStatus);
        }

        return report.toString();
    }
}
