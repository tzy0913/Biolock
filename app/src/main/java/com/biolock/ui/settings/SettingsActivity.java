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
import com.biolock.repository.FaceAuthenticationRepository;
import com.biolock.repository.Result;
import com.biolock.model.SecuritySettings;
import com.biolock.repository.UserRepository;
import com.biolock.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    private SessionManager sessionManager;
    private UserRepository userRepository;
    private FaceAuthenticationRepository faceAuthenticationRepository;

    private Switch switchBiometrics;
    private Button buttonEnrollFace;
    private Spinner spinnerMaxAttempts;
    private Spinner spinnerLockoutDuration;
    private Button buttonSaveSecuritySettings;
    private Button buttonSecurityCheck;
    private TextView textLastAssessment;
    private TextView textSecurityStatus;

    // Constants for spinners
    private static final int MAX_ALLOWED_ATTEMPTS = 5;
    private static final int MAX_LOCKOUT_DURATION = 60; // minutes
    private static final int LOCKOUT_DURATION_INTERVAL = 5; // minutes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeComponents();
    }

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

    private void setupClickListeners() {
        buttonSaveSecuritySettings.setOnClickListener(v -> saveSecuritySettings());

        buttonEnrollFace.setOnClickListener(v ->
                startActivity(new Intent(this, FaceEnrollmentActivity.class)));

        buttonSecurityCheck.setOnClickListener(v ->
                Toast.makeText(this, "Security check feature coming soon",
                        Toast.LENGTH_SHORT).show());

        // Updated biometrics switch handler
        switchBiometrics.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;

            if (isChecked) {
                switchBiometrics.setChecked(false);
                Toast.makeText(this, "Please enroll your face first", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show confirmation dialog for disabling
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

    private void handleDisableFaceRecognition() {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Disabling face recognition...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            try {
                // Delete face embedding
                Result<Boolean> result = faceAuthenticationRepository.deleteFace(sessionManager.getUserId());

                runOnUiThread(() -> {
                    progress.dismiss();
                    if (result.isSuccess()) {
                        Toast.makeText(this, "Face recognition disabled", Toast.LENGTH_SHORT).show();
                        updateBiometricUI(false);  // Update UI to reflect no face enrolled
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Initialize repository if null
        if (faceAuthenticationRepository == null) {
            faceAuthenticationRepository = new FaceAuthenticationRepository(this);
        }

        // Check face enrollment status
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

    private void updateBiometricUI(boolean hasFace) {
        switchBiometrics.setChecked(hasFace);
        buttonEnrollFace.setEnabled(!hasFace);
        // Disable switch if face not enrolled
        switchBiometrics.setEnabled(hasFace);
    }
}