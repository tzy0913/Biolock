/**
 * Utility class for analyzing face recognition security metrics.
 * Evaluates login patterns, success rates, and potential security risks.
 * Provides recommendations and security status assessments.
 */
package com.biolock.utils;

import com.biolock.model.FaceRecognitionLog;
import java.util.*;

public class SecurityAssessment {
    private static final String TAG = "SecurityAssessment";

    // ============================
    // Security Status Constants
    // ============================
    private static final String STATUS_EXCELLENT = "EXCELLENT";
    private static final String STATUS_GOOD = "GOOD";
    private static final String STATUS_FAIR = "FAIR";
    private static final String STATUS_ATTENTION = "ATTENTION NEEDED";
    private static final String STATUS_NOT_CONFIGURED = "NOT CONFIGURED";

    // ============================
    // Security Metrics Class
    // ============================
    public static class SecurityMetrics {
        // Core metrics
        public boolean hasFaceEnrollment;
        public int totalLoginAttempts;
        public float successRate;
        public float avgSimilarity;
        public float highQualityRate;

        // Pattern analysis
        public int uniqueDeviceCount;
        public int uniqueIPCount;
        public int suspiciousTimingCount;
        public String mostCommonLoginTime;

        // Assessment results
        public String securityStatus;
        public List<String> securityWarnings;
        public List<String> recommendations;

        public SecurityMetrics() {
            this.securityWarnings = new ArrayList<>();
            this.recommendations = new ArrayList<>();
        }

        /**
         * Gets the color representing security status
         */
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

        /**
         * Checks if security recommendations are needed
         */
        public boolean needsRecommendations() {
            return "FAIR".equals(securityStatus) || "ATTENTION NEEDED".equals(securityStatus);
        }
    }

    // ============================
    // Main Analysis Methods
    // ============================

    /**
     * Analyzes security metrics from login logs
     * @param logs List of face recognition logs
     * @param hasFaceEnrollment Whether face is enrolled
     * @return SecurityMetrics containing analysis results
     */
    public static SecurityMetrics analyzeSecurityMetrics(List<FaceRecognitionLog> logs,
                                                         boolean hasFaceEnrollment) {
        SecurityMetrics metrics = new SecurityMetrics();
        metrics.hasFaceEnrollment = hasFaceEnrollment;

        if (!hasFaceEnrollment) {
            metrics.securityStatus = STATUS_NOT_CONFIGURED;
            return metrics;
        }

        calculateBasicMetrics(logs, metrics);
        analyzeLoginPatterns(logs, metrics);
        determineSecurityStatus(metrics);

        return metrics;
    }

    /**
     * Calculates basic success and quality metrics
     */
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

        if (metrics.totalLoginAttempts > 0) {
            metrics.successRate = ((float)(metrics.totalLoginAttempts - failedAttempts) /
                    metrics.totalLoginAttempts) * 100;

            int successfulAttempts = metrics.totalLoginAttempts - failedAttempts;
            if (successfulAttempts > 0) {
                metrics.avgSimilarity = totalSimilarity / successfulAttempts;
                metrics.highQualityRate = ((float)highSimilarityCount / successfulAttempts) * 100;
            }
        }
    }

    /**
     * Analyzes login patterns and suspicious activity
     */
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

        calculateMostCommonLoginTime(loginHours, metrics);
    }

    /**
     * Determines overall security status and generates recommendations
     */
    private static void determineSecurityStatus(SecurityMetrics metrics) {
        int riskFactors = assessRiskFactors(metrics);

        if (riskFactors == 0 && metrics.successRate >= 80 && metrics.avgSimilarity >= 0.85f) {
            metrics.securityStatus = STATUS_EXCELLENT;
        } else if (riskFactors <= 1 && metrics.successRate >= 60 && metrics.avgSimilarity >= 0.80f) {
            metrics.securityStatus = STATUS_GOOD;
        } else if (riskFactors <= 2 && metrics.successRate >= 40) {
            metrics.securityStatus = STATUS_FAIR;
        } else {
            metrics.securityStatus = STATUS_ATTENTION;
        }
    }

    // ============================
    // Report Generation Methods
    // ============================

    /**
     * Generates security recommendations if needed
     */
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

    /**
     * Generates comprehensive security report
     */
    public static String formatSecurityReport(SecurityMetrics metrics) {
        StringBuilder report = new StringBuilder("Security Assessment:\n\n");

        if (!metrics.hasFaceEnrollment) {
            report.append("Face recognition is not configured. Please enroll your face in the settings.\n");
            return report.toString();
        }

        appendLoginMetrics(report, metrics);
        appendPatternAnalysis(report, metrics);

        if (metrics.hasFaceEnrollment) {
            report.append("\nSecurity Status: ").append(metrics.securityStatus);
        }

        return report.toString();
    }

    // ============================
    // Helper Methods
    // ============================

    private static void calculateMostCommonLoginTime(List<Integer> loginHours, SecurityMetrics metrics) {
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

    private static int assessRiskFactors(SecurityMetrics metrics) {
        int riskFactors = 0;

        if (assessDeviceRisk(metrics)) riskFactors++;
        if (assessIPRisk(metrics)) riskFactors++;
        if (assessTimingRisk(metrics)) riskFactors++;
        if (assessSuccessRateRisk(metrics)) riskFactors++;
        if (assessSimilarityRisk(metrics)) riskFactors++;

        return riskFactors;
    }

    private static boolean assessDeviceRisk(SecurityMetrics metrics) {
        if (metrics.uniqueDeviceCount > 2) {
            metrics.securityWarnings.add("Multiple devices detected (" +
                    metrics.uniqueDeviceCount + " devices)");
            metrics.recommendations.add("Review and verify all devices used for login");
            return true;
        }
        return false;
    }

    private static boolean assessIPRisk(SecurityMetrics metrics) {
        if (metrics.uniqueIPCount > 2) {
            metrics.securityWarnings.add("Multiple IP addresses detected (" +
                    metrics.uniqueIPCount + " IPs)");
            metrics.recommendations.add("Check login locations for suspicious activity");
            return true;
        }
        return false;
    }

    private static boolean assessTimingRisk(SecurityMetrics metrics) {
        if (metrics.suspiciousTimingCount > 2) {
            metrics.securityWarnings.add(metrics.suspiciousTimingCount +
                    " login(s) during unusual hours (1 AM - 5 AM)");
            metrics.recommendations.add("Review unusual login time patterns");
            return true;
        }
        return false;
    }

    private static boolean assessSuccessRateRisk(SecurityMetrics metrics) {
        if (metrics.successRate < 60) {
            metrics.securityWarnings.add("Low success rate: " +
                    String.format(Locale.US, "%.1f%%", metrics.successRate));
            metrics.recommendations.add("Consider re-enrolling your face in better lighting");
            metrics.recommendations.add("Ensure consistent lighting during authentication");
            return true;
        }
        return false;
    }

    private static boolean assessSimilarityRisk(SecurityMetrics metrics) {
        if (metrics.avgSimilarity < 0.85f) {
            metrics.securityWarnings.add("Low average similarity score: " +
                    String.format(Locale.US, "%.2f", metrics.avgSimilarity));
            metrics.recommendations.add("Re-enroll your face to improve recognition accuracy");
            metrics.recommendations.add("Use consistent pose and expression when authenticating");
            return true;
        }
        return false;
    }

    private static void appendLoginMetrics(StringBuilder report, SecurityMetrics metrics) {
        if (metrics.totalLoginAttempts == 0) {
            report.append("No login attempts recorded yet.\n");
        } else {
            report.append(String.format(Locale.US, "Login Success Rate: %.1f%%\n", metrics.successRate));
            if (metrics.successRate > 0) {
                report.append(String.format(Locale.US, "Average Match Score: %.2f\n",
                        metrics.avgSimilarity));
                report.append(String.format(Locale.US, "High Quality Matches: %.1f%%\n",
                        metrics.highQualityRate));
            }
        }
    }

    private static void appendPatternAnalysis(StringBuilder report, SecurityMetrics metrics) {
        if (metrics.totalLoginAttempts > 0 && metrics.mostCommonLoginTime != null) {
            report.append("\nLogin Patterns (Last 30 days):\n");
            report.append("• Most frequent login time: ").append(metrics.mostCommonLoginTime)
                    .append("\n");

            for (String warning : metrics.securityWarnings) {
                report.append("• ").append(warning).append("\n");
            }
        }
    }
}