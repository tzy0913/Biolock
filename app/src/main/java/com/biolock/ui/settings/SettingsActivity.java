package com.biolock.ui.settings;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.biolock.R;
import com.biolock.database.DatabaseHelper;
import com.biolock.repository.FaceEmbeddingRepository;
import com.biolock.repository.Result;
import com.biolock.repository.SecuritySettingsRepository;
import com.biolock.model.SecuritySettings;
import com.biolock.utils.SessionManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private SecuritySettingsRepository securitySettingsRepository;
    private FaceEmbeddingRepository faceEmbeddingRepository;
    private DatabaseHelper dbHelper;

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

        initializeViews();
        setupSpinners();
        initializeDatabase();
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

        // Disable buttons until db is initialized
        buttonSaveSecuritySettings.setEnabled(false);
        buttonSecurityCheck.setEnabled(false);
        buttonEnrollFace.setEnabled(false);
        switchBiometrics.setEnabled(false);
    }

    private void initializeDatabase() {
        new Thread(() -> {
            try {
                dbHelper = DatabaseHelper.getInstance();
                dbHelper.initialize();

                // Initialize repositories after database is ready
                sessionManager = new SessionManager(this);
                securitySettingsRepository = new SecuritySettingsRepository();
                faceEmbeddingRepository = new FaceEmbeddingRepository();

                runOnUiThread(() -> {
                    setupSpinners();
                    setupClickListeners();
                    loadCurrentSettings();

                    // Enable buttons after initialization
                    buttonSaveSecuritySettings.setEnabled(true);
                    buttonSecurityCheck.setEnabled(true);
                    buttonEnrollFace.setEnabled(true);
                    switchBiometrics.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Database initialization failed", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
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

            // Check if face is enrolled
            Result<Boolean> hasFaceResult = faceEmbeddingRepository.hasFaceEmbedding(sessionManager.getUserId());

            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    SecuritySettings settings = result.getData();

                    // Set spinner selections
                    int attemptsPosition = findSpinnerPosition(
                            spinnerMaxAttempts, settings.getMaxFailedAttempts());
                    spinnerMaxAttempts.setSelection(attemptsPosition);

                    int durationPosition = findSpinnerPosition(
                            spinnerLockoutDuration, settings.getLockoutDurationMins());
                    spinnerLockoutDuration.setSelection(durationPosition);

                    // Update biometric status
                    if (hasFaceResult.isSuccess()) {
                        boolean hasFace = hasFaceResult.getData();
                        switchBiometrics.setChecked(hasFace);
                        buttonEnrollFace.setEnabled(!hasFace);
                    }
                } else {
                    Toast.makeText(SettingsActivity.this, "Error loading settings", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void setupClickListeners() {
        buttonSaveSecuritySettings.setOnClickListener(v -> saveSecuritySettings());
        buttonSecurityCheck.setOnClickListener(v -> performSecurityCheck());
        buttonEnrollFace.setOnClickListener(v -> {
            startActivity(new Intent(this, FaceEnrollmentActivity.class));
        });

        // Add switch listener
        switchBiometrics.setOnCheckedChangeListener((buttonView, isChecked) -> {
            buttonEnrollFace.setEnabled(isChecked);
            if (!isChecked) {
                // TODO: Later - handle disabling face recognition
            }
        });
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
                    Toast.makeText(SettingsActivity.this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SettingsActivity.this,
                            "Error saving settings: " + result.getError().getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void performSecurityCheck() {
        new Thread(() -> {
            Result<Boolean> hasFaceResult = faceEmbeddingRepository.hasFaceEmbedding(sessionManager.getUserId());

            runOnUiThread(() -> {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                String currentTime = sdf.format(new Date());
                textLastAssessment.setText("Last checked: " + currentTime);

                if (hasFaceResult.isSuccess() && hasFaceResult.getData()) {
                    textSecurityStatus.setText("Face recognition is properly configured");
                } else {
                    textSecurityStatus.setText("Face enrollment required for biometric authentication");
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
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.cleanup();
        }
    }
}