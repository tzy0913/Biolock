package com.biolock.ui.dashboard;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.biolock.R;
import com.biolock.database.DatabaseHelper;
import com.biolock.model.Attendance;
import com.biolock.ui.settings.SettingsActivity;
import com.biolock.utils.SessionManager;
import com.biolock.ui.login.LoginActivity;
import com.biolock.ui.attendance.ViewAttendanceActivity;
import com.biolock.repository.SessionRepository;
import com.biolock.repository.ClassRepository;
import com.biolock.repository.AttendanceRepository;
import com.biolock.model.Session;
import com.biolock.model.Class;
import com.biolock.repository.Result;

import java.time.LocalTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class DashboardActivity extends AppCompatActivity {
    private static final String TAG = "DashboardActivity";
    private SessionManager sessionManager;
    private TextView textClassInfo;
    private TextView textGreeting;
    private Button buttonMarkAttendance;
    private DatabaseHelper dbHelper;
    private SessionRepository sessionRepository;
    private ClassRepository classRepository;
    private AttendanceRepository attendanceRepository;
    private Session currentSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        sessionRepository = new SessionRepository();
        classRepository = new ClassRepository();
        attendanceRepository = new AttendanceRepository();

        initializeDatabase();
        initializeViews();
        setupClickListeners();
        updateDashboard();
    }

    private void initializeDatabase() {
        new Thread(() -> {
            try {
                dbHelper = DatabaseHelper.getInstance();
                dbHelper.initialize();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Database initialization failed", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void initializeViews() {
        textClassInfo = findViewById(R.id.textClassInfo);
        textGreeting = findViewById(R.id.textGreeting);
        buttonMarkAttendance = findViewById(R.id.buttonMarkAttendance);
    }

    private void setupClickListeners() {
        findViewById(R.id.buttonSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        findViewById(R.id.buttonLogout).setOnClickListener(v -> {
            sessionManager.clearLoginState();
            if (dbHelper != null) {
                dbHelper.cleanup();
            }
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        });

        buttonMarkAttendance.setOnClickListener(v -> {
            if (currentSession != null) {
                markAttendance(currentSession.getSessionId());
            }
        });

        findViewById(R.id.buttonViewAttendance).setOnClickListener(v -> {
            startActivity(new Intent(this, ViewAttendanceActivity.class));
        });
    }

    private void markAttendance(int sessionId) {
        new Thread(() -> {
            Result<Long> result = attendanceRepository.markAttendance(
                    sessionManager.getUserId(),
                    sessionId
            );

            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    Toast.makeText(this, "Attendance marked successfully", Toast.LENGTH_SHORT).show();
                    checkCurrentClass(); // Refresh the display
                } else {
                    Toast.makeText(this, "Failed to mark attendance: " +
                            result.getError().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void updateDashboard() {
        String userName = sessionManager.getUserName();
        textGreeting.setText(String.format("Hi, %s!", userName));
        checkCurrentClass();
    }

    private void checkCurrentClass() {
        new Thread(() -> {
            try {
                LocalDate today = LocalDate.now();
                LocalTime currentTime = LocalTime.now();

                Log.d(TAG, "Checking class at: " + currentTime + " on " + today);

                Result<List<Session>> sessionsResult = sessionRepository.getSessionsByDate(today);
                if (!sessionsResult.isSuccess()) {
                    Log.e(TAG, "Failed to fetch sessions: " + sessionsResult.getError().getMessage());
                    showError("Could not fetch sessions");
                    return;
                }

                List<Session> sessions = sessionsResult.getData();
                Log.d(TAG, "Found " + sessions.size() + " sessions for today");

                currentSession = sessions.stream()
                        .peek(s -> Log.d(TAG, String.format("Checking session: start=%s, end=%s",
                                s.getStartTime(), s.getEndTime())))
                        .filter(session -> {
                            LocalTime markingStartTime = session.getStartTime().minusMinutes(30);
                            boolean canMark = (currentTime.isAfter(markingStartTime) ||
                                    currentTime.equals(markingStartTime)) &&
                                    currentTime.isBefore(session.getEndTime());
                            Log.d(TAG, String.format("Session %d: markingStart=%s, canMark=%b",
                                    session.getSessionId(), markingStartTime, canMark));
                            return canMark;
                        })
                        .findFirst()
                        .orElse(null);

                if (currentSession != null) {
                    Log.d(TAG, "Found current session: " + currentSession.getSessionId());
                    Result<Class> classResult = classRepository.getClassById(currentSession.getClassId());
                    if (classResult.isSuccess()) {
                        Class classData = classResult.getData();

                        Result<List<Attendance>> attendanceResult = attendanceRepository.getTodayAttendance(
                                sessionManager.getUserId()
                        );

                        boolean alreadyMarked = false;
                        String attendanceStatus = "Not Marked";
                        if (attendanceResult.isSuccess()) {
                            Optional<Attendance> existingAttendance = attendanceResult.getData().stream()
                                    .filter(a -> a.getSessionId() == currentSession.getSessionId())
                                    .findFirst();

                            if (existingAttendance.isPresent()) {
                                String status = existingAttendance.get().getStatus();
                                // Only consider it marked if status is present/late/absent
                                alreadyMarked = "present".equalsIgnoreCase(status) ||
                                        "late".equalsIgnoreCase(status) ||
                                        "absent".equalsIgnoreCase(status);
                                attendanceStatus = status;
                                Log.d(TAG, "Found existing attendance: " + status + ", alreadyMarked: " + alreadyMarked);
                            }
                        }

                        LocalTime lateAfterTime = currentSession.getStartTime().plusHours(1);
                        String markingStatus = currentTime.isAfter(lateAfterTime) ?
                                "Attendance will be marked as late" : "";

                        final boolean finalMarked = alreadyMarked;
                        final String finalStatus = attendanceStatus;
                        final String finalMarkingStatus = markingStatus;

                        runOnUiThread(() -> {
                            SpannableStringBuilder builder = new SpannableStringBuilder();
                            builder.append(String.format("%s - %s\n%s to %s\nRoom: %s\n",
                                    classData.getModuleCode(),
                                    classData.getModuleName(),
                                    currentSession.getStartTime().toString(),
                                    currentSession.getEndTime().toString(),
                                    classData.getRoom()));

                            // Add marking status with dark red color if it's late
                            if (!finalMarkingStatus.isEmpty()) {
                                SpannableString statusText = new SpannableString(finalMarkingStatus + "\n");
                                statusText.setSpan(
                                        new ForegroundColorSpan(
                                                ContextCompat.getColor(this, android.R.color.holo_red_dark)
                                        ),
                                        0,
                                        finalMarkingStatus.length(),
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                );
                                builder.append(statusText);
                            }

                            // Add attendance status
                            builder.append("Attendance marked: ");
                            String status = finalStatus;
                            if (status.equalsIgnoreCase("late") || status.equalsIgnoreCase("absent")) {
                                SpannableString statusText = new SpannableString(status);
                                statusText.setSpan(
                                        new ForegroundColorSpan(
                                                ContextCompat.getColor(this, android.R.color.holo_red_dark)
                                        ),
                                        0,
                                        status.length(),
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                );
                                builder.append(statusText);
                            } else if (status.equalsIgnoreCase("present")) {
                                SpannableString statusText = new SpannableString(status);
                                statusText.setSpan(
                                        new ForegroundColorSpan(
                                                ContextCompat.getColor(this, android.R.color.holo_green_dark)
                                        ),
                                        0,
                                        status.length(),
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                );
                                builder.append(statusText);
                            } else {
                                builder.append(status); // Not marked stays default color
                            }

                            textClassInfo.setText(builder);
                            buttonMarkAttendance.setEnabled(!finalMarked);
                            Log.d(TAG, "Updated UI - button enabled: " + !finalMarked);
                        });
                    } else {
                        Log.e(TAG, "Failed to get class data: " + classResult.getError().getMessage());
                        showError("Failed to get class information");
                    }
                } else {
                    Log.d(TAG, "No current session found");
                    runOnUiThread(() -> {
                        textClassInfo.setText("No class available for attendance marking");
                        buttonMarkAttendance.setEnabled(false);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in checkCurrentClass", e);
                showError("Error checking class schedule: " + e.getMessage());
            }
        }).start();
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            textClassInfo.setText("Error checking class schedule");
            buttonMarkAttendance.setEnabled(false);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkCurrentClass();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.cleanup();
        }
    }
}