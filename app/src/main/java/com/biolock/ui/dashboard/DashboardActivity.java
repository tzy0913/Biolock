package com.biolock.ui.dashboard;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
    private Button buttonViewClassAttendance;
    private Button buttonAttendanceStatistics;
    private AttendanceRepository attendanceRepository;
    private SessionManager sessionManager;
    private SimpleDateFormat timeFormat;

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
        buttonViewClassAttendance = findViewById(R.id.buttonViewClassAttendance);
        buttonAttendanceStatistics = findViewById(R.id.buttonAttendanceStatistics);

        timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        sessionManager = new SessionManager(this);
        attendanceRepository = new AttendanceRepository();

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
        });
    }

    private void handleStudentData(List<Attendance> attendances) {
        runOnUiThread(() -> {
            Attendance currentAttendance = findCurrentOrUpcomingClass(attendances);
            if (currentAttendance == null) {
                showNoClasses();
                return;
            }

            SpannableStringBuilder builder = new SpannableStringBuilder();
            appendClassInfo(builder, currentAttendance);
            appendAttendanceStatus(builder, currentAttendance.getStatus());
            textClassInfo.setText(builder);
            buttonMarkAttendance.setEnabled(canMarkAttendance(currentAttendance));
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
        if ("present".equalsIgnoreCase(attendance.getStatus())) return false;

        Calendar now = Calendar.getInstance();
        Calendar classStart = Calendar.getInstance();
        Calendar classEnd = Calendar.getInstance();

        classStart.setTime(attendance.getStartTime());
        classEnd.setTime(attendance.getEndTime());
        classEnd.add(Calendar.MINUTE, 30); // 30 min buffer for marking

        // Can mark attendance if within class time + buffer
        return now.after(classStart) && now.before(classEnd);
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
        switch (statusText.toLowerCase()) {
            case "present":
                color = ContextCompat.getColor(this, android.R.color.holo_green_dark);
                break;
            case "late":
            case "absent":
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
        // TODO: Implement mark attendance logic
        // This would typically involve scanning QR code or other validation
        Toast.makeText(this, "Mark attendance functionality coming soon", Toast.LENGTH_SHORT).show();
    }

    private void showNoClasses() {
        textClassInfo.setText("No classes scheduled for today");
        buttonMarkAttendance.setEnabled(false);
        buttonViewClassAttendance.setEnabled(false);
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            textClassInfo.setText("Error loading class information");
            buttonMarkAttendance.setEnabled(false);
            buttonViewClassAttendance.setEnabled(false);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData(User.ROLE_INSTRUCTOR.equals(sessionManager.getUserRole()));
    }
}