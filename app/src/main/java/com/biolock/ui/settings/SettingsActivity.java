package com.biolock.ui.settings;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.biolock.R;
import com.biolock.repository.FaceEmbeddingRepository;
import com.biolock.repository.FaceRecognitionLogRepository;
import com.biolock.repository.Result;
import com.biolock.repository.SecuritySettingsRepository;
import com.biolock.model.SecuritySettings;
import com.biolock.utils.SecurityAssessment;
import com.biolock.utils.SessionManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    private SessionManager sessionManager;
    private SecuritySettingsRepository securitySettingsRepository;
    private FaceEmbeddingRepository faceEmbeddingRepository;

    private Switch switchBiometrics;
    private Button buttonEnrollFace;
    private Spinner spinnerMaxAttempts;
    private Spinner spinnerLockoutDuration;
    private Button buttonSaveSecuritySettings;
    private Button buttonSecurityCheck;
    private TextView textLastAssessment;
    private TextView textSecurityStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeComponents();
    }

    private void initializeComponents() {
        initializeViews();
        setupSpinners();

        // Show progress while initializing
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Loading settings...");
        progress.setCancelable(false);
        progress.show();

        // Initialize on background thread
        new Thread(() -> {
            try {
                // Initialize repositories
                sessionManager = new SessionManager(this);
                securitySettingsRepository = new SecuritySettingsRepository();
                faceEmbeddingRepository = new FaceEmbeddingRepository();

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

        // Disable buttons initially
        disableButtons();
    }

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

    private void setupSpinners() {
        // Setup max attempts spinner (1 to MAX_ALLOWED_ATTEMPTS)
        List<Integer> attemptsValues = new ArrayList<>();
        for (int i = 1; i <= SecuritySettingsRepository.MAX_ALLOWED_ATTEMPTS; i++) {
            attemptsValues.add(i);
        }
        ArrayAdapter<Integer> attemptsAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, attemptsValues);
        attemptsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMaxAttempts.setAdapter(attemptsAdapter);

        // Setup lockout duration spinner (5 to MAX_LOCKOUT_DURATION by INTERVAL)
        List<Integer> durationValues = new ArrayList<>();
        for (int i = SecuritySettingsRepository.LOCKOUT_DURATION_INTERVAL;
             i <= SecuritySettingsRepository.MAX_LOCKOUT_DURATION;
             i += SecuritySettingsRepository.LOCKOUT_DURATION_INTERVAL) {
            durationValues.add(i);
        }
        ArrayAdapter<Integer> durationAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, durationValues);
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLockoutDuration.setAdapter(durationAdapter);
    }

    private void loadCurrentSettings() {
        new Thread(() -> {
            Result<SecuritySettings> result = securitySettingsRepository.getSettings(sessionManager.getUserId());
            Result<Boolean> hasFaceResult = faceEmbeddingRepository.hasFaceEmbedding(sessionManager.getUserId());

            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    SecuritySettings settings = result.getData();
                    updateSettingsUI(settings);
                } else {
                    Toast.makeText(this, "Error loading settings: " +
                            result.getError().getMessage(), Toast.LENGTH_SHORT).show();
                }

                if (hasFaceResult.isSuccess()) {
                    updateBiometricUI(hasFaceResult.getData());
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

    private void updateBiometricUI(boolean hasFace) {
        switchBiometrics.setChecked(hasFace);
        buttonEnrollFace.setEnabled(!hasFace);
        // Disable switch if face not enrolled
        switchBiometrics.setEnabled(hasFace);
    }

    private void setupClickListeners() {
        buttonSaveSecuritySettings.setOnClickListener(v -> saveSecuritySettings());
        buttonSecurityCheck.setOnClickListener(v -> performSecurityCheck());
        buttonEnrollFace.setOnClickListener(v -> startActivity(new Intent(this, FaceEnrollmentActivity.class)));

        switchBiometrics.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                return;
            }

            if (isChecked) {
                switchBiometrics.setChecked(false);
                Toast.makeText(this, "Please enroll your face first", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Disable Face Recognition")
                    .setMessage("Are you sure you want to disable face recognition? This will delete your enrolled face data.")
                    .setPositiveButton("Disable", (dialog, which) -> handleDisableFaceRecognition())
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        switchBiometrics.setChecked(true);
                    })
                    .show();
        });
    }

    private void handleDisableFaceRecognition() {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Disabling face recognition...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            try {
                Result<Boolean> deleteResult = faceEmbeddingRepository.deleteFaceEmbedding(sessionManager.getUserId());

                runOnUiThread(() -> {
                    progress.dismiss();
                    if (deleteResult.isSuccess()) {
                        Toast.makeText(this, "Face recognition disabled", Toast.LENGTH_SHORT).show();
                        switchBiometrics.setChecked(false);
                        buttonEnrollFace.setEnabled(true);
                    } else {
                        Toast.makeText(this,
                                "Error disabling face recognition: " + deleteResult.getError().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        switchBiometrics.setChecked(true);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(this,
                            "Error disabling face recognition: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    switchBiometrics.setChecked(true);
                });
            }
        }).start();
    }

    private void saveSecuritySettings() {
        SecuritySettings settings = new SecuritySettings();
        settings.setUserId(sessionManager.getUserId());
        settings.setMaxFailedAttempts((Integer) spinnerMaxAttempts.getSelectedItem());
        settings.setLockoutDurationMins((Integer) spinnerLockoutDuration.getSelectedItem());

        new Thread(() -> {
            Result<SecuritySettings> result = securitySettingsRepository.updateSettings(settings);
            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    Toast.makeText(this, "Settings saved successfully",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error saving settings: " +
                            result.getError().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void performSecurityCheck() {
        new Thread(() -> {
            Result<SecurityAssessment.SecurityMetrics> result =
                    SecurityAssessment.analyzeSecurityStatus(
                            sessionManager.getUserId(),
                            faceEmbeddingRepository,
                            new FaceRecognitionLogRepository()
                    );

            runOnUiThread(() -> {
                updateLastCheckedTime();
                if (result.isSuccess()) {
                    String report = SecurityAssessment.formatSecurityReport(result.getData());
                    textSecurityStatus.setText(report);
                } else {
                    textSecurityStatus.setText("Unable to assess security status: " +
                            result.getError().getMessage());
                }
            });
        }).start();
    }

    private void updateLastCheckedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        textLastAssessment.setText("Last checked: " + currentTime);
    }

    private int findSpinnerPosition(Spinner spinner, int value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if ((Integer) adapter.getItem(i) == value) {
                return i;
            }
        }
        return 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Initialize repository if null
        if (faceEmbeddingRepository == null) {
            faceEmbeddingRepository = new FaceEmbeddingRepository();
        }

        // Check face enrollment status when returning from enrollment activity
        new Thread(() -> {
            try {
                Result<Boolean> hasFaceResult = faceEmbeddingRepository.hasFaceEmbedding(sessionManager.getUserId());
                runOnUiThread(() -> {
                    if (hasFaceResult.isSuccess()) {
                        updateBiometricUI(hasFaceResult.getData());
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error checking face enrollment status", e);
            }
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}