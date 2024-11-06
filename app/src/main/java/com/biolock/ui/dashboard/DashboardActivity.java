package com.biolock.ui.dashboard;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.biolock.R;
import com.biolock.model.Attendance;
import com.biolock.model.CourseClass;
import com.biolock.repository.AttendanceRepository;
import com.biolock.repository.Result;
import com.biolock.repository.SessionRepository;
import com.biolock.ui.attendance.ViewAttendanceActivity;
import com.biolock.ui.login.LoginActivity;
import com.biolock.ui.settings.SettingsActivity;
import com.biolock.utils.SessionManager;
import com.biolock.model.User;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {
    private static final String TAG = "DashboardActivity";
    private TextView textGreeting;
    private TextView textClassInfo;
    private Button buttonMarkAttendance;
    private Button buttonViewAttendance;
    private LinearLayout sessionButtons;
    private Button buttonStartSession;
    private Button buttonEndSession;
    private AlertDialog sessionCodeDialog;
    private Button buttonViewClassAttendance;
    private Button buttonAttendanceStatistics;
    private AttendanceRepository attendanceRepository;
    private SessionRepository sessionRepository;
    private SessionManager sessionManager;
    private SimpleDateFormat timeFormat;
    private List<Attendance> attendanceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initializeViews();
        setupDashboard();
    }

    private void initializeViews() {
        textGreeting = findViewById(R.id.textGreeting);
        textClassInfo = findViewById(R.id.textClassInfo);
        buttonMarkAttendance = findViewById(R.id.buttonMarkAttendance);
        buttonViewAttendance = findViewById(R.id.buttonViewAttendance);
        sessionButtons = findViewById(R.id.sessionButtons);
        buttonStartSession = findViewById(R.id.buttonStartSession);
        buttonEndSession = findViewById(R.id.buttonEndSession);
        buttonViewClassAttendance = findViewById(R.id.buttonViewClassAttendance);
        buttonAttendanceStatistics = findViewById(R.id.buttonAttendanceStatistics);

        timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        sessionManager = new SessionManager(this);
        attendanceRepository = new AttendanceRepository();
        sessionRepository = new SessionRepository();

        // Set greeting
        textGreeting.setText(String.format("Hi, %s!", sessionManager.getUserName()));

        // Common buttons
        findViewById(R.id.buttonSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        findViewById(R.id.buttonLogout).setOnClickListener(v -> {
            sessionManager.logoutUser();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void setupDashboard() {
        boolean isInstructor = User.ROLE_INSTRUCTOR.equals(sessionManager.getUserRole());
        setupViewVisibility(isInstructor);
        setupClickListeners(isInstructor);
        loadDashboardData(isInstructor);
    }

    private void setupViewVisibility(boolean isInstructor) {
        // Student views
        buttonMarkAttendance.setVisibility(isInstructor ? View.GONE : View.VISIBLE);
        buttonViewAttendance.setVisibility(isInstructor ? View.GONE : View.VISIBLE);

        // Instructor views
        sessionButtons.setVisibility(isInstructor ? View.VISIBLE : View.GONE);
        buttonViewClassAttendance.setVisibility(isInstructor ? View.VISIBLE : View.GONE);
        buttonAttendanceStatistics.setVisibility(isInstructor ? View.VISIBLE : View.GONE);
    }

    private void setupClickListeners(boolean isInstructor) {
        if (isInstructor) {
            buttonViewClassAttendance.setOnClickListener(v -> {
                Intent intent = new Intent(this, ViewAttendanceActivity.class);
                CourseClass currentClass = CourseClass.getCurrentClass();
                if (currentClass != null) {
                    intent.putExtra(ViewAttendanceActivity.EXTRA_CLASS_ID, currentClass.getClassId());
                }
                startActivity(intent);
            });

            // Add session button listeners
            buttonStartSession.setOnClickListener(v -> startSession());
            buttonEndSession.setOnClickListener(v -> showEndSessionDialog());
        } else {
            buttonMarkAttendance.setOnClickListener(v -> markAttendance());
            buttonViewAttendance.setOnClickListener(v ->
                    startActivity(new Intent(this, ViewAttendanceActivity.class)));
        }
    }

    private void loadDashboardData(boolean isInstructor) {
        new Thread(() -> {
            try {
                Result<List<?>> result = attendanceRepository.getTodayAttendance(
                        sessionManager.getUserId(),
                        isInstructor
                );

                if (!result.isSuccess()) {
                    showError("Failed to load class information");
                    return;
                }

                if (isInstructor) {
                    handleInstructorData((List<CourseClass>) result.getData());
                } else {
                    handleStudentData((List<Attendance>) result.getData());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading dashboard data", e);
                showError("Error loading data");
            }
        }).start();
    }

    private void handleInstructorData(List<CourseClass> classes) {
        runOnUiThread(() -> {
            CourseClass currentClass = findCurrentOrUpcomingClass(classes);
            if (currentClass == null) {
                showNoClasses();
                return;
            }

            CourseClass.setCurrentClass(currentClass);
            SpannableStringBuilder builder = new SpannableStringBuilder();
            appendClassInfo(builder, currentClass);
            textClassInfo.setText(builder);
            buttonViewClassAttendance.setEnabled(true);

            // Enable/disable session buttons based on validation code and status
            boolean canStart = "ONGOING".equals(currentClass.getStatus())
                    && currentClass.getValidationCode() == null;
            boolean canEnd = "ONGOING".equals(currentClass.getStatus())
                    && currentClass.getValidationCode() != null;

            buttonStartSession.setEnabled(canStart);
            buttonEndSession.setEnabled(canEnd);
        });
    }

    private void startSession() {
        CourseClass currentClass = CourseClass.getCurrentClass();
        if (currentClass == null || currentClass.getSessionId() == null) {
            Toast.makeText(this, "No active class session", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            Result<String> result = sessionRepository.startSession(currentClass.getSessionId());
            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    showSessionCodeDialog(result.getData());
                    buttonStartSession.setEnabled(false);
                    buttonEndSession.setEnabled(true);
                    loadDashboardData(true); // Refresh dashboard
                } else {
                    Toast.makeText(this, result.getError(), Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void showSessionCodeDialog(String code) {
        new AlertDialog.Builder(this)
                .setTitle("Session Started")
                .setMessage(String.format("Share this code with your students:\n\n%s", code))
                .setPositiveButton("Copy Code", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Session Code", code);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Code copied to clipboard", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showEndSessionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("End Session Early")
                .setMessage("Are you sure you want to end the session now? This will update the end time to current time.")
                .setPositiveButton("End Session", (dialog, which) -> endSession())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void endSession() {
        CourseClass currentClass = CourseClass.getCurrentClass();
        if (currentClass == null || currentClass.getSessionId() == null) {
            Toast.makeText(this, "No active session", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            Result<Void> result = sessionRepository.endSession(currentClass.getSessionId());
            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    Toast.makeText(this, "Session ended", Toast.LENGTH_SHORT).show();
                    buttonStartSession.setEnabled(true);
                    buttonEndSession.setEnabled(false);
                    loadDashboardData(true); // Refresh dashboard
                } else {
                    Toast.makeText(this, result.getError(), Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void handleStudentData(List<Attendance> attendances) {
        runOnUiThread(() -> {
            this.attendanceList = attendances;
            Attendance currentAttendance = findCurrentOrUpcomingClass(attendances);
            if (currentAttendance == null) {
                showNoClasses();
                return;
            }

            SpannableStringBuilder builder = new SpannableStringBuilder();
            appendClassInfo(builder, currentAttendance);
            appendAttendanceStatus(builder, currentAttendance.getStatus());
            textClassInfo.setText(builder);

            // Can mark if:
            // 1. Class is ONGOING
            // 2. Has validation code
            // 3. No actual attendance record yet (attendance ID should be 0 or null)
            boolean isOngoing = "ONGOING".equals(currentAttendance.getStatus());
            boolean hasCode = currentAttendance.getValidationCode() != null;
            boolean notMarked = currentAttendance.getAttendanceId() == null ||
                    currentAttendance.getAttendanceId() == 0;

            Log.d(TAG, "Current Status: " + currentAttendance.getStatus());
            Log.d(TAG, "Validation Code: " + currentAttendance.getValidationCode());
            Log.d(TAG, "Attendance ID: " + currentAttendance.getAttendanceId());
            Log.d(TAG, "isOngoing: " + isOngoing);
            Log.d(TAG, "hasCode: " + hasCode);
            Log.d(TAG, "notMarked: " + notMarked);

            buttonMarkAttendance.setEnabled(isOngoing && hasCode && notMarked);
            buttonMarkAttendance.setText("Mark Attendance");
        });
    }

    private <T> T findCurrentOrUpcomingClass(List<T> classes) {
        if (classes == null || classes.isEmpty()) return null;

        Calendar now = Calendar.getInstance();

        for (T classObj : classes) {
            // Create calendar for class end time with proper date
            Calendar endTime = Calendar.getInstance();

            if (classObj instanceof Attendance) {
                Attendance attendance = (Attendance) classObj;
                endTime.setTime(attendance.getSessionDate());
                endTime.set(Calendar.HOUR_OF_DAY, attendance.getEndTime().getHours());
                endTime.set(Calendar.MINUTE, attendance.getEndTime().getMinutes());
            } else if (classObj instanceof CourseClass) {
                CourseClass courseClass = (CourseClass) classObj;
                endTime.setTime(courseClass.getSessionDate());
                endTime.set(Calendar.HOUR_OF_DAY, courseClass.getEndTime().getHours());
                endTime.set(Calendar.MINUTE, courseClass.getEndTime().getMinutes());
            } else {
                continue; // Skip unknown types
            }

            endTime.add(Calendar.MINUTE, 30);  // 30 min buffer after class ends

            // If class is current or upcoming (hasn't ended yet including buffer)
            if (endTime.after(now)) {
                return classObj;
            }
        }
        return null;
    }

    private boolean canMarkAttendance(Attendance attendance) {
        if (attendance == null) return false;

        // Can mark if:
        // 1. Not already marked (status is pending/not marked)
        // 2. Session has validation code
        boolean notMarked = "pending".equalsIgnoreCase(attendance.getStatus()) ||
                "NOT MARKED".equalsIgnoreCase(attendance.getStatus());

        return notMarked && attendance.getValidationCode() != null;
    }

    private void appendClassInfo(SpannableStringBuilder builder, CourseClass courseClass) {
        builder.append(courseClass.getModuleCode()).append("\n");
        builder.append(courseClass.getModuleName()).append("\n");
        builder.append(String.format("Section %s - %s",
                courseClass.getSection(),
                courseClass.getRoom()
        )).append("\n");

        if (courseClass.getStartTime() != null && courseClass.getEndTime() != null) {
            builder.append(String.format("%s - %s",
                    timeFormat.format(courseClass.getStartTime()),
                    timeFormat.format(courseClass.getEndTime())
            )).append("\n");
        }

        // Add status with color
        builder.append("Status: ");
        String statusText = courseClass.getStatus() != null ? courseClass.getStatus() : "NOT MARKED";
        SpannableString spannable = new SpannableString(statusText);

        int color;
        switch (statusText) {
            case "COMPLETED":
                color = ContextCompat.getColor(this, android.R.color.holo_green_dark);
                break;
            case "ONGOING":
                color = ContextCompat.getColor(this, android.R.color.holo_blue_dark);
                break;
            case "UPCOMING":
                color = ContextCompat.getColor(this, android.R.color.darker_gray);
                break;
            default:
                color = ContextCompat.getColor(this, android.R.color.darker_gray);
                break;
        }

        spannable.setSpan(
                new ForegroundColorSpan(color),
                0,
                statusText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        builder.append(spannable);
    }

    private void appendClassInfo(SpannableStringBuilder builder, Attendance attendance) {
        builder.append(attendance.getModuleCode()).append("\n");
        builder.append(attendance.getModuleName()).append("\n");
        builder.append(String.format("Section %s - %s",
                attendance.getSection(),
                attendance.getRoom()
        )).append("\n");

        if (attendance.getStartTime() != null && attendance.getEndTime() != null) {
            builder.append(String.format("%s - %s",
                    timeFormat.format(attendance.getStartTime()),
                    timeFormat.format(attendance.getEndTime())
            )).append("\n");
        }
    }

    private void appendAttendanceStatus(SpannableStringBuilder builder, String status) {
        builder.append("Status: ");
        String statusText = status != null ? status.toUpperCase() : "NOT MARKED";
        SpannableString spannable = new SpannableString(statusText);

        int color;
        switch (statusText) {
            case "PRESENT":
                color = ContextCompat.getColor(this, android.R.color.holo_green_dark);
                break;
            case "LATE":
            case "ABSENT":
                color = ContextCompat.getColor(this, android.R.color.holo_red_dark);
                break;
            default:
                color = ContextCompat.getColor(this, android.R.color.darker_gray);
                break;
        }

        spannable.setSpan(
                new ForegroundColorSpan(color),
                0,
                statusText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        builder.append(spannable);
    }

    private void markAttendance() {
        // Get current attendance session
        Attendance currentAttendance = findCurrentOrUpcomingClass(attendanceList);
        if (currentAttendance == null || currentAttendance.getSessionId() == null) {
            Toast.makeText(this, "No active session found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create simple input dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Attendance Code");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(6) }); // Limit to 6 digits
        input.setGravity(Gravity.CENTER);
        input.setHint("Enter 6-digit code");

        // Add padding to the input
        LinearLayout container = new LinearLayout(this);
        container.setPadding(60, 40, 60, 20);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Submit", null); // Set to null initially
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();

        // Override the positive button click to prevent dialog dismissal on error
        dialog.setOnShowListener(dialogInterface -> {
            Button submitButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            submitButton.setOnClickListener(view -> {
                String code = input.getText().toString().trim();
                if (!code.matches("\\d{6}")) {
                    input.setError("Please enter a valid 6-digit code");
                    return;
                }

                submitButton.setEnabled(false);
                submitButton.setText("Verifying...");

                // Validate code and mark attendance
                new Thread(() -> {
                    try {
                        // First validate the code
                        Result<Boolean> validationResult = sessionRepository.validateCode(
                                currentAttendance.getSessionId(),
                                code
                        );

                        if (!validationResult.isSuccess() || !validationResult.getData()) {
                            showError("Invalid code. Please try again.", input, submitButton);
                            return;
                        }

                        // If code is valid, mark attendance
                        Result<Void> markResult = attendanceRepository.markAttendance(
                                sessionManager.getUserId(),
                                currentAttendance.getSessionId()
                        );

                        if (!markResult.isSuccess()) {
                            showError("Failed to mark attendance. Please try again.", input, submitButton);
                            return;
                        }

                        // Success - update UI
                        runOnUiThread(() -> {
                            dialog.dismiss();
                            Toast.makeText(this, "Attendance marked successfully!", Toast.LENGTH_LONG).show();
                            loadDashboardData(false); // Refresh dashboard
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Error in validateAndMarkAttendance", e);
                        showError("An error occurred. Please try again.", input, submitButton);
                    }
                }).start();
            });
        });

        dialog.show();

        // Set focus to input and show keyboard
        input.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    private void showNoClasses() {
        textClassInfo.setText("No classes scheduled for today");
        buttonMarkAttendance.setEnabled(false);
        buttonStartSession.setEnabled(false);
        buttonEndSession.setEnabled(false);
    }

    private void showError(String message, Object... params) {
        runOnUiThread(() -> {
            // Show toast message for all error cases
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

            // Handle dialog-specific error UI updates
            if (params != null && params.length >= 2 && params[0] instanceof EditText && params[1] instanceof Button) {
                EditText input = (EditText) params[0];
                Button submitButton = (Button) params[1];
                input.setError(message);
                submitButton.setEnabled(true);
                submitButton.setText("Submit");
            }
            // Handle dashboard-specific error UI updates
            else {
                textClassInfo.setText("Error loading class information");
                buttonMarkAttendance.setEnabled(false);
                buttonViewClassAttendance.setEnabled(false);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData(User.ROLE_INSTRUCTOR.equals(sessionManager.getUserRole()));
    }
}