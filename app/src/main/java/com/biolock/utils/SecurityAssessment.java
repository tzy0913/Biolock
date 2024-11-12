/**
 * Utility class for analyzing face recognition security metrics.
 * Evaluates login patterns, attendance patterns, success rates, and potential security risks.
 * Provides recommendations and security status assessments.
 */
package com.biolock.utils;

// Biolock Models
import com.biolock.model.FaceRecognitionLog;

// Java Collections
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
        public int totalAttendanceAttempts;
        public float loginSuccessRate;
        public float attendanceSuccessRate;
        public float avgLoginSimilarity;
        public float avgAttendanceSimilarity;
        public float highQualityLoginRate;
        public float highQualityAttendanceRate;

        // Pattern analysis
        public int uniqueDeviceCount;
        public int uniqueIPCount;
        public int suspiciousTimingCount;
        public String mostCommonLoginTime;
        public String mostCommonAttendanceTime;

        // Attendance-specific metrics
        public int duplicateAttendanceAttempts;
        public int outOfScheduleAttempts;
        public Map<String, Integer> attendanceByDayOfWeek;
        public float attendanceComplianceRate;

        // Assessment results
        public String securityStatus;
        public List<String> securityWarnings;
        public List<String> recommendations;

        public SecurityMetrics() {
            this.securityWarnings = new ArrayList<>();
            this.recommendations = new ArrayList<>();
            this.attendanceByDayOfWeek = new HashMap<>();
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

    // ============================
    // Main Analysis Methods
    // ============================

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
        analyzeAttendancePatterns(logs, metrics);
        determineSecurityStatus(metrics);

        return metrics;
    }

    private static void calculateBasicMetrics(List<FaceRecognitionLog> logs, SecurityMetrics metrics) {
        int failedLogins = 0;
        int failedAttendance = 0;
        int highLoginSimilarityCount = 0;
        int highAttendanceSimilarityCount = 0;
        float totalLoginSimilarity = 0;
        float totalAttendanceSimilarity = 0;

        for (FaceRecognitionLog log : logs) {
            if (log.getActionType() == FaceRecognitionLog.ActionType.LOGIN) {
                metrics.totalLoginAttempts++;
                if (!log.isSuccess()) {
                    failedLogins++;
                } else {
                    totalLoginSimilarity += log.getSimilarity();
                    if (log.getSimilarity() >= 0.85f) {
                        highLoginSimilarityCount++;
                    }
                }
            } else if (log.getActionType() == FaceRecognitionLog.ActionType.ATTENDANCE) {
                metrics.totalAttendanceAttempts++;
                if (!log.isSuccess()) {
                    failedAttendance++;
                } else {
                    totalAttendanceSimilarity += log.getSimilarity();
                    if (log.getSimilarity() >= 0.85f) {
                        highAttendanceSimilarityCount++;
                    }
                }
            }
        }

        calculateSuccessRates(metrics, failedLogins, failedAttendance,
                totalLoginSimilarity, totalAttendanceSimilarity,
                highLoginSimilarityCount, highAttendanceSimilarityCount);
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

        calculateMostCommonLoginTime(loginHours, metrics);
    }

    private static void analyzeAttendancePatterns(List<FaceRecognitionLog> logs, SecurityMetrics metrics) {
        Map<String, Set<Date>> dailyAttendance = new HashMap<>();
        Calendar cal = Calendar.getInstance();
        String[] daysOfWeek = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

        for (FaceRecognitionLog log : logs) {
            if (log.getActionType() == FaceRecognitionLog.ActionType.ATTENDANCE) {
                cal.setTime(log.getAttemptTimestamp());
                String dateKey = String.format("%d-%02d-%02d",
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH));

                // Track attendance by day of week
                String dayOfWeek = daysOfWeek[cal.get(Calendar.DAY_OF_WEEK) - 1];
                metrics.attendanceByDayOfWeek.put(dayOfWeek,
                        metrics.attendanceByDayOfWeek.getOrDefault(dayOfWeek, 0) + 1);

                // Check for duplicate attendance
                Set<Date> dayAttendance = dailyAttendance.computeIfAbsent(dateKey, k -> new HashSet<>());
                if (!dayAttendance.isEmpty()) {
                    metrics.duplicateAttendanceAttempts++;
                }
                dayAttendance.add(log.getAttemptTimestamp());

                // Check for out-of-schedule attempts (assuming 7AM-7PM is normal)
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                if (hour < 7 || hour > 19) {
                    metrics.outOfScheduleAttempts++;
                }

                // Track most common attendance time
                if (metrics.mostCommonAttendanceTime == null) {
                    metrics.mostCommonAttendanceTime = String.format("%02d:00", hour);
                }
            }
        }

        // Calculate attendance compliance rate
        long totalWorkdays = calculateWorkdaysBetween(logs);
        if (totalWorkdays > 0) {
            metrics.attendanceComplianceRate = (dailyAttendance.size() / (float) totalWorkdays) * 100;
        }

        assessAttendancePatterns(metrics);
    }

    private static void determineSecurityStatus(SecurityMetrics metrics) {
        int riskFactors = assessRiskFactors(metrics);

        // Consider both login and attendance success rates
        float overallSuccessRate = (metrics.loginSuccessRate + metrics.attendanceSuccessRate) / 2;
        float overallSimilarity = (metrics.avgLoginSimilarity + metrics.avgAttendanceSimilarity) / 2;

        if (riskFactors == 0 && overallSuccessRate >= 80 && overallSimilarity >= 0.85f) {
            metrics.securityStatus = STATUS_EXCELLENT;
        } else if (riskFactors <= 1 && overallSuccessRate >= 60 && overallSimilarity >= 0.80f) {
            metrics.securityStatus = STATUS_GOOD;
        } else if (riskFactors <= 2 && overallSuccessRate >= 40) {
            metrics.securityStatus = STATUS_FAIR;
        } else {
            metrics.securityStatus = STATUS_ATTENTION;
        }
    }

    // ============================
    // Helper Methods
    // ============================

    private static void calculateSuccessRates(SecurityMetrics metrics, int failedLogins,
                                              int failedAttendance, float totalLoginSimilarity, float totalAttendanceSimilarity,
                                              int highLoginSimilarityCount, int highAttendanceSimilarityCount) {

        if (metrics.totalLoginAttempts > 0) {
            metrics.loginSuccessRate = ((float)(metrics.totalLoginAttempts - failedLogins) /
                    metrics.totalLoginAttempts) * 100;
            int successfulLogins = metrics.totalLoginAttempts - failedLogins;
            if (successfulLogins > 0) {
                metrics.avgLoginSimilarity = totalLoginSimilarity / successfulLogins;
                metrics.highQualityLoginRate = ((float)highLoginSimilarityCount / successfulLogins) * 100;
            }
        }

        if (metrics.totalAttendanceAttempts > 0) {
            metrics.attendanceSuccessRate = ((float)(metrics.totalAttendanceAttempts - failedAttendance) /
                    metrics.totalAttendanceAttempts) * 100;
            int successfulAttendance = metrics.totalAttendanceAttempts - failedAttendance;
            if (successfulAttendance > 0) {
                metrics.avgAttendanceSimilarity = totalAttendanceSimilarity / successfulAttendance;
                metrics.highQualityAttendanceRate = ((float)highAttendanceSimilarityCount / successfulAttendance) * 100;
            }
        }
    }

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

    private static void assessAttendancePatterns(SecurityMetrics metrics) {
        // Check duplicate attempts
        if (metrics.duplicateAttendanceAttempts > 0) {
            metrics.securityWarnings.add(String.format("Detected %d duplicate attendance attempts",
                    metrics.duplicateAttendanceAttempts));
            metrics.recommendations.add("Review attendance policy regarding multiple daily check-ins");
        }

        // Check out-of-schedule attempts
        if (metrics.outOfScheduleAttempts > 0) {
            metrics.securityWarnings.add(String.format("%d attendance attempts outside normal hours",
                    metrics.outOfScheduleAttempts));
            metrics.recommendations.add("Verify unusual attendance timing patterns");
        }

        // Check attendance compliance
        if (metrics.attendanceComplianceRate < 80) {
            metrics.securityWarnings.add(String.format("Low attendance compliance rate: %.1f%%",
                    metrics.attendanceComplianceRate));
            metrics.recommendations.add("Review attendance requirements and improve compliance");
        }
    }

    private static int assessRiskFactors(SecurityMetrics metrics) {
        int riskFactors = 0;

        if (assessDeviceRisk(metrics)) riskFactors++;
        if (assessIPRisk(metrics)) riskFactors++;
        if (assessTimingRisk(metrics)) riskFactors++;
        if (assessSuccessRateRisk(metrics)) riskFactors++;
        if (assessSimilarityRisk(metrics)) riskFactors++;
        if (assessAttendanceRisk(metrics)) riskFactors++;

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
        float overallSuccessRate = (metrics.loginSuccessRate + metrics.attendanceSuccessRate) / 2;
        if (overallSuccessRate < 60) {
            metrics.securityWarnings.add("Low overall success rate: " +
                    String.format(Locale.US, "%.1f%%", overallSuccessRate));
            metrics.recommendations.add("Consider re-enrolling your face in better lighting");
            metrics.recommendations.add("Ensure consistent lighting during authentication");
            return true;
        }
        return false;
    }

    private static boolean assessSimilarityRisk(SecurityMetrics metrics) {
        float overallSimilarity = (metrics.avgLoginSimilarity + metrics.avgAttendanceSimilarity) / 2;
        if (overallSimilarity < 0.85f) {
            metrics.securityWarnings.add("Low average similarity score: " +
                    String.format(Locale.US, "%.2f", overallSimilarity));
            metrics.recommendations.add("Re-enroll your face to improve recognition accuracy");
            metrics.recommendations.add("Use consistent pose and expression when authenticating");
            return true;
        }
        return false;
    }

    private static boolean assessAttendanceRisk(SecurityMetrics metrics) {
        boolean hasRisk = false;

        if (metrics.duplicateAttendanceAttempts > 5) {
            hasRisk = true;
        }

        if (metrics.outOfScheduleAttempts > 5) {
            hasRisk = true;
        }

        if (metrics.attendanceComplianceRate < 70) {
            hasRisk = true;
        }

        return hasRisk;
    }

    private static long calculateWorkdaysBetween(List<FaceRecognitionLog> logs) {
        if (logs.isEmpty()) return 0;

        Date firstDate = null;
        Date lastDate = null;
        for (FaceRecognitionLog log : logs) {
            if (log.getActionType() == FaceRecognitionLog.ActionType.ATTENDANCE) {
                if (firstDate == null || log.getAttemptTimestamp().before(firstDate)) {
                    firstDate = log.getAttemptTimestamp();
                }
                if (lastDate == null || log.getAttemptTimestamp().after(lastDate)) {
                    lastDate = log.getAttemptTimestamp();
                }
            }
        }

        if (firstDate == null || lastDate == null) return 0;

        // Calculate workdays (excluding weekends)
        Calendar cal = Calendar.getInstance();
        cal.setTime(firstDate);
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(lastDate);

        int workdays = 0;
        while (cal.before(endCal)) {
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                workdays++;
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return workdays;
    }

    // ============================
    // Report Generation Methods
    // ============================

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
        StringBuilder report = new StringBuilder("Security Assessment Report:\n\n");

        if (!metrics.hasFaceEnrollment) {
            report.append("Face recognition is not configured. Please enroll your face in the settings.\n");
            return report.toString();
        }

        appendLoginMetrics(report, metrics);
        appendAttendanceMetrics(report, metrics);
        appendPatternAnalysis(report, metrics);

        if (metrics.hasFaceEnrollment) {
            report.append("\nOverall Security Status: ").append(metrics.securityStatus);
        }

        return report.toString();
    }

    private static void appendLoginMetrics(StringBuilder report, SecurityMetrics metrics) {
        report.append("Login Metrics:\n");
        if (metrics.totalLoginAttempts == 0) {
            report.append("No login attempts recorded yet.\n");
        } else {
            report.append(String.format(Locale.US, "Success Rate: %.1f%%\n", metrics.loginSuccessRate));
            report.append(String.format(Locale.US, "Total Attempts: %d\n", metrics.totalLoginAttempts));
            if (metrics.loginSuccessRate > 0) {
                report.append(String.format(Locale.US, "Average Match Score: %.2f\n",
                        metrics.avgLoginSimilarity));
                report.append(String.format(Locale.US, "High Quality Matches: %.1f%%\n",
                        metrics.highQualityLoginRate));
            }
        }
    }

    private static void appendAttendanceMetrics(StringBuilder report, SecurityMetrics metrics) {
        report.append("\nAttendance Metrics:\n");
        if (metrics.totalAttendanceAttempts == 0) {
            report.append("No attendance records yet.\n");
        } else {
            report.append(String.format(Locale.US, "Success Rate: %.1f%%\n",
                    metrics.attendanceSuccessRate));
            report.append(String.format(Locale.US, "Total Attempts: %d\n",
                    metrics.totalAttendanceAttempts));
            report.append(String.format(Locale.US, "Compliance Rate: %.1f%%\n",
                    metrics.attendanceComplianceRate));

            if (metrics.attendanceSuccessRate > 0) {
                report.append(String.format(Locale.US, "Average Match Score: %.2f\n",
                        metrics.avgAttendanceSimilarity));
                report.append(String.format(Locale.US, "High Quality Matches: %.1f%%\n",
                        metrics.highQualityAttendanceRate));
            }

            if (metrics.duplicateAttendanceAttempts > 0 || metrics.outOfScheduleAttempts > 0) {
                report.append("\nAttendance Patterns:\n");
                if (metrics.duplicateAttendanceAttempts > 0) {
                    report.append(String.format("• %d duplicate attendance attempts\n",
                            metrics.duplicateAttendanceAttempts));
                }
                if (metrics.outOfScheduleAttempts > 0) {
                    report.append(String.format("• %d out-of-schedule attempts\n",
                            metrics.outOfScheduleAttempts));
                }
            }
        }
    }

    private static void appendPatternAnalysis(StringBuilder report, SecurityMetrics metrics) {
        if (metrics.totalLoginAttempts > 0 || metrics.totalAttendanceAttempts > 0) {
            report.append("\nBehavior Patterns:\n");

            if (metrics.mostCommonLoginTime != null) {
                report.append("• Most frequent login time: ")
                        .append(metrics.mostCommonLoginTime)
                        .append("\n");
            }

            if (metrics.mostCommonAttendanceTime != null) {
                report.append("• Most frequent attendance time: ")
                        .append(metrics.mostCommonAttendanceTime)
                        .append("\n");
            }

            if (!metrics.securityWarnings.isEmpty()) {
                report.append("\nWarnings:\n");
                for (String warning : metrics.securityWarnings) {
                    report.append("• ").append(warning).append("\n");
                }
            }

            if (!metrics.recommendations.isEmpty()) {
                report.append("\nRecommendations:\n");
                for (String recommendation : metrics.recommendations) {
                    report.append("• ").append(recommendation).append("\n");
                }
            }
        }
    }
}