/**
 * Activity for managing user security settings, face enrollment, and security assessments.
 * Handles biometric settings, face recognition configuration, and security metrics.
 */
package com.biolock.ui.settings;

// Android Core & UI Components
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

// AndroidX Libraries
import androidx.appcompat.app.AppCompatActivity;

// Biolock Models & Repositories
import com.biolock.R;
import com.biolock.model.SecuritySettings;
import com.biolock.repository.FaceAuthenticationRepository;
import com.biolock.repository.Result;
import com.biolock.repository.UserRepository;

// Biolock Utilities
import com.biolock.utils.SecurityAssessment;
import com.biolock.utils.SessionManager;

// Java Utilities
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    // Constants
    private static final String TAG = "SettingsActivity";
    private static final String DATE_FORMAT = "MMM dd, yyyy HH:mm";
    private static final int MAX_ALLOWED_ATTEMPTS = 5;
    private static final int MAX_LOCKOUT_DURATION = 60; // minutes
    private static final int LOCKOUT_DURATION_INTERVAL = 5; // minutes

    // Dependencies
    private SessionManager sessionManager;
    private UserRepository userRepository;
    private FaceAuthenticationRepository faceAuthenticationRepository;

    // UI Components
    private Switch switchBiometrics;
    private Button buttonEnrollFace;
    private Spinner spinnerMaxAttempts;
    private Spinner spinnerLockoutDuration;
    private Button buttonSaveSecuritySettings;
    private Button buttonSecurityCheck;
    private TextView textLastAssessment;
    private TextView textSecurityStatus;
    private View securityStatusLayout;

    // Lifecycle Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeComponents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (faceAuthenticationRepository == null) {
            faceAuthenticationRepository = new FaceAuthenticationRepository(this);
        }
        checkFaceEnrollmentStatus();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Initialization Methods
    private void initializeComponents() {
        initializeViews();
        setupSpinners();

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Loading settings...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            try {
                sessionManager = new SessionManager(this);
                userRepository = new UserRepository();
                faceAuthenticationRepository = new FaceAuthenticationRepository(this);

                runOnUiThread(() -> {
                    setupClickListeners();
                    loadCurrentSettings();
                    enableButtons();
                    progress.dismiss();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(this, "Failed to initialize: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void initializeViews() {
        switchBiometrics = findViewById(R.id.switchBiometrics);
        buttonEnrollFace = findViewById(R.id.buttonEnrollFace);
        spinnerMaxAttempts = findViewById(R.id.spinnerMaxAttempts);
        spinnerLockoutDuration = findViewById(R.id.spinnerLockoutDuration);
        buttonSaveSecuritySettings = findViewById(R.id.buttonSaveSecuritySettings);
        buttonSecurityCheck = findViewById(R.id.buttonSecurityCheck);
        textLastAssessment = findViewById(R.id.textLastAssessment);
        textSecurityStatus = findViewById(R.id.textSecurityStatus);
        securityStatusLayout = findViewById(R.id.securityStatusLayout);

        disableButtons();
    }

    private void setupSpinners() {
        // Max attempts spinner (1 to MAX_ALLOWED_ATTEMPTS)
        List<Integer> attemptsValues = new ArrayList<>();
        for (int i = 1; i <= MAX_ALLOWED_ATTEMPTS; i++) {
            attemptsValues.add(i);
        }
        ArrayAdapter<Integer> attemptsAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, attemptsValues);
        attemptsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMaxAttempts.setAdapter(attemptsAdapter);

        // Lockout duration spinner
        List<Integer> durationValues = new ArrayList<>();
        for (int i = LOCKOUT_DURATION_INTERVAL; i <= MAX_LOCKOUT_DURATION;
             i += LOCKOUT_DURATION_INTERVAL) {
            durationValues.add(i);
        }
        ArrayAdapter<Integer> durationAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, durationValues);
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLockoutDuration.setAdapter(durationAdapter);
    }

    private void setupClickListeners() {
        buttonSaveSecuritySettings.setOnClickListener(v -> saveSecuritySettings());

        buttonEnrollFace.setOnClickListener(v ->
                startActivity(new Intent(this, FaceEnrollmentActivity.class)));

        buttonSecurityCheck.setOnClickListener(v -> performSecurityAssessment());

        securityStatusLayout.setOnClickListener(v -> {
            String currentStatus = textSecurityStatus.getText().toString();
            if (!currentStatus.equals("Run a security check to view status")) {
                startActivity(SecurityDetailsActivity.createIntent(
                        this,
                        currentStatus,
                        textLastAssessment.getText().toString()
                ));
            }
        });

        switchBiometrics.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;

            if (isChecked) {
                switchBiometrics.setChecked(false);
                Toast.makeText(this, "Please enroll your face first", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Disable Face Recognition")
                    .setMessage("Are you sure you want to disable face recognition? This will delete your enrolled face data.")
                    .setPositiveButton("Disable", (dialog, which) -> {
                        handleDisableFaceRecognition();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        switchBiometrics.setChecked(true);
                    })
                    .show();
        });
    }

    // Settings Management Methods
    private void enableButtons() {
        buttonSaveSecuritySettings.setEnabled(true);
        buttonSecurityCheck.setEnabled(true);
        buttonEnrollFace.setEnabled(true);
        switchBiometrics.setEnabled(true);
    }

    private void disableButtons() {
        buttonSaveSecuritySettings.setEnabled(false);
        buttonSecurityCheck.setEnabled(false);
        buttonEnrollFace.setEnabled(false);
        switchBiometrics.setEnabled(false);
    }

    private void loadCurrentSettings() {
        new Thread(() -> {
            Result<SecuritySettings> result = userRepository.getSecuritySettings(sessionManager.getUserId());

            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    SecuritySettings settings = result.getData();
                    updateSettingsUI(settings);
                } else {
                    Toast.makeText(this, "Error loading settings: " +
                            result.getError(), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void updateSettingsUI(SecuritySettings settings) {
        int attemptsPosition = findSpinnerPosition(
                spinnerMaxAttempts, settings.getMaxFailedAttempts());
        spinnerMaxAttempts.setSelection(attemptsPosition);

        int durationPosition = findSpinnerPosition(
                spinnerLockoutDuration, settings.getLockoutDurationMins());
        spinnerLockoutDuration.setSelection(durationPosition);
    }

    private void saveSecuritySettings() {
        int maxAttempts = (Integer) spinnerMaxAttempts.getSelectedItem();
        int lockoutDuration = (Integer) spinnerLockoutDuration.getSelectedItem();

        new Thread(() -> {
            Result<Boolean> result = userRepository.updateSecuritySettings(
                    sessionManager.getUserId(),
                    maxAttempts,
                    lockoutDuration
            );

            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    Toast.makeText(this, "Settings saved successfully",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error saving settings: " +
                            result.getError(), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // Face Recognition Methods
    private void checkFaceEnrollmentStatus() {
        new Thread(() -> {
            try {
                Result<Boolean> hasFaceResult = faceAuthenticationRepository.hasFaceEnrolled(
                        sessionManager.getUserId());
                runOnUiThread(() -> {
                    if (hasFaceResult.isSuccess()) {
                        updateBiometricUI(hasFaceResult.getData());
                    } else {
                        Toast.makeText(this, "Error checking face enrollment: " +
                                hasFaceResult.getError(), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error checking face enrollment status", e);
            }
        }).start();
    }

    private void handleDisableFaceRecognition() {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Disabling face recognition...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            try {
                Result<Boolean> result = faceAuthenticationRepository.deleteFace(sessionManager.getUserId());

                runOnUiThread(() -> {
                    progress.dismiss();
                    if (result.isSuccess()) {
                        Toast.makeText(this, "Face recognition disabled", Toast.LENGTH_SHORT).show();
                        updateBiometricUI(false);
                    } else {
                        Toast.makeText(this, "Error disabling face recognition: " +
                                result.getError(), Toast.LENGTH_SHORT).show();
                        switchBiometrics.setChecked(true);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error disabling face recognition", e);
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(this, "Error disabling face recognition", Toast.LENGTH_SHORT).show();
                    switchBiometrics.setChecked(true);
                });
            }
        }).start();
    }

    private void updateBiometricUI(boolean hasFace) {
        switchBiometrics.setChecked(hasFace);
        buttonEnrollFace.setEnabled(!hasFace);
        switchBiometrics.setEnabled(hasFace);
    }

    // Security Assessment Methods
    private void performSecurityAssessment() {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Performing security assessment...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            try {
                Result<SecurityAssessment.SecurityMetrics> result =
                        faceAuthenticationRepository.runSecurityAssessment(sessionManager.getUserId());

                runOnUiThread(() -> {
                    if (result.isSuccess()) {
                        SecurityAssessment.SecurityMetrics metrics = result.getData();
                        updateSecurityStatus(metrics);
                        if (securityStatusLayout != null) {
                            securityStatusLayout.setBackgroundColor(getResources().getColor(
                                    android.R.color.background_light));
                        }
                    } else {
                        Toast.makeText(this, "Error performing assessment: " +
                                result.getError(), Toast.LENGTH_LONG).show();
                        textLastAssessment.setText("Assessment failed");
                        textSecurityStatus.setText("Unable to determine security status");
                        if (securityStatusLayout != null) {
                            securityStatusLayout.setBackgroundColor(getResources().getColor(
                                    android.R.color.darker_gray));
                        }
                    }
                    progress.dismiss();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(this, "Error performing security assessment: " +
                            e.getMessage(), Toast.LENGTH_LONG).show();
                    textLastAssessment.setText("Assessment failed");
                    textSecurityStatus.setText("Unable to determine security status");
                });
            }
        }).start();
    }

    private void updateSecurityStatus(SecurityAssessment.SecurityMetrics metrics) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        String timestamp = sdf.format(new Date());
        textLastAssessment.setText("Last assessed: " + timestamp);

        StringBuilder statusText = new StringBuilder();

        statusText.append(metrics.securityStatus).append("\n\n");

        if (metrics.totalLoginAttempts > 0) {
            statusText.append(String.format(Locale.US, "Login Success Rate: %.1f%%\n", metrics.loginSuccessRate));
            if (metrics.loginSuccessRate > 0) {
                statusText.append(String.format(Locale.US, "Average Match Score: %.2f\n", metrics.avgLoginSimilarity));
                statusText.append(String.format(Locale.US, "High Quality Matches: %.1f%%\n", metrics.highQualityLoginRate));
            }
            statusText.append("\n");
        }

        // Add attendance metrics if available
        if (metrics.totalAttendanceAttempts > 0) {
            statusText.append("Attendance Success Rate: ")
                    .append(String.format(Locale.US, "%.1f%%\n", metrics.attendanceSuccessRate));
            if (metrics.attendanceSuccessRate > 0) {
                statusText.append(String.format(Locale.US, "Average Match Score: %.2f\n", metrics.avgAttendanceSimilarity));
                statusText.append(String.format(Locale.US, "High Quality Matches: %.1f%%\n", metrics.highQualityAttendanceRate));
                statusText.append(String.format(Locale.US, "Attendance Compliance: %.1f%%\n", metrics.attendanceComplianceRate));
            }
            statusText.append("\n");
        }

        statusText.append("Login Patterns (Last 30 days):\n");
        if (metrics.mostCommonLoginTime != null) {
            statusText.append("• Most frequent login time: ")
                    .append(metrics.mostCommonLoginTime)
                    .append("\n");
        }
        if (metrics.mostCommonAttendanceTime != null) {
            statusText.append("• Most frequent attendance time: ")
                    .append(metrics.mostCommonAttendanceTime)
                    .append("\n");
        }

        if (!metrics.securityWarnings.isEmpty()) {
            statusText.append("\nWarnings:\n");
            for (String warning : metrics.securityWarnings) {
                statusText.append("• ").append(warning).append("\n");
            }
        }

        if (!metrics.recommendations.isEmpty()) {
            statusText.append("\nRecommendations:\n");
            for (String recommendation : metrics.recommendations) {
                statusText.append("• ").append(recommendation).append("\n");
            }
        }

        textSecurityStatus.setText(statusText.toString());
        textSecurityStatus.setTextColor(getResources().getColor(metrics.getStatusColor()));
    }

    // Utility Methods
    private int findSpinnerPosition(Spinner spinner, int value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if ((Integer) adapter.getItem(i) == value) {
                return i;
            }
        }
        return 0;
    }
}